# =============================================================================
# Stage 1: Build
# Uses Maven + Node.js (required by Quinoa to build the React frontend)
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Install Node.js 20 (LTS) for Quinoa's frontend build step
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pom.xml first and pre-download dependencies to leverage Docker layer caching
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

# Copy the full source tree and build the uber-jar (Quinoa will build the React UI internally)
COPY src ./src
RUN mvn -B clean package -DskipTests

# =============================================================================
# Stage 2: Runtime
# Minimal JRE-only image — just the uber-jar, nothing else
# =============================================================================
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

# Copy the Quarkus uber-jar produced by the build stage
COPY --from=builder /app/target/*-runner.jar app.jar

# Doota listens on port 8080
EXPOSE 8080

# Run as a non-root user for security
RUN addgroup --system doota && adduser --system --ingroup doota doota
USER doota

ENTRYPOINT ["java", "-jar", "app.jar"]
