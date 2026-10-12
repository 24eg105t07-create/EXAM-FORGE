# Multi-stage Dockerfile for ExamForge Spring Boot Backend
# Stage 1: Build JAR with Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create unprivileged application user
RUN addgroup -S examforge && adduser -S examforge -G examforge

# Create uploads directory with correct permissions
RUN mkdir -p /app/uploads && chown -R examforge:examforge /app

COPY --from=builder /app/target/examforge-backend-*.jar app.jar
RUN chown examforge:examforge app.jar

USER examforge

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=mysql \
    JAVA_OPTS="-Xms256m -Xmx768m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
