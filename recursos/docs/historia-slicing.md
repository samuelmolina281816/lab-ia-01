# División de la historia con SPIDR

Las subhistorias están ordenadas para producir incrementos verificables y mantener como invariantes de extremo a extremo: una conciliación por `operationReference`, idempotencia sin mutaciones inesperadas y estados terminales inmutables. Las decisiones aún no definidas se resuelven antes de comprometer su implementación.

## 1. Spike — Resolver decisiones de contrato y persistencia

**Descripción**

Como equipo de producto y desarrollo,  
Quiero investigar y acordar las decisiones pendientes de contrato REST, persistencia y concurrencia,  
Para implementar la conciliación con respuestas coherentes y garantías viables de unicidad e idempotencia.

**Criterios de aceptación**

- **Dado** que no están definidos los códigos y esquemas de respuestas exitosas, **Cuando** finaliza el spike, **Entonces** queda documentado un contrato de respuesta por endpoint y sus códigos HTTP, incluidos los errores `application/problem+json`.
- **Dado** que el servicio debe persistir una conciliación única por referencia, **Cuando** se evalúan las alternativas permitidas por las restricciones técnicas, **Entonces** queda documentado un mecanismo propuesto y cómo garantiza unicidad, atomicidad e idempotencia bajo concurrencia.
- **Dado** que los duplicados pueden contener datos distintos, **Cuando** se revisan los casos de pago y movimiento duplicados, **Entonces** queda acordado y registrado qué respuesta se devuelve y qué datos permanecen sin cambios.
- **Dado** que la tolerancia es configurable, **Cuando** se cierra el análisis, **Entonces** quedan definidos el mecanismo de configuración, la validación del valor y el valor predeterminado `1.00 PEN`.

**Dependencias:** Historia de usuario y restricciones del flujo.  
**Prioridad:** P0 — bloqueante para cerrar diseño.  
**Orden recomendado:** 1, con tiempo acotado; sus decisiones habilitan las historias dependientes.

## 2. Interface — Definir el contrato de integración REST

**Descripción**

Como cliente del servicio de conciliación,  
Quiero contar con un contrato REST preciso para registrar pagos, registrar movimientos core y consultar conciliaciones,  
Para integrar mis sistemas con solicitudes, respuestas y errores previsibles.

**Criterios de aceptación**

- **Dado** el alcance de los tres endpoints, **Cuando** se publica el contrato descriptivo, **Entonces** especifica `POST /v1/payments`, `POST /v1/core-transactions` y `GET /v1/reconciliations/{operationReference}`, con requests y responses.
- **Dado** el contrato descriptivo, **Cuando** se prepara la especificación OpenAPI, **Entonces** se entrega OpenAPI 3.0.3 alineado y no se generan interfaces ni código automáticamente.
- **Dado** solicitudes de registro, **Cuando** se documentan sus esquemas y errores, **Entonces** se especifican campos obligatorios, longitudes, formato y validación de importes, moneda `PEN`, fecha ISO-8601 con zona horaria, JSON mal formado y propiedades desconocidas.
- **Dado** una solicitud que no puede procesarse, **Cuando** el cliente recibe la respuesta, **Entonces** el contrato define el código HTTP y el formato `application/problem+json`.
- **Dado** que se requiere trazabilidad, **Cuando** se especifican headers y comportamiento operativo, **Entonces** queda documentado el uso de `X-Request-ID` y la prohibición de exponer datos sensibles en logs.

**Dependencias:** Spike (decisiones de respuestas, duplicados y configuración).  
**Prioridad:** P0 — base de integración y pruebas HTTP.  
**Orden recomendado:** 2, antes de implementar endpoints.

## 3. Data — Persistir conciliaciones con garantías

**Descripción**

Como servicio de conciliación,  
Quiero guardar el estado de cada operación con unicidad y cambios atómicos por `operationReference`,  
Para conservar resultados consistentes ante reintentos y solicitudes concurrentes.

**Criterios de aceptación**

- **Dado** un pago nuevo válido, **Cuando** se persiste, **Entonces** se crea un único registro para la referencia con estado `PENDING`.
- **Dado** dos solicitudes concurrentes para registrar el mismo pago, **Cuando** ambas intentan persistirse, **Entonces** existe una sola conciliación y se devuelve el registro original sin duplicarlo ni modificarlo.
- **Dado** una conciliación persistida, **Cuando** se guarda el movimiento asociado y el resultado calculado, **Entonces** el movimiento y la transición se aplican atómicamente de acuerdo con la decisión documentada en el spike.
- **Dado** una conciliación `MATCHED` o `MISMATCHED`, **Cuando** se intenta guardar un cambio posterior, **Entonces** el repositorio conserva el resultado terminal sin mutarlo.
- **Dado** el modelo persistido, **Cuando** se verifica la solución, **Entonces** cumple el mecanismo y las restricciones tecnológicas acordadas en el spike y mantiene una conciliación por referencia, no un historial.

**Dependencias:** Spike; Interface para el modelo y semántica acordados.  
**Prioridad:** P0 — protege integridad e idempotencia.  
**Orden recomendado:** 3, antes de completar los flujos de negocio.

## 4. Path — Entregar el flujo de registro y consulta de pago

**Descripción**

Como cliente del servicio de conciliación,  
Quiero registrar un pago y consultar su conciliación pendiente,  
Para confirmar que la operación fue recibida y conocer su estado inicial.

**Criterios de aceptación**

- **Dado** un pago que satisface el contrato, **Cuando** se envía a `POST /v1/payments`, **Entonces** se registra y se devuelve la respuesta definida en el contrato con estado `PENDING`.
- **Dado** que ya existe un pago con esa `operationReference`, **Cuando** se reintenta `POST /v1/payments`, **Entonces** se devuelve el registro original y no se sustituyen sus datos, aunque el reintento incluya otros valores.
- **Dado** una referencia con pago registrado, **Cuando** se consulta `GET /v1/reconciliations/{operationReference}`, **Entonces** se devuelve la conciliación en estado `PENDING` con los datos expuestos por el contrato.
- **Dado** una solicitud de pago que infringe el contrato, **Cuando** se procesa, **Entonces** se rechaza con el código HTTP definido y error `application/problem+json`, sin crear ni modificar una conciliación.
- **Dado** una referencia sin conciliación, **Cuando** se consulta, **Entonces** se devuelve el resultado de no encontrado acordado en el contrato, sin crear registros.

**Dependencias:** Interface y Data.  
**Prioridad:** P1 — primer flujo vertical observable.  
**Orden recomendado:** 4, después de asegurar el almacenamiento y contrato.

## 5. Rules — Conciliar el movimiento core y cerrar el estado

**Descripción**

Como cliente del servicio de conciliación,  
Quiero registrar el movimiento core correspondiente a un pago y obtener el resultado de la conciliación,  
Para saber si los importes coinciden dentro de la tolerancia acordada.

**Criterios de aceptación**

- **Dado** un movimiento válido para una referencia con pago pendiente y una diferencia entre `0` y la tolerancia inclusiva, **Cuando** se registra en `POST /v1/core-transactions`, **Entonces** la conciliación pasa a `MATCHED`; la tolerancia por defecto es `1.00 PEN`.
- **Dado** un movimiento válido para una referencia con pago pendiente y una diferencia negativa o superior a la tolerancia, **Cuando** se registra, **Entonces** la conciliación pasa a `MISMATCHED`.
- **Dado** que no existe un pago para la referencia, **Cuando** se registra un movimiento core, **Entonces** se responde `404` según el contrato y no se crea conciliación.
- **Dado** que el mismo movimiento se reintenta, **Cuando** se registra de nuevo, **Entonces** el servicio confirma el resultado idempotentemente y no crea ni altera registros adicionales.
- **Dado** que la conciliación está en `MATCHED` o `MISMATCHED`, **Cuando** llega otro movimiento, **Entonces** se confirma el resultado existente y el estado terminal no cambia.
- **Dado** un movimiento que incumple las validaciones del contrato, **Cuando** se procesa, **Entonces** se rechaza con `400` y no se altera la conciliación.
- **Dado** un pago y movimiento procesados, **Cuando** se consulta la referencia, **Entonces** se observa el estado terminal y la diferencia calculada como pago menos core, según el contrato.

**Dependencias:** Spike, Interface, Data y Path.  
**Prioridad:** P1 — completa el valor central de conciliación.  
**Orden recomendado:** 5, luego del flujo de pago; mantener como condición de salida la verificación de idempotencia y estados terminales.

## Orden resumido

1. **Spike:** cerrar decisiones pendientes.
2. **Interface:** acordar y publicar el contrato REST y OpenAPI.
3. **Data:** garantizar unicidad, persistencia atómica e invariantes.
4. **Path:** habilitar registro de pago y consulta del estado `PENDING`.
5. **Rules:** registrar el movimiento, calcular el resultado y verificar idempotencia y estados terminales.

En cada entrega, los reintentos no deben crear registros ni cambiar datos ya confirmados de forma inesperada. Ningún incremento posterior puede reabrir ni modificar un estado `MATCHED` o `MISMATCHED`.
