# Dockerfile para Ktor en Render
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Copiar archivos de construcción
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Dar permisos a gradlew
RUN chmod +x gradlew

# Instalar dependencias si es necesario
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Descargar dependencias primero (para caché)
RUN ./gradlew dependencies --no-daemon || true

# Construir FAT JAR
RUN ./gradlew clean fatJar --no-daemon

# Imagen final más pequeña
FROM openjdk:17-jre-slim

WORKDIR /app

# Copiar JAR
COPY --from=builder /app/build/libs/*-fat.jar app.jar

# Puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/health || exit 1

# Comando de inicio
CMD ["java", "-Xmx300m", "-Xms150m", "-jar", "app.jar"]