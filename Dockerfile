# Dockerfile CORREGIDO
FROM openjdk:17-slim AS builder

WORKDIR /app

# Copiar archivos necesarios para caché
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Dar permisos
RUN chmod +x gradlew

# Instalar dependencias para build
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Descargar dependencias
RUN ./gradlew dependencies --no-daemon || true

# Construir
RUN ./gradlew clean fatJar --no-daemon

# Runtime - usa la MISMA imagen (más simple)
FROM openjdk:17-slim

WORKDIR /app

# Instalar curl para health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiar JAR
COPY --from=builder /app/build/libs/*-fat.jar app.jar

# Puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/health || exit 1

# Comando con límites de memoria
CMD ["java", "-Xmx256m", "-Xms128m", "-XX:+UseSerialGC", "-jar", "app.jar"]