import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "2.3.9"

    // Core
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")


    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // HTML y páginas de error
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")  // ✅ Quita -jvm
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")   // ✅ Quita -jvm

    // Kotlinx HTML
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.9.1")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")

    // Para soporte de java.time en Exposed
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.3")

    // HikariCP (pool de conexiones - opcional pero recomendado)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Testing
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("com.example.MainKt")
}

// ✅ CONFIGURACIÓN PARA JAR EJECUTABLE (FAT JAR)
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Version" to project.version
        )
    }

    // Crear un FAT JAR (incluye dependencias)
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    // Estrategia para duplicados
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Excluir archivos de firma
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// ✅ TAREA PARA CREAR JAR CON TODAS LAS DEPENDENCIAS
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("ktor-app")
    archiveClassifier.set("fat")

    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// ✅ CONFIGURACIÓN PARA PRODUCCIÓN
tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "-Xmx300m",  // Menos memoria para Render Free
        "-Xms150m",
        "-XX:+UseSerialGC",  // GC más ligero
        "-Dfile.encoding=UTF-8"
    )
}

// ✅ Asegurar que 'assemble' cree el fatJar
tasks.named("assemble") {
    dependsOn("fatJar")
}