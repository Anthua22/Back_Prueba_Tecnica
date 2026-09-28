# Similar Products API

Spring Boot (WebFlux) en el puerto **5000**. Flujo: `Test -> :5000 (esta app) -> :3001 (mocks)`.

## Ejecutar
```bash
docker-compose up -d simulado influxdb grafana   # mocks en :3001 (repo del enunciado)
mvn spring-boot:run                              # esta app en :5000
docker-compose run --rm k6 run scripts/test.js   # prueba de carga
```
Tests unitarios: `mvn test`

## Documentación (Swagger)
- UI: http://localhost:5000/swagger-ui.html
- OpenAPI JSON: http://localhost:5000/v3/api-docs

## Diseño
- `GET /product/{productId}/similar` -> 200 con `ProductDetail[]` ordenado por similitud; 404 si el producto base no existe.
- **Rendimiento**: WebFlux/Netty no bloqueante, pool de conexiones HTTP, detalles de producto en paralelo (`flatMapSequential` conserva el orden) y caché Caffeine (TTL) que además unifica llamadas concurrentes al mismo ID.
- **Resiliencia**: timeouts de conexión y respuesta; un producto similar que da 404, 5xx o timeout se omite y se devuelve el resto; los fallos no se cachean.
- Todo configurable en `application.properties`.
