# ── Build stage ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Install Maven (Alpine)
RUN apk add --no-cache maven

# Copy service source and build (no platform-common dependency)
COPY services/api-gateway/ services/api-gateway/
RUN mvn -f services/api-gateway/pom.xml package -DskipTests -q

# ── Runtime stage ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /workspace/services/api-gateway/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
