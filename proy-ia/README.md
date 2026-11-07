# proy-conciliacion

API REST de conciliación de pagos informados y movimientos del core bancario, desarrollada con Java 21, Quarkus 3.31.2 y Maven. Utiliza H2 persistente fuera de pruebas y no ejecuta pagos ni mueve fondos.

## Requisitos

- JDK 21
- Maven Wrapper incluido

## Ejecución y pruebas

```powershell
.\mvnw.cmd quarkus:dev
.\mvnw.cmd clean test
```

La API queda disponible en `http://localhost:8080/v1`. La persistencia H2 de desarrollo se almacena bajo `data/conciliacion`; las pruebas usan una base H2 en memoria.

Endpoints: `POST /v1/payments`, `POST /v1/core-transactions`, `GET /v1/reconciliations/{operationReference}` y `GET /v1/ping`.

El proyecto puede abrirse directamente desde IntelliJ como proyecto Maven utilizando `pom.xml`.
