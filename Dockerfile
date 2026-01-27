# Dockerfile SIMPLIFICADO
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar todo
COPY . .

# Dar permisos
RUN chmod +x gradlew

# Instalar curl para health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Construir
RUN ./gradlew clean fatJar --no-daemon

# Puerto
EXPOSE 8080

# Comando
CMD ["java", "-Xmx300m", "-Xms150m", "-jar", "build/libs/*-fat.jar"]