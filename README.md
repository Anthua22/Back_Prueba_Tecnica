# Similar Products API

API REST en Spring Boot (WebFlux) que devuelve los detalles de los productos similares a uno dado.

Flujo: `Test (k6) -> :5000 (esta app) -> :3001 (mocks)`

## Requisitos

- Java 17
- Maven 3.9+
- Docker (para los mocks y k6)

## Ejecutar

```bash
# En el repo del enunciado: mocks en :3001
docker-compose up -d simulado influxdb grafana

# En este proyecto: la app en :5000
mvn spring-boot:run

# Prueba rápida
curl http://localhost:5000/product/1/similar

# Prueba de carga (desde el repo del enunciado)
docker-compose run --rm k6 run scripts/test.js
```

Resultados de k6: http://localhost:3000/d/Le2Ku9NMk/k6-performance-test

Tests unitarios: `mvn test`

## Documentación (Swagger)

- UI: http://localhost:5000/swagger-ui.html
- OpenAPI JSON: http://localhost:5000/v3/api-docs

## API

`GET /product/{productId}/similar`

| Código | Cuándo |
|---|---|
| 200 | Lista de `ProductDetail` ordenada por similitud (puede estar vacía) |
| 404 | El producto base no existe |

Es el contrato acordado con el equipo front (`similarProducts.yaml`), que solo define 200 y 404.

## Arquitectura

```
controller/  Endpoint REST
service/     Orquestación: paralelismo, orden, cachés y política de fallos
client/      Cliente HTTP no bloqueante de la API existente (WebClient)
config/      WebClient, propiedades y OpenAPI
exception/   Mapeo de errores a códigos HTTP
model/       ProductDetail
```

## Decisiones de diseño

**WebFlux (no bloqueante).** La prueba lanza 200 usuarios simultáneos. Con un modelo de un hilo por petición, cada hilo quedaría bloqueado esperando a los mocks. Con WebFlux unos pocos hilos atienden todas las peticiones.

**Detalles en paralelo y en orden.** Tras obtener los IDs, el detalle de cada producto se pide en paralelo (`flatMapSequential`, máx. 16 concurrentes por petición). Se lanzan a la vez, pero se emiten en el orden de similitud original, como exige el contrato.

**Un producto que falla no rompe la respuesta.** Si un producto similar devuelve 404, 5xx o supera el timeout, se omite y se responde 200 con el resto. Es un dato secundario y el contrato no admite otros códigos. Contrapartida: el usuario puede ver menos productos de los ideales.

**Timeouts.** Conexión 1 s y respuesta 2 s hacia la API existente, para que un mock lento no cuelgue peticiones ni agote recursos. Contrapartida: un producto que tarde más de 2 s no aparece. Ambos valores son configurables.

**Cachés (Caffeine, TTL 60 s).**
- Detalle de producto e IDs similares. Las peticiones concurrentes por el mismo dato comparten una única llamada al backend.
- Se cachean éxitos y 404 de producto; los errores transitorios (5xx, timeouts) **no** se cachean para no fijar un fallo puntual.
- Contrapartida: los datos (precio, disponibilidad) pueden estar hasta 60 s desactualizados.

**Omisión temporal de productos que fallan (5 s).** Si un producto acaba de fallar, se omite durante 5 s sin volver a llamarlo. Evita que cada petición espere el timeout completo cuando el backend está degradado. Es una versión simple de un circuit breaker.

**Reintentos solo en `/similarids`.** Hasta 2 reintentos con backoff ante fallos de conexión o 5xx, porque sin esa lista no hay respuesta posible. No se reintenta el detalle de producto: ahí es preferible omitir. Un 404 nunca se reintenta.

   
## Limitaciones y mejoras futuras

- Circuit breaker completo con Resilience4j en lugar de la omisión temporal de 5 s.
- Métricas y health checks con Actuator y Micrometer.
- Tests de integración del cliente HTTP (WireMock o MockWebServer).
- Responder 502/503 cuando la API dependiente no está disponible, en lugar de un 500.
- Externalizar a `application.properties` el TTL de fallos (hoy fijo en 5 s) y limitar la cola del pool de conexiones (hoy ilimitada).