# Contexto de la tarea (adaptado a `rest-conciliacion`)

## Objetivo

Permitir que el cliente registre pagos y movimientos del core, y consulte de forma consolidada y trazable el estado de conciliacion por referencia de operacion.

El servicio es de conciliacion: no ejecuta pagos ni mueve dinero.

## Alcance funcional

- Registrar pagos informados.
- Registrar movimientos del core bancario.
- Conciliar pago vs core por `operationReference`.
- Consultar el resultado de la conciliacion.
- Mantener trazabilidad con `X-Request-ID` y logs operativos.

## Endpoints esperados

- `POST /v1/payments`
- `POST /v1/core-transactions`
- `GET /v1/reconciliations/{operationReference}`

## Datos requeridos por solicitud

### Registro de pago (`POST /v1/payments`)

- `operationReference` (obligatorio, maximo 64 caracteres)
- `accountNumber` (obligatorio, maximo 34 caracteres)
- `amount` (obligatorio, positivo, hasta 17 enteros y 2 decimales)
- `currency` (obligatorio, solo `PEN`)
- `occurredAt` (obligatorio, ISO-8601 con zona horaria)

### Registro de movimiento core (`POST /v1/core-transactions`)

- `operationReference` (obligatorio, maximo 64 caracteres)
- `amount` (obligatorio, positivo, hasta 17 enteros y 2 decimales)
- `currency` (obligatorio, solo `PEN`)
- `occurredAt` (obligatorio, ISO-8601 con zona horaria)

### Consulta (`GET /v1/reconciliations/{operationReference}`)

- `operationReference` (obligatorio)

## Reglas de negocio

- Una `operationReference` identifica una sola conciliacion.
- Un pago nuevo nace en estado `PENDING`.
- La diferencia se calcula como: pago - core.
- La moneda permitida es unicamente `PEN`.
- `MATCHED`: diferencia >= 0 y <= 1.00 PEN.
- `MISMATCHED`: diferencia > 1.00 PEN o diferencia negativa.
- `MATCHED` y `MISMATCHED` son estados terminales.
- Pago duplicado: idempotente, devuelve el registro original sin modificarlo.
- Movimiento duplicado o posterior a estado terminal: idempotente, devuelve resultado confirmado.
- Movimiento sin pago previo: `404`.
- Error de validacion/formatos/JSON: `400`.

## Restricciones y supuestos

- Persiste una conciliacion por referencia (no historial de multiples movimientos por referencia).
- Rechazar JSON mal formado y propiedades desconocidas.
- Tolerancia configurable con valor por defecto `1.00 PEN`.
- No registrar cuerpos completos ni datos sensibles en logs.

## Fuera de alcance

- Ejecucion de pagos.
- Movimiento real de fondos.
- Integraciones cloud.
- Colas de eventos o notificaciones externas.
- Cierre contable, reversos y procesos batch.

