# Multi-stage build for Spring Boot backend
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install wget for healthcheck
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health 2>/dev/null || exit 1

# JVM options. Overridable so a resting deployment can cap heap (and therefore
# billed memory), e.g. JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseSerialGC".
ENV JAVA_OPTS=""

# Run the application (exec so the JVM stays PID 1 and receives signals)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

