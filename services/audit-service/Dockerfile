# ── Build stage ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Install Maven (Alpine)
RUN apk add --no-cache maven

# Copy platform-common first (dependency for this service)
COPY shared/platform-common/ shared/platform-common/
RUN mvn -f shared/platform-common/pom.xml install -q

# Copy service source and build
COPY services/audit-service/ services/audit-service/
RUN mvn -f services/audit-service/pom.xml package -DskipTests -q

# ── Runtime stage ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /workspace/services/audit-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
