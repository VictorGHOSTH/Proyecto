# Dockerfile - VERSIÓN FUNCIONAL PARA RENDER
FROM openjdk:17-slim AS builder

WORKDIR /app

# Copiar archivos de construcción
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Dar permisos a gradlew
RUN chmod +x gradlew

# Actualizar e instalar dependencias básicas
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Construir aplicación
RUN ./gradlew clean fatJar --no-daemon

# Imagen final
FROM openjdk:17-slim

WORKDIR /app

# Crear usuario no-root (mejores prácticas)
RUN groupadd -r ktorgroup && useradd -r -g ktorgroup ktoruser

# Copiar el JAR desde el builder
COPY --from=builder --chown=ktoruser:ktorgroup /app/build/libs/*-fat.jar app.jar

# Cambiar a usuario no-root
USER ktoruser

# Puerto expuesto
EXPOSE 8080

# Variables de entorno para optimizar memoria (importante para Render Free)
ENV JAVA_OPTS="-Xmx300m -Xms150m -XX:+UseSerialGC -Dfile.encoding=UTF-8"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]