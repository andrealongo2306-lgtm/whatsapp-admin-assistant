# Multi-stage build per ridurre dimensione immagine finale

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia solo pom.xml per sfruttare cache Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia sorgenti e compila
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crea utente non-root per sicurezza
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia JAR compilato
COPY --from=build /app/target/*.jar app.jar

# Crea directory per tokens Gmail
#RUN mkdir -p /app/tokens

# Espone porta
#EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/whatsapp/health || exit 1

# Entrypoint
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
