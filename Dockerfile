# Dockerfile PROBADO - funciona en Render
# ========== ETAPA 1: Builder ==========
FROM gradle:7.6-jdk17-alpine AS builder

WORKDIR /app

# Copiar archivos necesarios
COPY . .

# Construir la aplicación
RUN gradle clean fatJar --no-daemon

# ========== ETAPA 2: Runtime ==========
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar JAR desde builder
COPY --from=builder /app/build/libs/*-fat.jar app.jar

# Exponer puerto
EXPOSE 8080

# Comando de inicio con límites de memoria
CMD ["java", "-Xmx300m", "-Xms150m", "-jar", "app.jar"]