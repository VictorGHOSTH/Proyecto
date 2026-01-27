# Dockerfile CORREGIDO
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Copiar todo
COPY . .

# Dar permisos a gradlew
RUN chmod +x gradlew

# Construir (usa --no-daemon para Docker)
RUN ./gradlew clean fatJar --no-daemon

# Runtime
FROM openjdk:17-jre-slim

WORKDIR /app

# Copiar JAR usando patrón (así funciona con cualquier versión)
COPY --from=builder /app/build/libs/*-fat.jar app.jar

# Puerto
EXPOSE 8080

# Comando con límites de memoria para Render Free
CMD ["java", "-Xmx300m", "-Xms150m", "-jar", "app.jar"]