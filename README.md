# Dealer Inventory Management API

A multi-tenant REST API built with **Spring Boot 4.0** for managing dealer inventories. Features tenant isolation, role-based access control, audit logging, rate limiting, and soft-delete support.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.0.5, Spring Security, Spring Data JPA |
| Database | PostgreSQL 17 (Docker), H2 (tests) |
| Migrations | Liquibase |
| Rate Limiting | Bucket4j (token-bucket per tenant) |
| Audit | AOP aspect with async persistence |
| Build | Maven, JaCoCo (94%+ code coverage) |
| Testing | JUnit 5, Mockito, MockMvc |

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  HTTP Request                    │
│         X-Tenant-Id: acme   X-Role: USER        │
└──────────────────────┬──────────────────────────┘
                       ▼
              ┌────────────────┐
              │ RateLimitFilter │  ← per-tenant token bucket (Bucket4j)
              └───────┬────────┘
                      ▼
         ┌─────────────────────────┐
         │ TenantAuthenticationFilter │  ← extracts tenant, sets SecurityContext
         └────────────┬──────────────┘
                      ▼
          ┌───────────────────────┐
          │   Spring Security     │  ← RBAC: hasRole('GLOBAL_ADMIN')
          └───────────┬───────────┘
                      ▼
       ┌──────────────────────────────┐
       │   RestControllers            │  ← DealerController, VehicleController,
       │   (+ AuditAspect AOP)       │     AdminController
       └──────────────┬───────────────┘
                      ▼
          ┌───────────────────────┐
          │   Service Layer       │  ← tenant-scoped business logic
          └───────────┬───────────┘
                      ▼
          ┌───────────────────────┐
          │   JPA Repositories    │  ← @SQLRestriction("deleted = false")
          └───────────┬───────────┘
                      ▼
          ┌───────────────────────┐
          │   PostgreSQL          │
          └───────────────────────┘
```

---

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080** and Liquibase auto-creates the schema.

### 3. Run Tests

```bash
./mvnw test
```

98 tests, 94%+ code coverage. Tests use H2 in-memory (no Docker needed).

---

## Multi-Tenancy

Every request **must** include the `X-Tenant-Id` header. All data is scoped to the tenant — a tenant cannot see or modify another tenant's data.

| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-Id` | ✅ Yes | Identifies the tenant (e.g., `acme-corp`) |
| `X-Role` | No | Role for RBAC. Defaults to `USER`. Use `GLOBAL_ADMIN` for admin endpoints |

---

## API Endpoints

### Dealers (`/dealers`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/dealers` | Create a dealer |
| `GET` | `/dealers` | List dealers (paginated, sorted) |
| `GET` | `/dealers/{id}` | Get dealer by ID |
| `PATCH` | `/dealers/{id}` | Partial update |
| `DELETE` | `/dealers/{id}` | Soft-delete (cascades to vehicles) |

### Vehicles (`/vehicles`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/vehicles` | Create a vehicle |
| `GET` | `/vehicles` | List with filters (paginated, sorted) |
| `GET` | `/vehicles/{id}` | Get vehicle by ID |
| `PATCH` | `/vehicles/{id}` | Partial update |
| `DELETE` | `/vehicles/{id}` | Soft-delete |

**Vehicle filters** (query params on `GET /vehicles`):
- `model` — partial match, case-insensitive
- `status` — `AVAILABLE` or `SOLD`
- `priceMin` / `priceMax` — inclusive range
- `subscription` — `PREMIUM` returns only vehicles from premium dealers

**Pagination & sorting**: `?page=0&size=10&sort=price,asc`

### Admin (`/admin`) — requires `X-Role: GLOBAL_ADMIN`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/dealers/countBySubscription` | Count dealers by BASIC/PREMIUM (cross-tenant) |
| `GET` | `/admin/dealers` | List all dealers (`?includeDeleted=true`) |
| `GET` | `/admin/dealers/{id}` | Get dealer (`?includeDeleted=true`) |
| `GET` | `/admin/vehicles` | List all vehicles (`?includeDeleted=true`) |
| `GET` | `/admin/vehicles/{id}` | Get vehicle (`?includeDeleted=true`) |

---

## Example Requests

```bash
# Create a dealer
curl -X POST http://localhost:8080/dealers \
  -H "X-Tenant-Id: acme" \
  -H "X-Role: USER" \
  -H "Content-Type: application/json" \
  -d '{"name": "Acme Motors", "email": "info@acme.com", "subscriptionType": "PREMIUM"}'

# List vehicles with filters
curl "http://localhost:8080/vehicles?status=AVAILABLE&priceMin=20000&sort=price,asc" \
  -H "X-Tenant-Id: acme"

# Admin: count by subscription
curl http://localhost:8080/admin/dealers/countBySubscription \
  -H "X-Tenant-Id: acme" \
  -H "X-Role: GLOBAL_ADMIN"
```

---

## Key Design Decisions

### Soft Delete
All entities use a `deleted` flag with `@SQLRestriction("deleted = false")`. Standard queries automatically exclude deleted records. Admin endpoints bypass this with native queries to show deleted records.

### Tenant Isolation
- `TenantContext` (ThreadLocal) stores the tenant per request
- Every repository query is scoped by `tenantId`
- `TenantAuthenticationFilter` rejects requests without `X-Tenant-Id` (400)

### Audit Logging
- AOP `@Around` aspect intercepts all `@RestController` methods
- Captures: tenant, role, HTTP method, endpoint, entity type/ID, response status, duration
- Persisted **asynchronously** (`@Async`) with `REQUIRES_NEW` propagation so failures never impact the request

### Rate Limiting
- Per-tenant token bucket using Bucket4j
- Configurable capacity (200) and refill rate (100/sec)
- Returns `429 Too Many Requests` with `Retry-After: 1` header

### Pagination
- All list endpoints return `Page<T>` with standard Spring Pageable support
- Sortable by any field (e.g., `?sort=price,desc&sort=model,asc`)

---

## Project Structure

```
src/main/java/com/dealer/dealer_inventory/
├── DealerInventoryApplication.java
├── audit/
│   ├── annotation/Audited.java          # Custom audit annotation
│   ├── aspect/AuditAspect.java          # AOP around advice
│   ├── entity/AuditLog.java, AuditAction.java
│   ├── repository/AuditLogRepository.java
│   └── service/AuditService.java        # Async persistence
├── config/
│   ├── AsyncConfig.java
│   └── SecurityConfig.java              # Filter chain, CORS, RBAC
├── exception/
│   ├── GlobalExceptionHandler.java      # Centralized error responses
│   ├── ForbiddenException.java
│   ├── MissingTenantException.java
│   ├── RateLimitExceededException.java
│   └── ResourceNotFoundException.java
├── inventory/
│   ├── controller/                      # REST endpoints
│   ├── dto/                             # Request/Response DTOs
│   ├── entity/                          # JPA entities + enums
│   ├── repository/                      # Spring Data + Specifications
│   └── service/                         # Business logic
└── security/
    ├── RateLimitFilter.java             # Bucket4j per-tenant
    ├── TenantAuthenticationFilter.java  # Header-based auth
    └── TenantContext.java               # ThreadLocal tenant holder
```

---

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `app.rate-limit.enabled` | `true` | Enable/disable rate limiting |
| `app.rate-limit.requests-per-second` | `100` | Token refill rate |
| `app.rate-limit.bucket-capacity` | `200` | Max burst capacity |
| `app.cors.allowed-origins` | `*` | CORS origins |
| `spring.task.execution.pool.core-size` | `10` | Async audit thread pool |

