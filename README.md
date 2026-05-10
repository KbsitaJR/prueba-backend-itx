# Similar Products API

Spring Boot microservice that exposes `GET /product/{productId}/similar`, returning similar products with their details by orchestrating two existing APIs.

## Quick Start

```bash
mvn spring-boot:run
curl http://localhost:5000/product/1/similar
```

## Tech Stack

| Technology | Purpose |
|---|---|
| Spring Boot 3.4 | Framework |
| Java 21 | Virtual threads |
| Maven | Build |
| RestClient + JdkClientHttpRequestFactory | HTTP client |

## API

### `GET /product/{productId}/similar`

Returns similar products to a given one, ordered by similarity.

**Response `200`:**

```json
[
  { "id": "2", "name": "Dress", "price": 19.99, "availability": true },
  { "id": "3", "name": "Blazer", "price": 29.99, "availability": false }
]
```

**Response `404`** (product not found):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product not found: 999"
}
```

## Testing with Docker

Prerequisites: Docker Desktop with the [backendDevTest](https://github.com/dalogax/backendDevTest) repository.

```bash
# 1. Start mocks
docker compose up -d simulado influxdb grafana

# 2. Start the app
mvn spring-boot:run

# 3. Run performance test
docker compose run --rm k6 run scripts/test.js

# 4. View results
open http://localhost:3000/d/Le2Ku9NMk/k6-performance-test
```

## Configuration (`application.yml`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `5000` | App port |
| `external-api.base-url` | `http://localhost:3001` | Mock APIs base URL |
| `external-api.connect-timeout` | `2s` | Connection timeout |
| `external-api.read-timeout` | `3s` | Read timeout |
| `app.per-product-timeout` | `3000` | Per-product fetch timeout (ms) |

## Architecture

```
Controller → Service → ExternalApiClient → simulado mock (port 3001)
```

1. `ProductController` receives `GET /product/{productId}/similar`
2. `ProductService` calls `ExternalApiClient.fetchSimilarIds(productId)`
3. For each similar ID, fetches product detail **in parallel** via virtual threads
4. Each fetch has a 3s individual timeout — slow products are skipped
5. Products that return 404/500 are skipped gracefully
6. Returns the consolidated list of `ProductDetail`

## Key Decisions

**Virtual threads + CompletableFuture**: Java 21 virtual threads enable lightweight parallel fetching without thread pool management overhead. Each product detail is fetched in its own virtual thread via `CompletableFuture.supplyAsync`.

**3s per-product timeout**: Slow products (e.g., 5s or 50s delays) fail fast via `CompletableFuture.orTimeout()` without blocking other fetches. The p95 response time stays at ~3s under 200 concurrent users.

**Graceful degradation**: Individual product failures (404, 500, timeout) never cascade. They're caught, logged, and excluded from the response.

**RFC 7807 errors**: All error responses use `ProblemDetail` for consistent, standards-compliant error payloads.
