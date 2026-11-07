# Estandares para generar `rest-conciliacion`

## Historia de usuario

Formato obligatorio:

Como [rol]
Quiero [accion]
Para [beneficio]

## Criterios de aceptacion

Formato obligatorio:

Dado
Cuando
Entonces

Cantidad minima obligatoria: **10** criterios de aceptacion.

## Contrato API (etapa de diseno)

Para este flujo, en la etapa de diseno se requiere:

- Un contrato REST descriptivo en Markdown (`docs/contrato-rest.md`).
- Una especificacion OpenAPI 3.0.3 (`src/main/resources/openapi/openapi.yaml`) alineada al contrato descriptivo.

En esta etapa no se permite generacion automatica de interfaces ni codigo a partir de OpenAPI.

El contrato debe incluir:

- Endpoints
- Requests y responses
- Validaciones
- Codigos HTTP
- Errores `application/problem+json`

## Desarrollo

- Java 21
- Quarkus
- Maven

## Calidad

- Clean Code
- Principios SOLID
- Manejo de excepciones
- Logging sin datos sensibles
- Inyeccion por constructor
- Separacion de responsabilidades (hexagonal)

## Testing

- JUnit 5
- Mockito
- Pruebas de dominio
- Pruebas de casos de uso
- Pruebas HTTP con `@QuarkusTest` sin RestAssured

## Restricciones para este flujo

- No usar servicios cloud
- No usar contenedores
- No usar Kafka ni Redis
- No usar PostgreSQL
- No usar Lombok
- No usar RestAssured
- No usar Checkstyle ni Spotless

