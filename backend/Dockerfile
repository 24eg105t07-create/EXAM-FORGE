# Multi-stage Dockerfile for ExamForge Spring Boot Backend
# Stage 1: Build JAR with Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create unprivileged application user & directories
RUN addgroup -S examforge && adduser -S examforge -G examforge
RUN mkdir -p /app/uploads /app/data && chown -R examforge:examforge /app

COPY --from=builder /app/target/examforge-backend-*.jar app.jar
RUN chown examforge:examforge app.jar

USER examforge

EXPOSE 10000 8080

ENV SPRING_PROFILES_ACTIVE=dev \
    PORT=10000 \
    AI_PROVIDER=LOCAL_DEMO \
    JAVA_OPTS="-Xms128m -Xmx384m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
