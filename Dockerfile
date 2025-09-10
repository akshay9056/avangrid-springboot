# -------- Stage 1: Build with Maven --------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies first (caching layer)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests

# -------- Stage 2: Runtime with JRE only --------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port Render/Heroku style
EXPOSE 8080

# Render provides PORT dynamically, map it
ENV PORT=8080

# Run Spring Boot app
ENTRYPOINT ["java","-jar","app.jar"]
