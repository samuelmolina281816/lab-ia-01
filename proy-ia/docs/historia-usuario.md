# Historia de usuario: conciliación de operaciones

## Historia

Como cliente del servicio de conciliación,  
Quiero registrar pagos informados y movimientos del core bancario, y consultar su conciliación por referencia de operación,  
Para conocer de forma consolidada y trazable si los importes reportados coinciden, sin que el servicio ejecute pagos ni mueva fondos.

## Contexto y alcance funcional

El servicio recibe los datos de un pago y los del movimiento correspondiente del core, relacionándolos mediante `operationReference`. El pago registrado crea una conciliación pendiente; al recibirse el movimiento asociado, se calcula la diferencia y se determina el resultado. El cliente puede consultar el estado consolidado por referencia.

Endpoints del flujo:

- `POST /v1/payments`: registrar un pago informado.
- `POST /v1/core-transactions`: registrar un movimiento del core bancario.
- `GET /v1/reconciliations/{operationReference}`: consultar una conciliación.

El registro del pago requiere `operationReference` (máximo 64 caracteres), `accountNumber` (máximo 34), `amount` positivo (hasta 17 dígitos enteros y 2 decimales), `currency` y `occurredAt`. El movimiento core requiere `operationReference`, `amount`, `currency` y `occurredAt` con las mismas restricciones aplicables. La moneda permitida es `PEN` y las fechas deben ser ISO-8601 con zona horaria.

## Reglas de negocio

- Una `operationReference` identifica una sola conciliación; se mantiene un registro por referencia, no un historial de múltiples movimientos.
- Un pago nuevo crea la conciliación en estado `PENDING`.
- La diferencia se calcula como **pago − core**.
- La tolerancia es configurable y su valor predeterminado es `1.00 PEN`.
- El resultado es `MATCHED` cuando la diferencia está entre `0` y la tolerancia, inclusive. Es `MISMATCHED` cuando es negativa o supera la tolerancia.
- `MATCHED` y `MISMATCHED` son estados terminales.
- Registrar nuevamente un pago para una referencia ya existente es idempotente: se devuelve el registro original sin modificarlo.
- Un movimiento core duplicado o recibido cuando la conciliación ya es terminal es idempotente: se devuelve el resultado confirmado sin modificar la conciliación.
- Un movimiento core sin pago previo responde `404` y no crea una conciliación.
- Los errores de validación, formato o JSON mal formado responden `400`.

## Restricciones y supuestos

- El servicio realiza conciliación informativa: no ejecuta pagos, no mueve fondos y no cambia datos en el core bancario.
- Se admite únicamente moneda `PEN`.
- Se rechazan solicitudes JSON mal formadas y propiedades desconocidas.
- Se usa `X-Request-ID` para trazabilidad operativa. Los logs no deben incluir cuerpos completos ni datos sensibles.
- Los errores se representan con `application/problem+json`.
- Persisten las reglas de unicidad e idempotencia por referencia. El mecanismo de persistencia y sus garantías de concurrencia quedan por definir en el diseño técnico.
- Los estados y reglas descritos no especifican códigos HTTP ni esquemas completos de respuestas exitosas; esos detalles deben establecerse en el contrato REST.

## Casos fuera de alcance

- Ejecutar pagos o realizar movimientos reales de fondos.
- Integraciones con servicios cloud o sistemas externos.
- Colas de eventos y notificaciones externas.
- Cierre contable, reversos y procesos batch.
- Mantener historial de múltiples movimientos por referencia.
- Generar automáticamente interfaces o código a partir de OpenAPI.

## Criterios de aceptación

1. **Dado** un pago con todos los campos obligatorios válidos, **Cuando** se registra en `POST /v1/payments`, **Entonces** se crea una conciliación en estado `PENDING` para su `operationReference`.
2. **Dado** un pago con `operationReference` de más de 64 caracteres o `accountNumber` de más de 34, **Cuando** se registra, **Entonces** se rechaza la solicitud con `400`.
3. **Dado** un pago o movimiento con importe no positivo, **Cuando** se registra, **Entonces** se rechaza la solicitud con `400`.
4. **Dado** un pago o movimiento con más de 17 dígitos enteros o más de 2 decimales, **Cuando** se registra, **Entonces** se rechaza la solicitud con `400`.
5. **Dado** un pago o movimiento con moneda distinta de `PEN`, **Cuando** se registra, **Entonces** se rechaza la solicitud con `400`.
6. **Dado** un pago o movimiento cuya fecha `occurredAt` no sea ISO-8601 válida con zona horaria, **Cuando** se registra, **Entonces** se rechaza la solicitud con `400`.
7. **Dado** un cuerpo JSON mal formado o con propiedades desconocidas, **Cuando** se envía a un endpoint de registro, **Entonces** se rechaza la solicitud con `400`.
8. **Dado** una conciliación pendiente y un movimiento cuyo importe deja una diferencia entre `0` y `1.00 PEN`, inclusive, **Cuando** se registra el movimiento en `POST /v1/core-transactions`, **Entonces** la conciliación queda `MATCHED`.
9. **Dado** una conciliación pendiente y un movimiento cuya diferencia es negativa o supera `1.00 PEN`, **Cuando** se registra el movimiento, **Entonces** la conciliación queda `MISMATCHED`.
10. **Dado** que no existe un pago para una referencia, **Cuando** se registra un movimiento core para esa referencia, **Entonces** el servicio responde `404` y no crea la conciliación.
11. **Dado** que ya existe un pago para una referencia, **Cuando** se registra nuevamente un pago para ella, **Entonces** se devuelve el registro original sin modificar la conciliación.
12. **Dado** que una conciliación ya está en estado terminal, **Cuando** se registra otro movimiento core para su referencia, **Entonces** se devuelve el resultado confirmado y no se modifica el estado.
13. **Dado** una referencia con conciliación existente, **Cuando** el cliente consulta `GET /v1/reconciliations/{operationReference}`, **Entonces** obtiene el resultado consolidado de esa conciliación.
14. **Dado** una solicitud procesada, **Cuando** se genera trazabilidad operativa, **Entonces** se conserva `X-Request-ID` y no se registran cuerpos completos ni datos sensibles.
15. **Dado** que una solicitud produce un error de API, **Cuando** el servicio devuelve la respuesta, **Entonces** el contenido del error usa `application/problem+json`.

## Subtareas técnicas

1. Definir el contrato REST descriptivo en `docs/contrato-rest.md`, incluyendo endpoints, requests, responses, validaciones, códigos HTTP y errores `application/problem+json`.
2. Elaborar `src/main/resources/openapi/openapi.yaml` en OpenAPI 3.0.3 y verificar su alineación con el contrato descriptivo, sin generar interfaces ni código automáticamente.
3. Precisar los modelos de entrada y salida y las validaciones de campos obligatorios, longitudes, importes, moneda, fecha y propiedades JSON desconocidas.
4. Diseñar el modelo de dominio de conciliación, sus estados y transiciones, incluyendo cálculo de diferencia y tolerancia configurable predeterminada en `1.00 PEN`.
5. Definir los casos de uso para registrar pagos, registrar movimientos core y consultar conciliaciones, respetando idempotencia y estados terminales.
6. Proponer un mecanismo de persistencia compatible con unicidad por `operationReference` y documentar las decisiones de atomicidad y concurrencia antes de implementarlo.
7. Definir el manejo uniforme de errores y excepciones, con respuestas `application/problem+json` y los códigos HTTP acordados.
8. Diseñar la propagación de `X-Request-ID` y los logs operativos para garantizar trazabilidad sin exponer cuerpos completos ni datos sensibles.
9. Preparar pruebas de dominio y casos de uso con JUnit 5 y Mockito, cubriendo reglas de conciliación, validaciones, duplicados y estados terminales.
10. Preparar pruebas HTTP con `@QuarkusTest` sin RestAssured para los endpoints, códigos HTTP y formatos de respuesta acordados.
11. Mantener el diseño compatible con Java 21, Quarkus y Maven, separación de responsabilidades hexagonal, SOLID e inyección por constructor, y respetar las herramientas y tecnologías prohibidas para este flujo.
