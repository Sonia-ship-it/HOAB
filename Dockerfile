# ==========================================
# Stage 1: Build JAR using Maven
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Cache Maven dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and package application (skip tests during container build as tests run in CI)
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Extract layered Spring Boot application for fast startup
RUN java -Djarmode=layertools -jar target/*.jar extract

# ==========================================
# Stage 2: Minimal Production JRE Runtime
# ==========================================
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="Amahoro General Hospital DevOps Team <devops@amahoro.gov.rw>"
LABEL project="Hospital Online Appointment Booking System (HOABS)"

# Run as non-privileged system user for security hardening
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

# Copy layered spring application
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

# Expose Spring Boot port
EXPOSE 8080

# Configure production JVM flags
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
