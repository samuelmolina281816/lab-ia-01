# Contrato REST — Servicio de conciliación

## 1. Propósito y convenciones

API para registrar pagos informados y movimientos del core bancario, y consultar la conciliación por referencia de operación. El servicio es informativo: no ejecuta pagos ni mueve fondos.

- Ruta base: `/v1`.
- Formato de solicitudes y respuestas exitosas: `application/json`.
- Formato de errores: `application/problem+json`.
- Todas las solicitudes de registro requieren `Content-Type: application/json`.
- `X-Request-ID` es opcional en la solicitud; si se envía, debe contener entre 1 y 128 caracteres. El servicio conserva su valor o genera uno si no se envió y lo devuelve en la respuesta; los errores también lo incluyen como `requestId`.
- Los timestamps de respuesta se expresan en UTC con sufijo `Z`. Los de entrada deben incluir zona u offset horario.
- Se rechazan propiedades JSON desconocidas.
- Los importes se expresan como números decimales en PEN, con máximo dos decimales y máximo 17 dígitos enteros. El consumidor debe tratarlos como decimales monetarios, no como valores binarios de punto flotante.

## 2. Modelo de conciliación

La respuesta de conciliación es un objeto JSON con estos campos:

| Campo | Tipo y formato | Presencia / significado |
|---|---|---|
| `id` | string, UUID | Identificador estable de la conciliación. |
| `operationReference` | string, 1–64 caracteres | Referencia única de operación. |
| `accountNumber` | string, máximo 34 caracteres | Cuenta del pago. |
| `paymentAmount` | number, mayor que cero, máximo 17 enteros y 2 decimales | Importe informado en el pago. |
| `coreAmount` | number, mayor que cero, máximo 17 enteros y 2 decimales | Importe recibido del core. Se omite mientras el estado sea `PENDING`. |
| `difference` | number, hasta 17 enteros y 2 decimales, puede ser negativo | Diferencia `paymentAmount - coreAmount`. Se omite mientras el estado sea `PENDING`. |
| `currency` | string, valor `PEN` | Moneda de ambos importes. |
| `status` | string, enum `PENDING`, `MATCHED`, `MISMATCHED` | Estado actual. |
| `createdAt` | string, date-time ISO-8601 | Momento de creación del registro. |
| `resolvedAt` | string, date-time ISO-8601 | Momento en que se determinó un estado terminal. Se omite mientras el estado sea `PENDING`. |

`accountNumber` y `paymentAmount` representan el pago original. Cuando `status` es `PENDING`, `coreAmount`, `difference` y `resolvedAt` no existen en la respuesta y se omiten, no se envían como `null`.

## 3. Endpoints

### 3.1 Registrar pago — `POST /v1/payments`

**Propósito:** registrar un pago informado y crear una conciliación pendiente.

**Request JSON (`PaymentRequest`)**

| Campo | Obligatorio | Tipo y validaciones |
|---|---:|---|
| `operationReference` | Sí | string no vacío, máximo 64 caracteres. |
| `accountNumber` | Sí | string no vacío, máximo 34 caracteres. |
| `amount` | Sí | number, estrictamente positivo, máximo `99999999999999999.99`, múltiplo de `0.01`. |
| `currency` | Sí | string; único valor admitido: `PEN`. |
| `occurredAt` | Sí | string ISO-8601/RFC 3339 `date-time`, con zona u offset horario. |

**Response JSON:** objeto `Reconciliation`. Para una creación nueva tendrá `status: "PENDING"` y omitirá `coreAmount`, `difference` y `resolvedAt`.

**Códigos HTTP**

- `201 Created`: pago registrado por primera vez; body con la conciliación pendiente y header `Location: /v1/reconciliations/{operationReference}`.
- `200 OK`: referencia de pago ya registrada; devuelve la conciliación canónica existente sin modificar el pago ni el estado.
- `400 Bad Request`: JSON mal formado, campos inválidos, ausentes o propiedades desconocidas.
- `415 Unsupported Media Type`: el request no usa `application/json`.
- `500 Internal Server Error`: error inesperado; body de problema sin información interna sensible.

**Idempotencia:** `operationReference` es la clave idempotente. El primer request válido crea el registro. Todo pago posterior para esa referencia devuelve el registro existente y no sobrescribe ninguno de sus datos, aunque el payload repetido sea distinto. La respuesta refleja el estado canónico actual de la conciliación.

**Ejemplo de creación — `201`**

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /v1/reconciliations/OP-2026-0001
X-Request-ID: 8d94c633-9025-4db4-b3bc-321c493f125f
```

```json
{
  "id": "14b3f791-48b3-4f20-9fa1-f31dd6ef9eb6",
  "operationReference": "OP-2026-0001",
  "accountNumber": "001234567890",
  "paymentAmount": 1250.50,
  "currency": "PEN",
  "status": "PENDING",
  "createdAt": "2026-11-05T20:30:00Z"
}
```

**Ejemplo de pago repetido — `200`**

```json
{
  "id": "14b3f791-48b3-4f20-9fa1-f31dd6ef9eb6",
  "operationReference": "OP-2026-0001",
  "accountNumber": "001234567890",
  "paymentAmount": 1250.50,
  "currency": "PEN",
  "status": "PENDING",
  "createdAt": "2026-11-05T20:30:00Z"
}
```

**Ejemplo de error de validación — `400`**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
X-Request-ID: 8d94c633-9025-4db4-b3bc-321c493f125f
```

```json
{
  "type": "https://example.com/problems/validation-error",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "Uno o más campos no cumplen las validaciones.",
  "instance": "/v1/payments",
  "requestId": "8d94c633-9025-4db4-b3bc-321c493f125f",
  "errors": [
    {
      "field": "amount",
      "message": "Debe ser positivo y tener como máximo 17 dígitos enteros y 2 decimales."
    }
  ]
}
```

### 3.2 Registrar movimiento core — `POST /v1/core-transactions`

**Propósito:** registrar el importe informado por el core para una conciliación existente y resolverla.

**Request JSON (`CoreTransactionRequest`)**

| Campo | Obligatorio | Tipo y validaciones |
|---|---:|---|
| `operationReference` | Sí | string no vacío, máximo 64 caracteres. |
| `amount` | Sí | number, estrictamente positivo, máximo `99999999999999999.99`, múltiplo de `0.01`. |
| `currency` | Sí | string; único valor admitido: `PEN`. |
| `occurredAt` | Sí | string ISO-8601/RFC 3339 `date-time`, con zona u offset horario. |

**Response JSON:** objeto `Reconciliation`. La diferencia se calcula como `paymentAmount - coreAmount`. La tolerancia configurable tiene valor predeterminado `1.00 PEN`: `MATCHED` si la diferencia es mayor o igual que cero y menor o igual que la tolerancia; `MISMATCHED` si es negativa o supera la tolerancia.

**Códigos HTTP**

- `200 OK`: movimiento aplicado, repetido o recibido para conciliación terminal; body con el resultado canónico.
- `400 Bad Request`: JSON mal formado, campos inválidos, ausentes o propiedades desconocidas.
- `404 Not Found`: no existe un pago previo para `operationReference`; no se crea conciliación.
- `415 Unsupported Media Type`: el request no usa `application/json`.
- `500 Internal Server Error`: error inesperado; body de problema sin información interna sensible.

**Idempotencia y estados terminales:** un movimiento asociado a una referencia existente resuelve una conciliación pendiente. Un reintento o cualquier movimiento posterior a `MATCHED` o `MISMATCHED` devuelve el resultado canónico confirmado sin crear otro registro ni modificarlo. Los estados terminales son inmutables.

**Ejemplo exitoso — `200`**

```http
HTTP/1.1 200 OK
Content-Type: application/json
X-Request-ID: 77daac81-1b33-4534-8eb4-3034c20e2f5e
```

```json
{
  "id": "14b3f791-48b3-4f20-9fa1-f31dd6ef9eb6",
  "operationReference": "OP-2026-0001",
  "accountNumber": "001234567890",
  "paymentAmount": 1250.50,
  "coreAmount": 1250.00,
  "difference": 0.50,
  "currency": "PEN",
  "status": "MATCHED",
  "createdAt": "2026-11-05T20:30:00Z",
  "resolvedAt": "2026-11-05T20:31:12Z"
}
```

**Ejemplo de movimiento sin pago — `404`**

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json
X-Request-ID: 77daac81-1b33-4534-8eb4-3034c20e2f5e
```

```json
{
  "type": "https://example.com/problems/payment-not-found",
  "title": "Pago no encontrado",
  "status": 404,
  "detail": "No existe un pago previo para la referencia de operación indicada.",
  "instance": "/v1/core-transactions",
  "requestId": "77daac81-1b33-4534-8eb4-3034c20e2f5e"
}
```

### 3.3 Consultar conciliación — `GET /v1/reconciliations/{operationReference}`

**Propósito:** obtener el estado consolidado actual de una conciliación por su referencia.

**Path parameter**

| Campo | Obligatorio | Tipo y validaciones |
|---|---:|---|
| `operationReference` | Sí | string no vacío, máximo 64 caracteres; debe codificarse como segmento de URL. |

No tiene request body. La respuesta exitosa es el objeto `Reconciliation`; si está `PENDING`, omite `coreAmount`, `difference` y `resolvedAt`.

**Códigos HTTP**

- `200 OK`: conciliación encontrada.
- `400 Bad Request`: referencia vacía, mayor de 64 caracteres o path inválido.
- `404 Not Found`: no existe una conciliación para la referencia.
- `500 Internal Server Error`: error inesperado; body de problema sin información interna sensible.

**Idempotencia:** operación de consulta, no modifica el estado ni crea registros.

**Ejemplo exitoso — `200`**

```http
GET /v1/reconciliations/OP-2026-0001 HTTP/1.1
X-Request-ID: 83ffb15a-2dbb-46c7-b34d-5d821afbb4e1
```

```json
{
  "id": "14b3f791-48b3-4f20-9fa1-f31dd6ef9eb6",
  "operationReference": "OP-2026-0001",
  "accountNumber": "001234567890",
  "paymentAmount": 1250.50,
  "coreAmount": 1250.00,
  "difference": 0.50,
  "currency": "PEN",
  "status": "MATCHED",
  "createdAt": "2026-11-05T20:30:00Z",
  "resolvedAt": "2026-11-05T20:31:12Z"
}
```

**Ejemplo de referencia inexistente — `404`**

```json
{
  "type": "https://example.com/problems/reconciliation-not-found",
  "title": "Conciliación no encontrada",
  "status": 404,
  "detail": "No existe una conciliación para la referencia de operación indicada.",
  "instance": "/v1/reconciliations/OP-2026-9999",
  "requestId": "83ffb15a-2dbb-46c7-b34d-5d821afbb4e1"
}
```

## 4. Formato común de errores

Todos los errores de API usan `Content-Type: application/problem+json` y contienen:

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---:|---|
| `type` | string, URI | Sí | Identificador del tipo de problema. |
| `title` | string | Sí | Resumen legible del problema. |
| `status` | integer | Sí | Código HTTP de la respuesta. |
| `detail` | string | Sí | Explicación segura y específica para esta ocurrencia. |
| `instance` | string, URI-reference | Sí | Ruta de la solicitud que produjo el error. |
| `requestId` | string | Sí | Identificador de trazabilidad, igual al `X-Request-ID` de respuesta. |
| `errors` | array de `{field, message}` | No | Detalles de validación por campo. Se omite cuando no aplica. |

No se deben incluir cuerpos completos, números de cuenta, stack traces ni otros datos sensibles en logs o detalles de error.

## 5. Supuestos de diseño adoptados

- `id` se representa como UUID; el contrato lo considera estable y opaco para el consumidor.
- `POST /payments` devuelve `201` al crear la conciliación y `200` al repetir una referencia ya registrada.
- `POST /core-transactions` devuelve `200` con el resultado canónico tanto al resolver como en reintentos o solicitudes posteriores a un estado terminal.
- La misma `operationReference` identifica una única conciliación. Un pago repetido nunca sustituye el pago original; luego de resolver, ningún movimiento puede alterar el resultado terminal.
- Los ejemplos `example.com` en `type` son identificadores ilustrativos que deben sustituirse por URIs controladas por el servicio antes de producción.
