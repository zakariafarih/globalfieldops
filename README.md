# GlobalField Ops

A multinational field-operations platform built with **Java 21, Spring Boot 3.5, Spring Cloud Gateway, PostgreSQL 16, Docker, and Kubernetes**.

Dispatchers create work orders, assign technicians with downstream validation, track status transitions, and preserve an immutable audit trail — all across country boundaries.

## Why This Project Exists

This is a deliberately scoped microservices system designed to demonstrate:
- **Bounded service responsibilities** with clean Spring Boot layering (controller → service → repository)
- **Inter-service communication** via Spring 6 `RestClient` with proper error mapping
- **JWT-secured API gateway** with role-based access control (RSA asymmetric keys)
- **Kubernetes-native deployment** with Service DNS discovery, probes, and externalized config
- **Production-minded testing** with JUnit 5, MockMvc, and Testcontainers (PostgreSQL — never H2)
- **Observability** with correlation ID propagation, structured logging, and custom Micrometer metrics

Every class, endpoint, and manifest has a reason. No decorative complexity.

## Architecture

```
              ┌──────────────┐
 Internet ───►│  api-gateway │  (JWT validation, routing, correlation IDs)
              └──────┬───────┘
       ┌─────────────┼─────────────┐
       ▼             ▼             ▼
 ┌───────────┐ ┌───────────┐ ┌───────────┐
 │ technician│ │ work-order│ │   audit   │
 │  service  │ │  service  │ │  service  │
 └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
       ▼             ▼             ▼
   [postgres]    [postgres]    [postgres]
```

| Service | Port | Responsibility |
|---------|------|----------------|
| api-gateway | 8080 | JWT validation, request routing, correlation ID generation |
| technician-service | 8081 | Technician CRUD, skills, availability, activation |
| work-order-service | 8082 | Work orders, status transitions, assignment with downstream validation |
| audit-service | 8083 | Immutable audit event storage |

### Core Business Flow

```
Create Technician → Create Work Order → Assign Technician → Status Transitions → Audit Trail
```

Assignment triggers a cross-service call: `work-order-service` validates the technician (exists, active, available, same country) by calling `technician-service`, then persists the assignment and records an audit event through `audit-service`.

## Tech Stack

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Java 21 | Modern LTS with records, sealed types |
| Framework | Spring Boot 3.5 | Standard enterprise framework |
| Gateway | Spring Cloud Gateway (MVC) | Servlet-based, same ecosystem |
| Security | Spring Security (JWT Resource Server) | RSA asymmetric key validation |
| Persistence | Spring Data JPA + PostgreSQL 16 | Real relational modeling |
| Migrations | Flyway | Versioned, explicit schema evolution |
| HTTP Client | Spring 6 RestClient | Modern, blocking, clean |
| Mapping | Manual mapper classes | Explainable line-by-line in interviews |
| Testing | JUnit 5 + MockMvc + Testcontainers | Unit, web, and real-DB integration |
| Containers | Docker + Docker Compose | Multi-stage builds, non-root images |
| Orchestration | Kubernetes (minikube) | Deployments, Services, ConfigMaps, Secrets, probes |
| Observability | Actuator + Micrometer + MDC logging | Health, metrics, correlation IDs |

## Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Build and start all 7 containers
docker compose -f infrastructure/docker-compose.yml up --build -d

# Verify all services are healthy (~30s)
docker compose -f infrastructure/docker-compose.yml ps

# Test the flow
curl -X POST http://localhost:8081/api/technicians \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"TECH-001","email":"tech@gfo.com","firstName":"Jane","lastName":"Doe","countryCode":"US"}'

curl -X POST http://localhost:8082/api/work-orders \
  -H "Content-Type: application/json" \
  -d '{"title":"Fix pump","summary":"Hydraulic pump replacement","priority":"HIGH","countryCode":"US"}'

# Assign (replace IDs from responses above)
curl -X POST http://localhost:8082/api/work-orders/{woId}/assign/{techId}

# Check audit trail
curl http://localhost:8083/api/audit-events

# Gateway enforces JWT (returns 401)
curl -o /dev/null -w "%{http_code}" http://localhost:8080/api/technicians
```

### Option 2: Kubernetes (Minikube)

```bash
minikube start --cpus=4 --memory=6144 --driver=docker

# Load pre-built images
minikube image load infrastructure-technician-service:latest
minikube image load infrastructure-work-order-service:latest
minikube image load infrastructure-audit-service:latest
minikube image load infrastructure-api-gateway:latest

# Deploy
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/postgres.yaml
kubectl wait --for=condition=ready pod -l app=postgres-technician -n globalfieldops --timeout=120s
kubectl wait --for=condition=ready pod -l app=postgres-workorder -n globalfieldops --timeout=120s
kubectl wait --for=condition=ready pod -l app=postgres-audit -n globalfieldops --timeout=120s
kubectl apply -f infrastructure/k8s/technician-service/
kubectl apply -f infrastructure/k8s/audit-service/
kubectl apply -f infrastructure/k8s/work-order-service/
kubectl apply -f infrastructure/k8s/gateway/

# All 7 pods should be Running
kubectl get pods -n globalfieldops
```

### Option 3: Run Individually

```bash
# Start databases
cd infrastructure && docker compose up -d postgres-technician postgres-workorder postgres-audit

# Run any service
cd services/technician-service && ./mvnw spring-boot:run
```

### Run Tests

```bash
# Single service
cd services/technician-service && ./mvnw test

# All services (requires Docker for Testcontainers)
for svc in technician-service work-order-service audit-service api-gateway; do
  (cd services/$svc && ./mvnw test)
done
```

## Test Coverage

142 tests across all services:

| Layer | What's Tested |
|-------|--------------|
| Unit (Service) | Business rules, status transitions, assignment validation |
| Web (MockMvc) | HTTP status codes, validation errors, response shapes |
| Integration (Testcontainers) | Full request → DB → response with real PostgreSQL |
| Gateway (WireMock) | Route forwarding, JWT enforcement, correlation ID propagation |

## Project Structure

```
globalfield-ops/
├── docs/
│   ├── architecture.md          # Service boundaries, design decisions
│   ├── api-contracts.md         # Endpoints, payloads, auth rules
│   ├── deployment.md            # Docker, K8s, AWS target
│   └── interview-defense.md     # Trade-off explanations (20+ Q&A)
├── infrastructure/
│   ├── docker-compose.yml       # Full local stack (7 containers)
│   └── k8s/                     # Namespace, deployments, services, configs
├── services/
│   ├── api-gateway/             # Spring Cloud Gateway MVC
│   ├── technician-service/      # Technician domain
│   ├── work-order-service/      # Work order domain + cross-service calls
│   └── audit-service/           # Immutable audit log
└── shared/
    └── platform-common/         # Cross-cutting exceptions only
```

## Key Design Decisions

| Decision | Chosen | Why |
|----------|--------|-----|
| K8s DNS over Eureka | Native platform discovery | No extra infrastructure process |
| JWT Resource Server over custom auth | Focus on API protection | Auth server is a different project |
| Synchronous HTTP over Kafka | Simplicity for v1 | Messaging is a documented next step |
| Manual mapping over MapStruct | Interview explainability | No codegen magic |
| PostgreSQL + Testcontainers over H2 | Real database behavior | Fake DBs create fake confidence |
| Narrow shared library | Prevent exception drift | Business logic stays in owning service |


## Phases Completed

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Foundation & bootstrap | ✅ |
| 1 | technician-service (CRUD, skills, availability) | ✅ |
| 2 | work-order-service (status transitions, comments) | ✅ |
| 3 | Inter-service assignment flow | ✅ |
| 4 | audit-service (immutable event log) | ✅ |
| 5 | Gateway security (JWT, roles, routing) | ✅ |
| 6 | Observability (metrics, probes, correlation logging) | ✅ |
| 7 | Docker Compose (multi-stage builds, healthchecks) | ✅ |
| 8 | Kubernetes (minikube, probes, DNS discovery) | ✅ |

## License

Private portfolio project — not for redistribution.
