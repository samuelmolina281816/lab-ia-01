# Supuestos activos

## Reglas funcionales clave

- El servicio registra pagos informados y movimientos del core, y consulta conciliaciones; no ejecuta pagos ni mueve fondos.
- `operationReference` identifica una única conciliación. Se conserva un registro por referencia, sin historial de movimientos.
- Un pago válido inicia en `PENDING`. Se asume que un movimiento core asociado permite resolver la conciliación; sin pago previo, el registro del movimiento responde `404`.
- La diferencia es `pago - core`. Con tolerancia configurable de `1.00 PEN` por defecto, el resultado es `MATCHED` si está entre `0` y `1.00` inclusive; en otro caso es `MISMATCHED`. Ambos estados son terminales.
- Los registros duplicados son idempotentes: el pago devuelve el registro original y el movimiento duplicado o posterior a un estado terminal devuelve el resultado confirmado, sin alterar la conciliación.
- Solo se admite moneda `PEN`. Los errores de validación, formato o JSON mal formado responden `400`.
- `X-Request-ID` y los logs operativos aportan trazabilidad; los logs no contienen cuerpos completos ni datos sensibles.

## Restricciones técnicas

- Desarrollo previsto con Java 21, Quarkus y Maven; diseño con separación de responsabilidades hexagonal, SOLID, Clean Code e inyección por constructor.
- En etapa de diseño se deben entregar `docs/contrato-rest.md` y `src/main/resources/openapi/openapi.yaml`, OpenAPI 3.0.3, alineados entre sí. No se genera código ni interfaces desde OpenAPI.
- Pruebas con JUnit 5 y Mockito: dominio, casos de uso y HTTP con `@QuarkusTest`, sin RestAssured.
- No usar servicios cloud, contenedores, Kafka, Redis, PostgreSQL, Lombok, Checkstyle ni Spotless.
- Rechazar propiedades JSON desconocidas. Los errores de API se describen con `application/problem+json`.
- Fuera de alcance: integraciones externas, notificaciones, cierre contable, reversos y procesos batch.

## Criterios de aceptación mínimos

1. **Dado** un pago con todos los campos válidos, **Cuando** se registra en `POST /v1/payments`, **Entonces** se crea la conciliación para su referencia en estado `PENDING`.
2. **Dado** un pago cuya referencia excede 64 caracteres o cuya cuenta excede 34, **Cuando** se registra, **Entonces** la solicitud se rechaza con `400`.
3. **Dado** un pago con monto no positivo, precisión o magnitud fuera del máximo de 17 enteros y 2 decimales, **Cuando** se registra, **Entonces** la solicitud se rechaza con `400`.
4. **Dado** un pago o movimiento con moneda distinta de `PEN`, **Cuando** se registra, **Entonces** la solicitud se rechaza con `400`.
5. **Dado** un pago o movimiento con `occurredAt` sin zona horaria o no válido como ISO-8601, **Cuando** se registra, **Entonces** la solicitud se rechaza con `400`.
6. **Dado** un cuerpo JSON mal formado o con propiedades desconocidas, **Cuando** se envía a un endpoint de registro, **Entonces** la solicitud se rechaza con `400`.
7. **Dado** que existe un pago y el movimiento core deja una diferencia entre `0` y `1.00 PEN`, inclusive, **Cuando** se registra el movimiento, **Entonces** la conciliación queda `MATCHED`.
8. **Dado** que existe un pago y el movimiento core deja una diferencia negativa o mayor que `1.00 PEN`, **Cuando** se registra el movimiento, **Entonces** la conciliación queda `MISMATCHED`.
9. **Dado** que no existe un pago para una referencia, **Cuando** se registra un movimiento core para ella, **Entonces** el servicio responde `404` y no crea una conciliación.
10. **Dado** que ya existe un pago para una referencia, **Cuando** se vuelve a registrar un pago con esa referencia, **Entonces** se devuelve el registro original sin modificarlo.
11. **Dado** que una conciliación está en estado terminal, **Cuando** se registra otro movimiento core para esa referencia, **Entonces** se devuelve el resultado confirmado sin cambiar el estado terminal.
12. **Dado** una referencia existente, **Cuando** se consulta `GET /v1/reconciliations/{operationReference}`, **Entonces** se devuelve el estado consolidado y trazable de la conciliación.
13. **Dado** cualquier solicitud procesada, **Cuando** se generan logs operativos, **Entonces** se preserva `X-Request-ID` y no se registran cuerpos completos ni datos sensibles.
14. **Dado** un error de API, **Cuando** se devuelve la respuesta, **Entonces** el error se representa con `application/problem+json`.

## Riesgos iniciales

- No están definidos el mecanismo de persistencia ni las garantías de atomicidad y concurrencia; pueden afectar la unicidad por referencia y la idempotencia bajo solicitudes simultáneas.
- El contrato no precisa el comportamiento de respuestas exitosas (códigos HTTP, esquemas y campos), ni todos los detalles de `application/problem+json`; deben cerrarse en el diseño del contrato.
- Se asume que el movimiento core es el evento que resuelve una conciliación pendiente. No se especifica el tratamiento de pagos o movimientos recibidos fuera de orden, salvo el movimiento sin pago previo.
- No se precisa cómo comparar duplicados con contenido distinto ni qué respuesta exacta devolver tras un movimiento posterior a estado terminal.
- La tolerancia debe ser configurable sin alterar el valor predeterminado de `1.00 PEN`; falta precisar el mecanismo de configuración y validación de ese valor.
