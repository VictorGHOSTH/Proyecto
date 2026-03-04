// Main.kt - BREADCRUMBS con DISEÑO MINIMALISTA CON CARRUSEL AUTOMÁTICO
package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.html.*
import kotlinx.html.*
import io.ktor.http.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.uri
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import java.time.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.net.URLEncoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.server.request.receiveText
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.http.content.TextContent
import io.ktor.http.ContentType
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.response.respondText
import io.ktor.server.response.respond
import kotlinx.serialization.encodeToString
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveParameters
import kotlinx.serialization.StringFormat


// Data class para almacenar los datos del formulario tradicional
data class FormData(
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val mensaje: String = "",
    val recaptchaToken: String = "",
    val terminosAceptados: Boolean = false
)

// Data class para el formulario CRUD de personas
@Serializable
data class PersonaData(
    val id: Int = 0,
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val genero: String = "",
    val estado: String = ""
)


@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val currentPage: Int,
    val pageSize: Int,
    val totalRegistros: Long,
    val totalPages: Int
)
// Validador simple del formulario
object FormValidator {
    fun validate(formData: FormData): Boolean {
        // Validaciones básicas
        val nombreRegex = Regex("^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]+\$")
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        val mensajeRegex = Regex("^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]*\$")

        return when {
            formData.nombre.isBlank() -> false
            !nombreRegex.matches(formData.nombre) -> false
            formData.nombre.length > 40 -> false
            formData.email.isBlank() -> false
            !emailRegex.matches(formData.email) -> false
            formData.telefono.isBlank() -> false
            !formData.telefono.all { it.isDigit() } -> false
            formData.telefono.length != 10 -> false
            !mensajeRegex.matches(formData.mensaje) -> false
            formData.mensaje.split("\\s+".toRegex()).size > 20 -> false
            formData.recaptchaToken.isBlank() -> false
            !formData.terminosAceptados -> false
            else -> true
        }
    }

    fun validatePersona(persona: PersonaData): Boolean {
        val nombreRegex = Regex("^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]+\$")
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        val telefonoRegex = Regex("^\\d{8,15}\$")

        return when {
            persona.nombre.isBlank() -> false
            !nombreRegex.matches(persona.nombre) -> false
            persona.nombre.length > 50 -> false
            persona.apellido.isBlank() -> false
            !nombreRegex.matches(persona.apellido) -> false
            persona.apellido.length > 50 -> false
            persona.email.isBlank() -> false
            !emailRegex.matches(persona.email) -> false
            persona.telefono.isBlank() -> false
            !telefonoRegex.matches(persona.telefono) -> false
            persona.genero.isBlank() -> false
            persona.genero !in listOf("Masculino", "Femenino", "Otro") -> false
            persona.estado !in listOf("Activo", "Inactivo") -> false
            else -> true
        }
    }
}

// Definición de la tabla PalabrasGuardadas
object PalabrasGuardadasTable : Table() {
    val id = integer("id").autoIncrement()
    val palabra = text("palabra")
    val fechaCreacion = datetime("fecha_creacion").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

// Definición de la tabla para personas CRUD
object PersonasTable : Table() {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 50)
    val apellido = varchar("apellido", 50)
    val email = varchar("email", 100).uniqueIndex()
    val telefono = varchar("telefono", 15)
    val genero = varchar("genero", 10)
    val estado = varchar("estado", 10).default("Activo")
    val fechaCreacion = datetime("fecha_creacion").defaultExpression(CurrentDateTime)
    val fechaActualizacion = datetime("fecha_actualizacion").nullable()

    override val primaryKey = PrimaryKey(id)
}

fun main() {
    println("Iniciando servidor en puerto 8080...")

    embeddedServer(Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = "0.0.0.0"
    ) {

        // CONFIGURAR BASE DE DATOS PRIMERO
        configureDatabase()

        configureStatusPages()
        routing {
            intercept(ApplicationCallPipeline.Call) {
                try {
                    val requestUri = call.request.uri
                    val method = call.request.httpMethod.value
                    val startTime = System.currentTimeMillis()
                    val clientIp = call.request.origin.remoteHost

                    println("[$clientIp] $method $requestUri")

                    if (requestUri.contains("..") || requestUri.contains("//")) {
                        call.respond(HttpStatusCode.BadRequest, "Ruta inválida")
                        return@intercept
                    }

                    proceed()

                    val duration = System.currentTimeMillis() - startTime
                    val status = call.response.status()?.value ?: 200
                    println("[$clientIp] $method $requestUri - $status (${duration}ms)")

                } catch (e: Exception) {
                    println("ERROR en interceptor: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error interno del servidor")
                }
            }

            // ========== CONFIGURAR ARCHIVOS ESTÁTICOS ==========
            get("/static/{file...}") {
                val filePath = call.parameters.getAll("file")?.joinToString("/")
                    ?: return@get call.respondText("No se especificó archivo", status = HttpStatusCode.BadRequest)

                println("📁 Sirviendo archivo estático: $filePath")
                println("🔍 Buscando en classpath: static/$filePath")

                try {
                    val classLoader = Thread.currentThread().contextClassLoader
                    val resource = classLoader.getResource("static/$filePath")

                    println("📌 Resource encontrado: ${resource != null}")

                    if (resource != null) {
                        // Determinar Content-Type
                        val contentType = when {
                            filePath.endsWith(".js") -> ContentType.Application.JavaScript
                            filePath.endsWith(".css") -> ContentType.Text.CSS
                            filePath.endsWith(".html") -> ContentType.Text.Html
                            filePath.endsWith(".png") -> ContentType.Image.PNG
                            filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") -> ContentType.Image.JPEG
                            filePath.endsWith(".gif") -> ContentType.Image.GIF
                            filePath.endsWith(".svg") -> ContentType.Image.SVG
                            filePath.endsWith(".json") -> ContentType.Application.Json
                            else -> ContentType.Text.Plain
                        }

                        call.response.header("Content-Type", contentType.toString())

                        if (resource.protocol == "jar") {
                            val tempFile = java.io.File.createTempFile("static_", filePath.substringAfterLast("/"))
                            tempFile.deleteOnExit()
                            resource.openStream().use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                            call.respondFile(tempFile)
                        } else {
                            call.respondFile(java.io.File(resource.toURI()))
                        }
                    } else {
                        println("❌ NO ENCONTRADO: static/$filePath")

                        // DEBUG: Listar recursos disponibles
                        val resources = classLoader.getResources("static")
                        println("📂 Recursos en 'static':")
                        while (resources.hasMoreElements()) {
                            println("   - ${resources.nextElement()}")
                        }

                        call.respondText("Archivo no encontrado: $filePath", status = HttpStatusCode.NotFound)
                    }
                } catch (e: Exception) {
                    println("❌ Error: ${e.message}")
                    e.printStackTrace()
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            // ========== PÁGINA PRINCIPAL CON DISEÑO MINIMALISTA ==========
            get("/") {
                println("Sirviendo página principal")

                call.respondHtml {
                    head {
                        title { +"Sistema de Breadcrumbs" }
                        style {
                            unsafe {
                                +"""
                                /* ===== RESET Y TIPOGRAFÍA ===== */
                                * {
                                    margin: 0;
                                    padding: 0;
                                    box-sizing: border-box;
                                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                                }
                                
                                body {
                                    background: #ffffff;
                                    color: #000000;
                                    line-height: 1.6;
                                    min-height: 100vh;
                                }
                                
                                /* ===== BARRA DE MENÚ SUPERIOR ===== */
                                .top-menu-bar {
                                    background: #212121;
                                    color: white;
                                    padding: 0 20px;
                                    position: sticky;
                                    top: 0;
                                    z-index: 1000;
                                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                                }
                                
                                .menu-container {
                                    max-width: 1200px;
                                    margin: 0 auto;
                                    display: flex;
                                    justify-content: space-between;
                                    align-items: center;
                                    height: 60px;
                                }
                                
                                .menu-logo a {
                                    color: white;
                                    text-decoration: none;
                                    font-size: 1.25rem;
                                    font-weight: 300;
                                    letter-spacing: -0.5px;
                                }
                                
                                .menu-items {
                                    display: flex;
                                    gap: 30px;
                                    height: 100%;
                                    align-items: center;
                                }
                                
                                .menu-item {
                                    position: relative;
                                    height: 100%;
                                    display: flex;
                                    align-items: center;
                                }
                                
                                .menu-item > a {
                                    color: #e0e0e0;
                                    text-decoration: none;
                                    font-size: 0.95rem;
                                    padding: 0 10px;
                                    transition: color 0.2s;
                                    height: 100%;
                                    display: flex;
                                    align-items: center;
                                }
                                
                                .menu-item > a:hover {
                                    color: white;
                                }
                                
                                /* Submenú */
                                .submenu {
                                    position: absolute;
                                    top: 60px;
                                    left: 0;
                                    background: white;
                                    min-width: 200px;
                                    border-radius: 4px;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                                    opacity: 0;
                                    visibility: hidden;
                                    transform: translateY(-10px);
                                    transition: all 0.3s ease;
                                    z-index: 1001;
                                }
                                
                                .menu-item:hover .submenu {
                                    opacity: 1;
                                    visibility: visible;
                                    transform: translateY(0);
                                }
                                
                                .submenu a {
                                    display: block;
                                    padding: 12px 16px;
                                    color: #212121;
                                    text-decoration: none;
                                    font-size: 0.9rem;
                                    border-bottom: 1px solid #e0e0e0;
                                    transition: background 0.2s;
                                }
                                
                                .submenu a:last-child {
                                    border-bottom: none;
                                }
                                
                                .submenu a:hover {
                                    background: #f5f5f5;
                                }
                                
                                /* Indicador de submenú */
                                .has-submenu::after {
                                    content: '▼';
                                    font-size: 0.7rem;
                                    margin-left: 6px;
                                    color: #9e9e9e;
                                }
                                
                                /* ===== SECCIÓN DE BIENVENIDA ===== */
                                .welcome-section {
                                    background: #f5f5f5;
                                    border: 1px solid #e0e0e0;
                                    border-radius: 8px;
                                    padding: 30px;
                                    margin: 30px 0;
                                    text-align: center;
                                }
                                
                                .welcome-title {
                                    font-size: 2rem;
                                    font-weight: 300;
                                    color: #212121;
                                    margin-bottom: 15px;
                                }
                                
                                .welcome-text {
                                    font-size: 1.1rem;
                                    color: #616161;
                                    max-width: 600px;
                                    margin: 0 auto;
                                    line-height: 1.6;
                                }
                                
                                .welcome-icon {
                                    font-size: 3rem;
                                    margin-bottom: 20px;
                                    color: #212121;
                                }
                                
                                /* ===== LAYOUT PRINCIPAL ===== */
                                .main-container {
                                    max-width: 1200px;
                                    margin: 0 auto;
                                    padding: 20px;
                                }
                                
                                /* ===== HERO SECTION ===== */
                                .hero-container {
                                    background: #fafafa;
                                    border: 1px solid #e0e0e0;
                                    border-radius: 8px;
                                    padding: 40px;
                                    margin-bottom: 30px;
                                }
                                
                                .header-section {
                                    text-align: center;
                                    margin-bottom: 30px;
                                    padding-bottom: 20px;
                                    border-bottom: 1px solid #e0e0e0;
                                }
                                
                                .main-title {
                                    color: #000000;
                                    font-size: 2.5rem;
                                    font-weight: 300;
                                    margin-bottom: 12px;
                                    letter-spacing: -0.5px;
                                }
                                
                                .subtitle {
                                    color: #424242;
                                    font-size: 1.125rem;
                                    font-weight: 400;
                                    line-height: 1.6;
                                    max-width: 600px;
                                    margin: 0 auto;
                                }
                                
                                /* ===== BOTONES ===== */
                                .start-button {
                                    display: inline-flex;
                                    align-items: center;
                                    justify-content: center;
                                    padding: 14px 32px;
                                    background: #212121;
                                    color: white;
                                    text-decoration: none;
                                    border-radius: 4px;
                                    font-size: 1rem;
                                    font-weight: 400;
                                    margin-top: 20px;
                                    transition: background 0.2s;
                                    border: none;
                                    cursor: pointer;
                                }
                                
                                .start-button:hover {
                                    background: #424242;
                                }
                                
                                /* ===== CARACTERÍSTICAS ===== */
                                .features-list {
                                    background: #f5f5f5;
                                    border-radius: 6px;
                                    padding: 20px;
                                    margin-top: 30px;
                                    border: 1px solid #e0e0e0;
                                }
                                
                                .features-title {
                                    font-size: 1.125rem;
                                    font-weight: 500;
                                    color: #000000;
                                    margin-bottom: 15px;
                                }
                                
                                .features-grid {
                                    display: grid;
                                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                                    gap: 12px;
                                }
                                
                                .feature-item {
                                    color: #424242;
                                    font-size: 0.9rem;
                                    display: flex;
                                    align-items: center;
                                    gap: 8px;
                                }
                                
                                .feature-item::before {
                                    content: '✓';
                                    color: #212121;
                                    font-weight: bold;
                                }
                                
                                /* ===== RESPONSIVE ===== */
                                @media (max-width: 768px) {
                                    .main-container {
                                        padding: 15px;
                                    }
                                    
                                    .hero-container, .form-container {
                                        padding: 25px 20px;
                                    }
                                    
                                    .main-title {
                                        font-size: 2rem;
                                    }
                                    
                                    .menu-container {
                                        flex-direction: column;
                                        height: auto;
                                        padding: 10px 0;
                                    }
                                    
                                    .menu-items {
                                        flex-direction: column;
                                        gap: 10px;
                                        width: 100%;
                                    }
                                    
                                    .menu-item {
                                        width: 100%;
                                        text-align: center;
                                    }
                                    
                                    .submenu {
                                        position: static;
                                        opacity: 1;
                                        visibility: visible;
                                        transform: none;
                                        box-shadow: none;
                                        margin-top: 10px;
                                        display: none;
                                    }
                                    
                                    .menu-item:hover .submenu {
                                        display: block;
                                    }
                                }
                                
                                /* ===== UTILIDADES ===== */
                                .checkbox-group {
                                    display: flex;
                                    align-items: center;
                                    gap: 8px;
                                }
                                
                                .checkbox-group input[type="checkbox"] {
                                    width: 18px;
                                    height: 18px;
                                }
                                
                                /* Mensaje de éxito */
                                .success-message {
                                    background: #d4edda;
                                    color: #155724;
                                    padding: 12px 16px;
                                    border-radius: 4px;
                                    margin-bottom: 20px;
                                    border: 1px solid #c3e6cb;
                                    animation: fadeIn 0.5s ease;
                                }
                                """
                            }
                        }
                    }
                    body {
                        // ===== BARRA DE MENÚ SUPERIOR =====
                        div("top-menu-bar") {
                            div("menu-container") {
                                div("menu-logo") {
                                    a(href = "/") { +"Kotlin Server" }
                                }
                                div("menu-items") {
                                    // Inicio
                                    div("menu-item") {
                                        a(href = "/") { +"Inicio" }
                                    }

                                    // Unidad 1 con submenú
                                    div("menu-item") {
                                        a(href = "#", classes = "has-submenu") { +"Unidad 1" }
                                        div("submenu") {
                                            a(href = "/neon-conexion") { +"Conexión a Neon" }
                                            a(href = "/breadcrumbs") { +"Breadcrumbs" }
                                        }
                                    }

                                    // Unidad 2 con submenú (ahora con 4 opciones)
                                    div("menu-item") {
                                        a(href = "#", classes = "has-submenu") { +"Unidad 2" }
                                        div("submenu") {
                                            a(href = "/unidad2/carrusel") { +"Carrusel de Imágenes" }
                                            a(href = "/unidad2/formulario") { +"Formulario Tradicional" }
                                            a(href = "/unidad2/crud-personas") { +"CRUD Personas" }
                                        }
                                    }
                                }
                            }
                        }

                        div("main-container") {
                            // ===== SECCIÓN DE BIENVENIDA =====
                            div("welcome-section") {
                                div("welcome-icon") { +"🚀" }
                                h2("welcome-title") { +"Hola bienvenidos al framework Kotlin Server" }
                                p("welcome-text") {
                                    +"Este es un ejemplo demostrativo de un servidor web construido con Ktor, mostrando diferentes funcionalidades como breadcrumbs, formularios con validación, CRUD completo y conexión a bases de datos."
                                }
                            }

                            // ===== HERO SECTION MINIMALISTA =====
                            div("hero-container") {
                                div("header-section") {
                                    h1("main-title") { +"Bienvenido al Sistema" }
                                    p("subtitle") { +"Explora las diferentes funcionalidades del servidor a través del menú superior" }
                                }

                                div("features-list") {
                                    h4("features-title") { +"Módulos Disponibles" }
                                    div("features-grid") {
                                        div("feature-item") { +"Unidad 1 - Conexión a Neon PostgreSQL" }
                                        div("feature-item") { +"Unidad 1 - Sistema de Breadcrumbs" }
                                        div("feature-item") { +"Unidad 2 - Carrusel de Imágenes" }
                                        div("feature-item") { +"Unidad 2 - Formulario Tradicional" }
                                        div("feature-item") { +"Unidad 2 - CRUD Personas" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== RUTA PARA PROCESAR EL FORMULARIO TRADICIONAL ==========
            post("/unidad2/submit-form") {
                println("Procesando formulario tradicional de Unidad 2...")

                // Obtener datos del formulario
                val params = call.receiveParameters()

                // Crear objeto FormData
                val formData = FormData(
                    nombre = params["nombre"] ?: "",
                    email = params["email"] ?: "",
                    telefono = params["telefono"] ?: "",
                    mensaje = params["mensaje"] ?: "",
                    recaptchaToken = params["g-recaptcha-response"] ?: "",
                    terminosAceptados = params["terminos"] != null
                )

                // Validar datos
                val isValid = FormValidator.validate(formData)

                if (isValid) {
                    println("✅ Formulario tradicional válido recibido:")
                    println("   Nombre: ${formData.nombre}")
                    println("   Email: ${formData.email}")
                    println("   Teléfono: ${formData.telefono}")
                    println("   Mensaje: ${formData.mensaje}")
                    println("   reCAPTCHA: Verificado")
                    println("   Términos: ${formData.terminosAceptados}")

                    // Redirigir de vuelta con mensaje de éxito
                    val mensajeExito = URLEncoder.encode("✅ Formulario tradicional enviado exitosamente!", "UTF-8")
                    call.respondRedirect("/unidad2/formulario?success=true&message=$mensajeExito")

                } else {
                    println("❌ Formulario tradicional inválido")

                    // Redirigir de vuelta con mensaje de error
                    val mensajeError = URLEncoder.encode("❌ Por favor, corrija los errores en el formulario", "UTF-8")
                    call.respondRedirect("/unidad2/formulario?success=false&message=$mensajeError")
                }
            }

            // ========== RUTAS PARA CRUD DE PERSONAS ==========

            // Obtener todas las personas con paginación
            get("/unidad2/api/personas") {
                println("Obteniendo lista de personas con paginación")

                try {
                    // Obtener parámetros de paginación
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 5
                    val offset = (page - 1) * pageSize

                    println("Página: $page, Tamaño: $pageSize, Offset: $offset")

                    // Obtener total de registros
                    val totalRegistros = transaction {
                        PersonasTable.selectAll().count()
                    }

                    // Obtener registros paginados
                    val personas = transaction {
                        PersonasTable.selectAll()
                            .orderBy(PersonasTable.id to SortOrder.ASC)
                            .limit(pageSize, offset.toLong())
                            .map {
                                PersonaData(
                                    id = it[PersonasTable.id],
                                    nombre = it[PersonasTable.nombre],
                                    apellido = it[PersonasTable.apellido],
                                    email = it[PersonasTable.email],
                                    telefono = it[PersonasTable.telefono],
                                    genero = it[PersonasTable.genero],
                                    estado = it[PersonasTable.estado]
                                )
                            }
                    }

                    // Calcular total de páginas
                    val totalPages = ((totalRegistros + pageSize - 1) / pageSize).toInt()

                    // Crear objeto de respuesta con paginación
                    val respuesta = PaginatedResponse(
                        data = personas,
                        pagination = PaginationInfo(
                            currentPage = page,
                            pageSize = pageSize,
                            totalRegistros = totalRegistros,
                            totalPages = totalPages
                        )
                    )

                    // Configuración JSON
                    val json = Json {
                        encodeDefaults = true
                        prettyPrint = false
                    }

                    val jsonString = json.encodeToString(respuesta)
                    println("JSON enviado con paginación")

                    call.respondText(
                        jsonString,
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    println("❌ Error obteniendo personas: ${e.message}")
                    e.printStackTrace() // Para ver más detalles del error
                    call.respondText(
                        Json.encodeToString(mapOf("error" to e.message)),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Crear nueva persona
            post("/unidad2/api/personas") {
                println("Creando nueva persona")

                try {
                    val content = call.receiveText()
                    val persona = Json.decodeFromString<PersonaData>(content)

                    // Validar datos
                    if (!FormValidator.validatePersona(persona)) {
                        call.respondText(
                            Json.encodeToString(mapOf("error" to "Datos inválidos")),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@post
                    }

                    // Insertar en la base de datos
                    val id = transaction {
                        PersonasTable.insert {
                            it[nombre] = persona.nombre
                            it[apellido] = persona.apellido
                            it[email] = persona.email
                            it[telefono] = persona.telefono
                            it[genero] = persona.genero
                            it[estado] = persona.estado
                        } get PersonasTable.id
                    }

                    val nuevaPersona = persona.copy(id = id)

                    call.respondText(
                        Json.encodeToString(nuevaPersona),
                        ContentType.Application.Json,
                        HttpStatusCode.Created
                    )
                } catch (e: Exception) {
                    println("❌ Error creando persona: ${e.message}")
                    call.respondText(
                        Json.encodeToString(mapOf("error" to e.message)),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Actualizar persona existente
            put("/unidad2/api/personas/{id}") {
                val idParam = call.parameters["id"]?.toIntOrNull()

                if (idParam == null) {
                    call.respondText(
                        Json.encodeToString(mapOf("error" to "ID inválido")),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@put
                }

                println("Actualizando persona con ID: $idParam")

                try {
                    val content = call.receiveText()
                    val persona = Json.decodeFromString<PersonaData>(content)

                    // Validar datos
                    if (!FormValidator.validatePersona(persona)) {
                        call.respondText(
                            Json.encodeToString(mapOf("error" to "Datos inválidos")),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@put
                    }

                    // Actualizar en la base de datos
                    val updated = transaction {
                        PersonasTable.update({ PersonasTable.id eq idParam }) {
                            it[nombre] = persona.nombre
                            it[apellido] = persona.apellido
                            it[email] = persona.email
                            it[telefono] = persona.telefono
                            it[genero] = persona.genero
                            it[estado] = persona.estado
                            it[fechaActualizacion] = CurrentDateTime
                        } > 0
                    }

                    if (updated) {
                        val personaActualizada = persona.copy(id = idParam)
                        call.respondText(
                            Json.encodeToString(personaActualizada),
                            ContentType.Application.Json
                        )
                    } else {
                        call.respondText(
                            Json.encodeToString(mapOf("error" to "Persona no encontrada")),
                            ContentType.Application.Json,
                            HttpStatusCode.NotFound
                        )
                    }
                } catch (e: Exception) {
                    println("❌ Error actualizando persona: ${e.message}")
                    call.respondText(
                        Json.encodeToString(mapOf("error" to e.message)),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Eliminar persona
            delete("/unidad2/api/personas/{id}") {
                val idParam = call.parameters["id"]?.toIntOrNull()

                if (idParam == null) {
                    call.respondText(
                        Json.encodeToString(mapOf("error" to "ID inválido")),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@delete
                }

                println("Eliminando persona con ID: $idParam")

                try {
                    val deleted = transaction {
                        PersonasTable.deleteWhere { PersonasTable.id eq idParam }
                    } > 0

                    if (deleted) {
                        call.respondText(
                            Json.encodeToString(mapOf("success" to true, "message" to "Persona eliminada")),
                            ContentType.Application.Json
                        )
                    } else {
                        call.respondText(
                            Json.encodeToString(mapOf("error" to "Persona no encontrada")),
                            ContentType.Application.Json,
                            HttpStatusCode.NotFound
                        )
                    }
                } catch (e: Exception) {
                    println("❌ Error eliminando persona: ${e.message}")
                    call.respondText(
                        Json.encodeToString(mapOf("error" to e.message)),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Página del CRUD de personas
            get("/unidad2/crud-personas") {
                println("Sirviendo CRUD de personas")

                call.respondHtml {
                    head {
                        title { +"CRUD Personas - Unidad 2" }
                        style { unsafe { +getMinimalistCSSConCRUD() } }
                        // Referencia al archivo JavaScript externo
                        script {
                            src = "/static/js/crud-personas.js"
                            type = "text/javascript"
                        }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Kotlin Server" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "/neon-conexion", classes = "nav-item") { +"Neon" }
                                        a(href = "/breadcrumbs", classes = "nav-item") { +"Breadcrumbs" }
                                        a(href = "/unidad2/carrusel", classes = "nav-item") { +"Carrusel" }
                                        a(href = "/unidad2/formulario", classes = "nav-item") { +"Formulario" }
                                        a(href = "#", classes = "nav-item active") { +"CRUD" }
                                    }
                                }
                            }

                            // Breadcrumbs
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "#", classes = "breadcrumb-link") { +"Unidad 2" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"CRUD Personas" }
                                    }
                                }
                            }

                            // Main Content
                            main("content-area") {
                                div("content-card") {
                                    div("card-header") {
                                        h2("card-title") { +"Administrar Personas" }
                                        div("card-subtitle") { +"CRUD completo con Fetch API y base de datos" }
                                    }

                                    div("card-body") {
                                        p("card-text") {
                                            +"Este módulo permite realizar operaciones CRUD (Crear, Leer, Actualizar, Eliminar) sobre un registro de personas, utilizando Fetch API para comunicación asíncrona con el servidor."
                                        }

                                        // ===== FORMULARIO CRUD =====
                                        div("crud-container") {
                                            // Barra de filtros
                                            div("crud-filters") {
                                                style = "margin-bottom: 20px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap;"

                                                label {
                                                    style = "font-weight: 500; color: #212121;"
                                                    +"Filtrar registros por:"
                                                }

                                                input(type = InputType.text) {
                                                    id = "filtroInput"
                                                    placeholder = "Nombre, Apellidos, Email, Teléfono o Género..."
                                                    style = "flex: 1; padding: 8px 12px; border: 1px solid #bdbdbd; border-radius: 4px; font-size: 0.95rem; min-width: 250px;"
                                                }

                                                button(type = ButtonType.button) {
                                                    id = "btnFiltrar"
                                                    style = "padding: 8px 16px; background: #212121; color: white; border: none; border-radius: 4px; cursor: pointer;"
                                                    +"Filtrar"
                                                }

                                                button(type = ButtonType.button) {
                                                    id = "btnLimpiarFiltro"
                                                    style = "padding: 8px 16px; background: #f5f5f5; color: #212121; border: 1px solid #bdbdbd; border-radius: 4px; cursor: pointer;"
                                                    +"Limpiar"
                                                }
                                            }

                                            // Tabla de personas
                                            div("crud-table-container") {
                                                style = "overflow-x: auto; margin-bottom: 20px; border: 1px solid #e0e0e0; border-radius: 4px;"

                                                table("crud-table") {
                                                    style = "width: 100%; border-collapse: collapse; min-width: 800px;"

                                                    thead {
                                                        style = "background: #f5f5f5;"
                                                        tr {
                                                            //th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121; width: 50px;"; +"ID" }
                                                            th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121;"; +"Nombre" }
                                                            th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121;"; +"Apellido" }
                                                            th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121;"; +"Email" }
                                                            th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121;"; +"Teléfono" }
                                                            th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121;"; +"Género" }
                                                            th { style = "padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121;"; +"Estado" }
                                                            th { style = "padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0; font-weight: 600; color: #212121; width: 150px;"; +"Acciones" }
                                                        }
                                                    }

                                                    tbody {
                                                        id = "tablaPersonasBody"
                                                        style = "background: white;"
                                                        tr {
                                                            td {
                                                                attributes["colspan"] = "7"
                                                                style = "padding: 40px; text-align: center; color: #9e9e9e;"
                                                                +"Cargando personas..."
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Controles de paginación
                                            div("pagination-controls") {
                                                id = "paginationControls"
                                                style = "display: flex; justify-content: space-between; align-items: center; margin: 20px 0; padding: 10px; background: #f5f5f5; border-radius: 4px;"

                                                div("pagination-info") {
                                                    id = "paginationInfo"
                                                    style = "color: #616161; font-size: 0.9rem;"
                                                    +"Cargando..."
                                                }

                                                div("pagination-buttons") {
                                                    style = "display: flex; gap: 10px;"

                                                    button(type = ButtonType.button) {
                                                        id = "btnPrevPage"
                                                        style = "padding: 8px 16px; background: #212121; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem;"
                                                        +"Anterior"
                                                    }

                                                    button(type = ButtonType.button) {
                                                        id = "btnNextPage"
                                                        style = "padding: 8px 16px; background: #212121; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem;"
                                                        +"Siguiente"
                                                    }
                                                }
                                            }

                                            // Botón para agregar nueva persona
                                            div("crud-actions") {
                                                style = "display: flex; justify-content: flex-end; margin-bottom: 20px;"

                                                button(type = ButtonType.button) {
                                                    id = "btnAgregar"
                                                    style = "padding: 12px 24px; background: #4caf50; color: white; border: none; border-radius: 4px; font-size: 1rem; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 8px;"
                                                    span { +"+" }
                                                    +"Agregar Persona"
                                                }
                                            }

                                            // Modal para formulario de persona
                                            div("crud-modal") {
                                                id = "personaModal"
                                                style = "display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 2000; align-items: center; justify-content: center;"

                                                div("crud-modal-content") {
                                                    style = "background: white; border-radius: 8px; max-width: 500px; width: 90%; max-height: 90vh; overflow-y: auto; position: relative;"

                                                    div("crud-modal-header") {
                                                        style = "padding: 20px; border-bottom: 1px solid #e0e0e0; display: flex; justify-content: space-between; align-items: center;"

                                                        h3("crud-modal-title") {
                                                            id = "modalTitle"
                                                            style = "font-size: 1.25rem; font-weight: 500; color: #212121; margin: 0;"
                                                            +"Agregar Persona"
                                                        }

                                                        button(type = ButtonType.button) {
                                                            id = "btnCerrarModal"
                                                            style = "background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #9e9e9e;"
                                                            +"×"
                                                        }
                                                    }

                                                    div("crud-modal-body") {
                                                        style = "padding: 20px;"

                                                        form {
                                                            id = "personaForm"

                                                            // ID oculto para edición
                                                            input(type = InputType.hidden) {
                                                                id = "personaId"
                                                                name = "personaId"
                                                                value = "0"
                                                            }

                                                            // Nombre
                                                            div("form-group") {
                                                                style = "margin-bottom: 15px;"
                                                                label("form-label required") {
                                                                    attributes["for"] = "personaNombre"
                                                                    +"Nombre"
                                                                }
                                                                input(type = InputType.text, classes = "form-input") {
                                                                    id = "personaNombre"
                                                                    name = "nombre"
                                                                    placeholder = "Ingrese el nombre"
                                                                    attributes["required"] = "true"
                                                                    attributes["maxlength"] = "50"
                                                                }
                                                                div("real-time-error") {
                                                                    id = "nombreError"
                                                                }
                                                            }

                                                            // Apellido
                                                            div("form-group") {
                                                                style = "margin-bottom: 15px;"
                                                                label("form-label required") {
                                                                    attributes["for"] = "personaApellido"
                                                                    +"Apellido"
                                                                }
                                                                input(type = InputType.text, classes = "form-input") {
                                                                    id = "personaApellido"
                                                                    name = "apellido"
                                                                    placeholder = "Ingrese el apellido"
                                                                    attributes["required"] = "true"
                                                                    attributes["maxlength"] = "50"
                                                                }
                                                                div("real-time-error") {
                                                                    id = "apellidoError"
                                                                }
                                                            }

                                                            // Email
                                                            div("form-group") {
                                                                style = "margin-bottom: 15px;"
                                                                label("form-label required") {
                                                                    attributes["for"] = "personaEmail"
                                                                    +"Email"
                                                                }
                                                                input(type = InputType.email, classes = "form-input") {
                                                                    id = "personaEmail"
                                                                    name = "email"
                                                                    placeholder = "ejemplo@correo.com"
                                                                    attributes["required"] = "true"
                                                                }
                                                                div("real-time-error") {
                                                                    id = "emailError"
                                                                }
                                                            }

                                                            // Teléfono
                                                            div("form-group") {
                                                                style = "margin-bottom: 15px;"
                                                                label("form-label required") {
                                                                    attributes["for"] = "personaTelefono"
                                                                    +"Teléfono"
                                                                }
                                                                input(type = InputType.tel, classes = "form-input") {
                                                                    id = "personaTelefono"
                                                                    name = "telefono"
                                                                    placeholder = "1234567890"
                                                                    attributes["required"] = "true"
                                                                    attributes["maxlength"] = "15"
                                                                }
                                                                div("real-time-error") {
                                                                    id = "telefonoError"
                                                                }
                                                            }

                                                            // Género
                                                            div("form-group") {
                                                                style = "margin-bottom: 15px;"
                                                                label("form-label required") {
                                                                    attributes["for"] = "personaGenero"
                                                                    +"Género"
                                                                }
                                                                select(classes = "form-input") {
                                                                    id = "personaGenero"
                                                                    name = "genero"
                                                                    attributes["required"] = "true"
                                                                    option {
                                                                        attributes["value"] = ""
                                                                        +"Seleccione género"
                                                                    }
                                                                    option {
                                                                        attributes["value"] = "Masculino"
                                                                        +"Masculino"
                                                                    }
                                                                    option {
                                                                        attributes["value"] = "Femenino"
                                                                        +"Femenino"
                                                                    }
                                                                    option {
                                                                        attributes["value"] = "Otro"
                                                                        +"Otro"
                                                                    }
                                                                }
                                                                div("real-time-error") {
                                                                    id = "generoError"
                                                                }
                                                            }

                                                            // Estado
                                                            div("form-group") {
                                                                style = "margin-bottom: 15px;"
                                                                label("form-label required") {
                                                                    attributes["for"] = "personaEstado"
                                                                    +"Estado"
                                                                }
                                                                select(classes = "form-input") {
                                                                    id = "personaEstado"
                                                                    name = "estado"
                                                                    attributes["required"] = "true"
                                                                    option {
                                                                        attributes["value"] = "Activo"
                                                                        selected = true
                                                                        +"Activo"
                                                                    }
                                                                    option {
                                                                        attributes["value"] = "Inactivo"
                                                                        +"Inactivo"
                                                                    }
                                                                }
                                                            }

                                                            // Botones del formulario
                                                            div("form-buttons") {
                                                                style = "display: flex; gap: 10px; margin-top: 20px;"

                                                                button(type = ButtonType.submit, classes = "submit-btn") {
                                                                    id = "btnGuardar"
                                                                    style = "flex: 2; padding: 12px; background: #212121; color: white; border: none; border-radius: 4px; font-size: 1rem; cursor: pointer;"
                                                                    +"Guardar"
                                                                }

                                                                button(type = ButtonType.button, classes = "reset-btn") {
                                                                    id = "btnCancelar"
                                                                    style = "flex: 1; padding: 12px; background: #f5f5f5; color: #212121; border: 1px solid #bdbdbd; border-radius: 4px; font-size: 1rem; cursor: pointer;"
                                                                    +"Cancelar"
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Contenedor para mensajes
                                            div("crud-message") {
                                                id = "crudMessage"
                                                style = "display: none; padding: 12px 16px; border-radius: 4px; margin-bottom: 20px;"
                                            }
                                        }
                                    }

                                    div("card-footer") {
                                        div("action-buttons") {
                                            a(href = "/", classes = "action-button secondary") {
                                                +"Volver al Inicio"
                                            }
                                            a(href = "/unidad2/carrusel", classes = "action-button primary") {
                                                +"Ir al Carrusel"
                                            }
                                            a(href = "/unidad2/formulario", classes = "action-button primary") {
                                                +"Formulario Tradicional"
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer
                            footer("main-footer") {
                                div("footer-content") {
                                    p("footer-text") { +"© 2024 Kotlin Server - Unidad 2: CRUD Personas" }
                                    div("footer-links") {
                                        a(href = "#") { +"Documentación" }
                                        a(href = "#") { +"Acerca de" }
                                        a(href = "#") { +"Contacto" }
                                    }
                                }
                            }
                        }

                        // ELIMINADO: Todo el script inline que estaba aquí
                    }
                }
            }

            // ========== RUTA PARA CONEXIÓN A NEON (Unidad 1) ==========
            get("/neon-conexion") {
                println("Sirviendo página de conexión a Neon")

                // Manejar envío del formulario si existe
                val palabraGuardada = call.parameters["palabra"]
                var mensajeExito = ""

                if (palabraGuardada != null && palabraGuardada.isNotBlank()) {
                    try {
                        transaction {
                            PalabrasGuardadasTable.insert {
                                it[PalabrasGuardadasTable.palabra] = palabraGuardada.trim()
                            }
                        }
                        mensajeExito = "✅ Palabra '${palabraGuardada.trim()}' guardada exitosamente en Neon PostgreSQL"
                        println(mensajeExito)
                    } catch (e: Exception) {
                        println("❌ Error guardando palabra en Neon: ${e.message}")
                        mensajeExito = "❌ Error al guardar la palabra: ${e.message}"
                    }
                }

                call.respondHtml {
                    head {
                        title { +"Conexión a Neon - Kotlin Server" }
                        style { unsafe { +getMinimalistCSS() } }
                        script {
                            src = "https://www.google.com/recaptcha/api.js"
                            async = true
                            defer = true
                        }
                        script {
                            src = "/static/js/neon.js"
                            type = "text/javascript"
                        }
                    }
                    body {
                        div("page-wrapper") {
                            // Header con menú
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Kotlin Server" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "#", classes = "nav-item active") { +"Neon" }
                                        a(href = "/breadcrumbs", classes = "nav-item") { +"Breadcrumbs" }
                                        a(href = "/unidad2/carrusel", classes = "nav-item") { +"Carrusel" }
                                        a(href = "/unidad2/formulario", classes = "nav-item") { +"Formulario" }
                                        a(href = "/unidad2/crud-personas", classes = "nav-item") { +"CRUD" }
                                    }
                                }
                            }

                            // Breadcrumbs jerárquicos
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "#", classes = "breadcrumb-link") { +"Unidad 1" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"Conexión a Neon" }
                                    }
                                }
                            }

                            // Main Content
                            main("content-area") {
                                div("content-card") {
                                    div("card-header") {
                                        h2("card-title") { +"Conexión a Neon PostgreSQL" }
                                        div("card-subtitle") { +"Base de datos serverless en la nube" }
                                    }

                                    div("card-body") {
                                        p("card-text") {
                                            +"Neon es una base de datos PostgreSQL serverless diseñada para la nube. Ofrece escalado automático, branching y una experiencia de desarrollo optimizada."
                                        }

                                        // FORMULARIO PARA GUARDAR EN BD
                                        div("bd-test-container") {
                                            style = "background: #f8f9fa; border: 2px solid #e9ecef; border-radius: 8px; padding: 20px; margin: 20px 0;"

                                            h3 {
                                                style = "color: #212529; margin-bottom: 15px; font-weight: 500;"
                                                +"🔍 Prueba de Base de Datos Neon"
                                            }

                                            p {
                                                style = "color: #6c757d; margin-bottom: 15px; font-size: 0.95rem;"
                                                +"Escribe una palabra para guardarla en PostgreSQL (Neon). Verifica en el dashboard de Neon que se guardó correctamente."
                                            }

                                            // Mostrar mensaje de éxito/error
                                            if (mensajeExito.isNotBlank()) {
                                                div {
                                                    val bgColor = if (mensajeExito.startsWith("✅")) "#d1e7dd" else "#f8d7da"
                                                    val textColor = if (mensajeExito.startsWith("✅")) "#0f5132" else "#842029"
                                                    val borderColor = if (mensajeExito.startsWith("✅")) "#badbcc" else "#f5c2c7"

                                                    style = "background: $bgColor; color: $textColor; padding: 10px 15px; border-radius: 4px; margin-bottom: 15px; border: 1px solid $borderColor;"
                                                    +mensajeExito
                                                }
                                            }

                                            // Formulario para guardar en BD
                                            form {
                                                method = FormMethod.get
                                                action = "/neon-conexion"

                                                div("bd-form-group") {
                                                    style = "display: flex; gap: 10px; margin-bottom: 10px;"

                                                    input(type = InputType.text) {
                                                        name = "palabra"
                                                        placeholder = "Escribe una palabra (ej: hola, prueba, test)"
                                                        style = "flex: 1; padding: 10px 12px; border: 1px solid #ced4da; border-radius: 4px; font-size: 0.95rem;"
                                                        attributes["required"] = "true"
                                                        attributes["maxlength"] = "50"
                                                    }

                                                    button(type = ButtonType.submit) {
                                                        style = "background: #212529; color: white; border: none; border-radius: 4px; padding: 10px 20px; font-size: 0.95rem; cursor: pointer; transition: background 0.2s;"
                                                        +"Guardar en BD"
                                                    }
                                                }

                                                div("bd-form-hint") {
                                                    style = "font-size: 0.8rem; color: #6c757d; display: flex; align-items: center; gap: 5px;"
                                                    +"📍 Conectado a: "
                                                    code {
                                                        style = "background: #e9ecef; padding: 2px 6px; border-radius: 3px; font-size: 0.75rem;"
                                                        +"Neon PostgreSQL"
                                                    }
                                                }
                                            }
                                        }

                                        div("info-grid") {
                                            div("info-block") {
                                                h4("info-title") { +"Estado de la conexión" }
                                                div("connection-status") {
                                                    style = "margin-top: 10px;"
                                                    div {
                                                        style = "display: flex; align-items: center; gap: 10px; background: #e8f5e8; color: #2e7d32; padding: 10px; border-radius: 4px;"
                                                        span { +"✅" }
                                                        span { +"Conectado exitosamente a Neon PostgreSQL" }
                                                    }
                                                }
                                            }

                                            div("info-block") {
                                                h4("info-title") { +"Detalles de la conexión" }
                                                ul("type-features") {
                                                    li { +"Host: ep-green-thunder-aifcls8i-pooler.c-4.us-east-1.aws.neon.tech" }
                                                    li { +"Base de datos: neondb" }
                                                    li { +"Usuario: neondb_owner" }
                                                    li { +"SSL Mode: require" }
                                                }
                                            }

                                            div("info-block") {
                                                h4("info-title") { +"Tablas disponibles" }
                                                ul("type-features") {
                                                    li { +"palabras_guardadas" }
                                                    li { +"personas (nueva tabla CRUD)" }
                                                }
                                            }
                                        }

                                        div("code-example") {
                                            h3("code-title") { +"Ejemplo de configuración" }
                                            div("code-block") {
                                                pre {
                                                    code {
                                                        +"""// Configuración de la base de datos
fun Application.configureDatabase() {
    val host = "ep-green-thunder-aifcls8i-pooler.c-4.us-east-1.aws.neon.tech"
    val dbName = "neondb"
    val user = "neondb_owner"
    val password = "npg_hJaWEqNS6AV5"
    val jdbcUrl = "jdbc:postgresql://${'$'}host/${'$'}dbName?sslmode=require"

    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )
}"""
                                                    }
                                                }
                                            }
                                        }

                                        div("highlight-section") {
                                            h3("highlight-title") { +"Características de Neon" }
                                            div("highlight-content") {
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Escalado automático: se ajusta a la demanda sin intervención manual"
                                                    }
                                                }
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Branching: crea ramas de tu base de datos para desarrollo y pruebas"
                                                    }
                                                }
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Serverless: paga solo por lo que usas, sin servidores que administrar"
                                                    }
                                                }
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Totalmente compatible con PostgreSQL"
                                                    }
                                                }
                                            }
                                        }

                                        // Enlace al dashboard de Neon
                                        div("bd-dashboard-link") {
                                            style = "margin-top: 20px; padding-top: 20px; border-top: 1px solid #dee2e6;"

                                            a(href = "https://console.neon.tech/app/projects", target = "_blank") {
                                                style = "display: inline-flex; align-items: center; gap: 5px; color: #0d6efd; text-decoration: none; font-size: 0.9rem;"
                                                +"🔗 Abrir dashboard de Neon"
                                            }

                                            span {
                                                style = "color: #6c757d; font-size: 0.85rem; margin-left: 10px;"
                                                +"para ver los datos guardados"
                                            }
                                        }
                                    }

                                    div("card-footer") {
                                        div("action-buttons") {
                                            a(href = "/", classes = "action-button secondary") {
                                                +"Volver al Inicio"
                                            }
                                            a(href = "/breadcrumbs", classes = "action-button primary") {
                                                +"Ir a Breadcrumbs"
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer
                            footer("main-footer") {
                                div("footer-content") {
                                    p("footer-text") { +"© 2024 Kotlin Server - Ejemplo con Neon PostgreSQL" }
                                    div("footer-links") {
                                        a(href = "https://neon.tech") { +"Neon" }
                                        a(href = "https://ktor.io") { +"Ktor" }
                                        a(href = "https://kotlinlang.org") { +"Kotlin" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== RUTA PARA CARRUSEL (Unidad 2) ==========
            get("/unidad2/carrusel") {
                println("Sirviendo Carrusel de Unidad 2")

                call.respondHtml {
                    head {
                        title { +"Carrusel de Imágenes - Unidad 2" }
                        style { unsafe { +getMinimalistCSSConCarrusel() } }
                        script {
                            src = "/static/js/carrusel.js"
                            type = "text/javascript"
                        }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Kotlin Server" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "/neon-conexion", classes = "nav-item") { +"Neon" }
                                        a(href = "/breadcrumbs", classes = "nav-item") { +"Breadcrumbs" }
                                        a(href = "#", classes = "nav-item active") { +"Carrusel" }
                                        a(href = "/unidad2/formulario", classes = "nav-item") { +"Formulario" }
                                        a(href = "/unidad2/crud-personas", classes = "nav-item") { +"CRUD" }
                                    }
                                }
                            }

                            // Breadcrumbs
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "#", classes = "breadcrumb-link") { +"Unidad 2" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"Carrusel de Imágenes" }
                                    }
                                }
                            }

                            // Main Content
                            main("content-area") {
                                div("content-card") {
                                    div("card-header") {
                                        h2("card-title") { +"Carrusel de Imágenes" }
                                        div("card-subtitle") { +"Galería automática con CSS puro" }
                                    }

                                    div("card-body") {
                                        p("card-text") {
                                            +"Este carrusel utiliza animaciones CSS automáticas para mostrar diferentes ejemplos de implementación de breadcrumbs en sitios web modernos."
                                        }

                                        // ===== CARRUSEL DE IMÁGENES CON ANIMACIÓN AUTOMÁTICA CSS =====
                                        div("carousel-section") {
                                            div("carousel-header") {
                                                h3("carousel-title") { +"Galería de Ejemplos" }
                                                p("carousel-subtitle") { +"Explora diferentes implementaciones de breadcrumbs" }
                                            }

                                            div("carousel-container") {
                                                div("carousel-track") {
                                                    // Slide 1
                                                    div("carousel-slide") {
                                                        img(
                                                            src = "https://images.unsplash.com/photo-1551650975-87deedd944c3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                            classes = "carousel-image"
                                                        ) {
                                                            attributes["alt"] = "Ejemplo de breadcrumbs en e-commerce"
                                                        }
                                                        div("carousel-caption") {
                                                            h3 { +"E-commerce" }
                                                            p { +"Breadcrumbs en tiendas online mostrando categorías de productos" }
                                                        }
                                                    }

                                                    // Slide 2
                                                    div("carousel-slide") {
                                                        img(
                                                            src = "https://images.unsplash.com/photo-1555099962-4199c345e5dd?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                            classes = "carousel-image"
                                                        ) {
                                                            attributes["alt"] = "Breadcrumbs en documentación técnica"
                                                        }
                                                        div("carousel-caption") {
                                                            h3 { +"Documentación" }
                                                            p { +"Navegación jerárquica en manuales y documentación técnica" }
                                                        }
                                                    }

                                                    // Slide 3
                                                    div("carousel-slide") {
                                                        img(
                                                            src = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                            classes = "carousel-image"
                                                        ) {
                                                            attributes["alt"] = "Breadcrumbs en blogs"
                                                        }
                                                        div("carousel-caption") {
                                                            h3 { +"Blogs y Noticias" }
                                                            p { +"Estructura de categorías y etiquetas en publicaciones" }
                                                        }
                                                    }

                                                    // Slide 4
                                                    div("carousel-slide") {
                                                        img(
                                                            src = "https://images.unsplash.com/photo-1555949963-aa79dcee981c?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                            classes = "carousel-image"
                                                        ) {
                                                            attributes["alt"] = "Breadcrumbs en dashboards"
                                                        }
                                                        div("carousel-caption") {
                                                            h3 { +"Dashboards" }
                                                            p { +"Navegación en paneles de control y aplicaciones empresariales" }
                                                        }
                                                    }
                                                }

                                                // Controles
                                                div("carousel-controls") {
                                                    button(type = ButtonType.button, classes = "carousel-btn prev-btn") {
                                                        +"‹"
                                                    }
                                                    button(type = ButtonType.button, classes = "carousel-btn next-btn") {
                                                        +"›"
                                                    }
                                                }

                                                // Barra de progreso automática
                                                div("carousel-progress") {
                                                    div("progress-bar") {}
                                                }
                                            }

                                            // Indicadores con animación automática
                                            div("carousel-indicators") {
                                                button(type = ButtonType.button, classes = "carousel-dot") {}
                                                button(type = ButtonType.button, classes = "carousel-dot") {}
                                                button(type = ButtonType.button, classes = "carousel-dot") {}
                                                button(type = ButtonType.button, classes = "carousel-dot") {}
                                            }
                                        }
                                    }

                                    div("card-footer") {
                                        div("action-buttons") {
                                            a(href = "/", classes = "action-button secondary") {
                                                +"Volver al Inicio"
                                            }
                                            a(href = "/unidad2/formulario", classes = "action-button primary") {
                                                +"Ir al Formulario"
                                            }
                                            a(href = "/unidad2/crud-personas", classes = "action-button primary") {
                                                +"Ir al CRUD"
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer
                            footer("main-footer") {
                                div("footer-content") {
                                    p("footer-text") { +"© 2024 Kotlin Server - Unidad 2: Carrusel de Imágenes" }
                                    div("footer-links") {
                                        a(href = "#") { +"Documentación" }
                                        a(href = "#") { +"Acerca de" }
                                        a(href = "#") { +"Contacto" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== RUTA PARA FORMULARIO TRADICIONAL (Unidad 2) ==========
            get("/unidad2/formulario") {
                println("Sirviendo Formulario Tradicional de Unidad 2")

                // Verificar si hay mensaje de éxito del formulario
                val successMessage = call.parameters["success"]
                val formMessage = call.parameters["message"]
                var formSuccessMessage = ""

                if (successMessage == "true" && formMessage != null) {
                    formSuccessMessage = formMessage
                }

                call.respondHtml {
                    head {
                        title { +"Formulario Tradicional - Unidad 2" }
                        style { unsafe { +getMinimalistCSSConFormulario() } }
                        script {
                            src = "https://www.google.com/recaptcha/api.js"
                            async = true
                            defer = true
                        }
                        script {
                            src = "/static/js/formulario.js"
                            type = "text/javascript"
                        }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Kotlin Server" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "/neon-conexion", classes = "nav-item") { +"Neon" }
                                        a(href = "/breadcrumbs", classes = "nav-item") { +"Breadcrumbs" }
                                        a(href = "/unidad2/carrusel", classes = "nav-item") { +"Carrusel" }
                                        a(href = "#", classes = "nav-item active") { +"Formulario" }
                                        a(href = "/unidad2/crud-personas", classes = "nav-item") { +"CRUD" }
                                    }
                                }
                            }

                            // Breadcrumbs
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "#", classes = "breadcrumb-link") { +"Unidad 2" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"Formulario Tradicional" }
                                    }
                                }
                            }

                            // Main Content
                            main("content-area") {
                                div("content-card") {
                                    div("card-header") {
                                        h2("card-title") { +"Formulario de Contacto Tradicional" }
                                        div("card-subtitle") { +"Envío tradicional con recarga de página y reCAPTCHA" }
                                    }

                                    div("card-body") {
                                        p("card-text") {
                                            +"Este formulario utiliza envío tradicional con recarga de página, incluye validaciones en tiempo real con HTML5/CSS y reCAPTCHA para verificación de seguridad."
                                        }

                                        // Mostrar mensaje de éxito del formulario
                                        if (formSuccessMessage.isNotBlank()) {
                                            div("success-message") {
                                                style = "background: #d4edda; color: #155724; padding: 12px 16px; border-radius: 4px; margin-bottom: 20px; border: 1px solid #c3e6cb; animation: fadeIn 0.5s ease;"
                                                +formSuccessMessage
                                            }
                                        }

                                        // ===== FORMULARIO TRADICIONAL =====
                                        div("form-container") {
                                            div("form-wrapper") {
                                                form {
                                                    method = FormMethod.post
                                                    action = "/unidad2/submit-form"
                                                    attributes["novalidate"] = "true"

                                                    // Campo 1: Nombre completo
                                                    div("form-group") {
                                                        label(classes = "form-label required") {
                                                            attributes["for"] = "nombre"
                                                            +"Nombre completo"
                                                        }
                                                        input(type = InputType.text, classes = "form-input") {
                                                            attributes["id"] = "nombre"
                                                            attributes["name"] = "nombre"
                                                            attributes["placeholder"] = "Ingrese su nombre completo (solo letras)"
                                                            attributes["pattern"] = "^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]+$"
                                                            attributes["maxlength"] = "40"
                                                            attributes["required"] = "true"
                                                            attributes["title"] = "Solo letras y espacios, máximo 40 caracteres"
                                                        }
                                                        div("field-strength") {}
                                                        div("real-time-error") {
                                                            +"Solo letras y espacios, máximo 40 caracteres"
                                                        }
                                                        div("form-hint") {
                                                            +"Máximo 40 caracteres, solo letras y espacios"
                                                        }
                                                    }

                                                    // Campo 2: Email
                                                    div("form-group") {
                                                        label(classes = "form-label required") {
                                                            attributes["for"] = "email"
                                                            +"Correo electrónico"
                                                        }
                                                        input(type = InputType.email, classes = "form-input") {
                                                            attributes["id"] = "email"
                                                            attributes["name"] = "email"
                                                            attributes["placeholder"] = "ejemplo@correo.com"
                                                            attributes["required"] = "true"
                                                            attributes["pattern"] = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
                                                            attributes["title"] = "Formato: usuario@dominio.com"
                                                        }
                                                        div("field-strength") {}
                                                        div("real-time-error") {
                                                            +"Formato inválido. Ejemplo: usuario@dominio.com"
                                                        }
                                                        div("form-hint") {
                                                            +"Ejemplo: usuario@dominio.com"
                                                        }
                                                    }

                                                    // Campo 3: Teléfono
                                                    div("form-group") {
                                                        label(classes = "form-label required") {
                                                            attributes["for"] = "telefono"
                                                            +"Teléfono"
                                                        }
                                                        input(type = InputType.tel, classes = "form-input") {
                                                            attributes["id"] = "telefono"
                                                            attributes["name"] = "telefono"
                                                            attributes["placeholder"] = "1234567890"
                                                            attributes["pattern"] = "^\\d{10}$"
                                                            attributes["maxlength"] = "10"
                                                            attributes["required"] = "true"
                                                            attributes["title"] = "10 dígitos numéricos"
                                                        }
                                                        div("field-strength") {}
                                                        div("real-time-error") {
                                                            +"10 dígitos numéricos sin espacios"
                                                        }
                                                        div("form-hint") {
                                                            +"Formato: 10 dígitos sin espacios ni guiones"
                                                        }
                                                    }

                                                    // Campo 4: Mensaje
                                                    div("form-group") {
                                                        label(classes = "form-label") {
                                                            attributes["for"] = "mensaje"
                                                            +"Mensaje adicional"
                                                        }
                                                        textArea(classes = "textarea-field") {
                                                            attributes["id"] = "mensaje"
                                                            attributes["name"] = "mensaje"
                                                            attributes["placeholder"] = "Ingrese su mensaje (máximo 20 palabras)"
                                                            attributes["rows"] = "4"
                                                            attributes["pattern"] = "^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]*$"
                                                            attributes["title"] = "Solo letras y espacios están permitidos"
                                                            attributes["data-max-words"] = "20"
                                                        }
                                                        div("word-counter") {
                                                            +"Palabras: 0/20"
                                                        }
                                                        div("real-time-error") {
                                                            +"Solo letras y espacios están permitidos, máximo 20 palabras"
                                                        }
                                                        div("form-hint") {
                                                            +"Máximo 20 palabras, solo letras y espacios"
                                                        }
                                                    }

                                                    // Campo 5: reCAPTCHA v2
                                                    div("form-group") {
                                                        div("form-label required") {
                                                            +"Verificación de seguridad"
                                                        }
                                                        div("recaptcha-container") {
                                                            div("recaptcha-badge") {
                                                                +"Este sitio está protegido por reCAPTCHA y se aplican la "
                                                                a(href = "https://policies.google.com/privacy", target = "_blank") {
                                                                    +"Política de privacidad"
                                                                }
                                                                +" y los "
                                                                a(href = "https://policies.google.com/terms", target = "_blank") {
                                                                    +"Términos de servicio"
                                                                }
                                                                +" de Google."
                                                            }

                                                            div("g-recaptcha") {
                                                                attributes["data-sitekey"] = "6LfVDVcsAAAAAErTmnJNGjMvB19ND5u5wN9NKGde"
                                                                attributes["data-callback"] = "onRecaptchaSuccess"
                                                                attributes["data-expired-callback"] = "onRecaptchaExpired"
                                                            }

                                                            div("error-message") {
                                                                attributes["id"] = "recaptchaError"
                                                                style = "display: none;"
                                                                +"Por favor, complete la verificación de seguridad"
                                                            }
                                                            div("form-hint") {
                                                                +"Marque la casilla 'No soy un robot' para verificar que es humano"
                                                            }
                                                        }
                                                    }

                                                    // Campo 6: Términos y condiciones
                                                    div("form-group") {
                                                        div("checkbox-group") {
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "terminos"
                                                                attributes["name"] = "terminos"
                                                                attributes["required"] = "true"
                                                                attributes["title"] = "Debe aceptar los términos y condiciones"
                                                            }
                                                            label {
                                                                attributes["for"] = "terminos"
                                                                span("form-label required") {
                                                                    +"Acepto los términos y condiciones"
                                                                }
                                                            }
                                                        }
                                                        div("real-time-error") {
                                                            +"Debe aceptar los términos y condiciones"
                                                        }
                                                    }

                                                    // Botones del formulario
                                                    div("form-buttons") {
                                                        button(type = ButtonType.submit, classes = "submit-btn") {
                                                            +"Enviar Formulario"
                                                        }
                                                        button(type = ButtonType.reset, classes = "reset-btn") {
                                                            +"Limpiar Formulario"
                                                        }
                                                    }
                                                }
                                            }

                                            // Pie del formulario
                                            div("form-footer") {
                                                p { +"Todos los campos marcados con * son obligatorios" }
                                                p { +"Los datos proporcionados son solo para fines demostrativos" }
                                            }
                                        }
                                    }

                                    div("card-footer") {
                                        div("action-buttons") {
                                            a(href = "/", classes = "action-button secondary") {
                                                +"Volver al Inicio"
                                            }
                                            a(href = "/unidad2/carrusel", classes = "action-button primary") {
                                                +"Ir al Carrusel"
                                            }
                                            a(href = "/unidad2/crud-personas", classes = "action-button primary") {
                                                +"Ir al CRUD"
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer
                            footer("main-footer") {
                                div("footer-content") {
                                    p("footer-text") { +"© 2024 Kotlin Server - Unidad 2: Formulario Tradicional" }
                                    div("footer-links") {
                                        a(href = "#") { +"Documentación" }
                                        a(href = "#") { +"Acerca de" }
                                        a(href = "#") { +"Contacto" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== NIVEL 1 DE BREADCRUMBS (DENTRO DE UNIDAD 1) ==========
            get("/breadcrumbs") {
                println("Sirviendo Nivel 1 de Breadcrumbs")
                call.respondHtml {
                    head {
                        title { +"Nivel 1 - Introducción a Breadcrumbs" }
                        style { unsafe { +getMinimalistCSS() } }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Breadcrumbs" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "/neon-conexion", classes = "nav-item") { +"Neon" }
                                        a(href = "#", classes = "nav-item active") { +"Breadcrumbs" }
                                        a(href = "/unidad2/carrusel", classes = "nav-item") { +"Carrusel" }
                                        a(href = "/unidad2/formulario", classes = "nav-item") { +"Formulario" }
                                        a(href = "/unidad2/crud-personas", classes = "nav-item") { +"CRUD" }
                                    }
                                }
                            }

                            // Breadcrumbs jerárquicos
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "#", classes = "breadcrumb-link") { +"Unidad 1" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"Nivel 1" }
                                    }
                                }
                            }

                            // Navegación interna de niveles
                            div("levels-container") {
                                style = "display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; margin: 30px 0;"

                                a(classes = "level-card", href = "/breadcrumbs") {
                                    style = "background: #ffffff; border: 2px solid #212121; border-radius: 6px; padding: 25px; text-align: center; text-decoration: none; color: #212121; transition: all 0.2s ease;"
                                    div("level-number") {
                                        style = "display: inline-block; background: #212121; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.125rem; margin: 0 auto 15px;"
                                        +"1"
                                    }
                                    h3("level-title") { +"Nivel 1 - Introducción" }
                                    p("level-description") { +"Conceptos básicos y fundamentos de los sistemas de breadcrumbs" }
                                }

                                a(classes = "level-card", href = "/breadcrumbs/detalle") {
                                    style = "background: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; text-align: center; text-decoration: none; color: #212121; transition: all 0.2s ease;"
                                    div("level-number") {
                                        style = "display: inline-block; background: #212121; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.125rem; margin: 0 auto 15px;"
                                        +"2"
                                    }
                                    h3("level-title") { +"Nivel 2 - Detalles" }
                                    p("level-description") { +"Análisis técnico y casos de uso de navegación jerárquica" }
                                }

                                a(classes = "level-card", href = "/breadcrumbs/detalle/configuracion") {
                                    style = "background: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; text-align: center; text-decoration: none; color: #212121; transition: all 0.2s ease;"
                                    div("level-number") {
                                        style = "display: inline-block; background: #212121; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.125rem; margin: 0 auto 15px;"
                                        +"3"
                                    }
                                    h3("level-title") { +"Nivel 3 - Configuración" }
                                    p("level-description") { +"Personalización y configuración avanzada del sistema" }
                                }
                            }

                            // Main Content
                            main("content-area") {
                                div("content-card") {
                                    div("card-header") {
                                        h2("card-title") { +"Introducción a los Breadcrumbs" }
                                        div("card-subtitle") { +"Conceptos fundamentales" }
                                    }

                                    div("card-body") {
                                        p("card-text") {
                                            +"Los breadcrumbs, también conocidos como migas de pan, son un sistema de navegación secundario que muestra la ruta que un usuario ha seguido para llegar a una página específica dentro de la jerarquía de un sitio web."
                                        }

                                        div("highlight-section") {
                                            h3("highlight-title") { +"Beneficios Principales" }
                                            div("highlight-content") {
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Mejora la experiencia de usuario al proporcionar contexto de navegación"
                                                    }
                                                }
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Reduce la tasa de rebote al facilitar la exploración de contenido relacionado"
                                                    }
                                                }
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Mejora el posicionamiento SEO al estructurar jerárquicamente el contenido"
                                                    }
                                                }
                                                div("benefit-item") {
                                                    div("benefit-icon") { +"•" }
                                                    div("benefit-text") {
                                                        +"Aumenta la accesibilidad para usuarios con necesidades especiales"
                                                    }
                                                }
                                            }
                                        }

                                        div("info-grid") {
                                            div("info-block") {
                                                h4("info-title") { +"Propósito" }
                                                p("info-text") {
                                                    +"Mostrar la ubicación actual dentro de la estructura jerárquica del sitio, permitiendo a los usuarios comprender su posición y navegar fácilmente hacia niveles superiores."
                                                }
                                            }
                                            div("info-block") {
                                                h4("info-title") { +"Implementación" }
                                                p("info-text") {
                                                    +"Este ejemplo utiliza breadcrumbs estáticos implementados manualmente en cada página, ideal para sitios con estructura fija y predecible."
                                                }
                                            }
                                            div("info-block") {
                                                h4("info-title") { +"Aplicación" }
                                                p("info-text") {
                                                    +"Comúnmente utilizados en sitios de comercio electrónico, documentación técnica, portales educativos y cualquier sitio con estructura jerárquica compleja."
                                                }
                                            }
                                        }
                                    }

                                    div("card-footer") {
                                        div("action-buttons") {
                                            a(href = "/", classes = "action-button secondary") {
                                                +"Volver al Inicio"
                                            }
                                            a(href = "/breadcrumbs/detalle", classes = "action-button primary") {
                                                +"Continuar al Nivel 2"
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer
                            footer("main-footer") {
                                div("footer-content") {
                                    p("footer-text") { +"© 2024 Sistema de Breadcrumbs - Unidad 1" }
                                    div("footer-links") {
                                        a(href = "#") { +"Documentación" }
                                        a(href = "#") { +"Acerca de" }
                                        a(href = "#") { +"Contacto" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== NIVEL 2 DE BREADCRUMBS ==========
            get("/breadcrumbs/detalle") {
                println("Sirviendo Nivel 2 de Breadcrumbs")
                call.respondHtml {
                    head {
                        title { +"Nivel 2 - Detalles de Breadcrumbs" }
                        style { unsafe { +getMinimalistCSS() } }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Breadcrumbs" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "/neon-conexion", classes = "nav-item") { +"Neon" }
                                        a(href = "/breadcrumbs", classes = "nav-item") { +"Nivel 1" }
                                        a(href = "#", classes = "nav-item active") { +"Nivel 2" }
                                        a(href = "/unidad2/carrusel", classes = "nav-item") { +"Carrusel" }
                                        a(href = "/unidad2/formulario", classes = "nav-item") { +"Formulario" }
                                        a(href = "/unidad2/crud-personas", classes = "nav-item") { +"CRUD" }
                                    }
                                }
                            }

                            // Breadcrumbs jerárquicos
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "#", classes = "breadcrumb-link") { +"Unidad 1" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item") {
                                        a(href = "/breadcrumbs", classes = "breadcrumb-link") { +"Nivel 1" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"Nivel 2" }
                                    }
                                }
                            }

                            // Main Content
                            main("content-area") {
                                div("content-card") {
                                    div("card-header") {
                                        h2("card-title") { +"Detalles Técnicos" }
                                        div("card-subtitle") { +"Análisis y casos de uso" }
                                    }

                                    div("card-body") {
                                        p("card-text") {
                                            +"En este nivel exploramos los aspectos técnicos y las diferentes metodologías de implementación de sistemas de breadcrumbs en aplicaciones web modernas."
                                        }

                                        div("implementation-types") {
                                            style = "display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; margin: 24px 0;"

                                            div("type-card") {
                                                style = "background: white; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px;"
                                                h3("type-title") { +"Breadcrumbs Estáticos" }
                                                ul("type-features") {
                                                    li { +"Definidos manualmente en cada página" }
                                                    li { +"Ideales para sitios con estructura fija" }
                                                    li { +"Fáciles de implementar y mantener" }
                                                    li { +"Rendimiento óptimo sin lógica compleja" }
                                                }
                                                div("type-tag static") {
                                                    style = "display: inline-block; padding: 4px 10px; border-radius: 4px; font-size: 0.7rem; background: #f5f5f5; color: #424242;"
                                                    +"Recomendado para sitios pequeños"
                                                }
                                            }

                                            div("type-card") {
                                                style = "background: white; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px;"
                                                h3("type-title") { +"Breadcrumbs Dinámicos" }
                                                ul("type-features") {
                                                    li { +"Generados automáticamente según la estructura" }
                                                    li { +"Escalables para sitios de gran tamaño" }
                                                    li { +"Se adaptan a cambios en la jerarquía" }
                                                    li { +"Requieren lógica de programación avanzada" }
                                                }
                                                div("type-tag dynamic") {
                                                    style = "display: inline-block; padding: 4px 10px; border-radius: 4px; font-size: 0.7rem; background: #eeeeee; color: #212121;"
                                                    +"Ideal para aplicaciones complejas"
                                                }
                                            }

                                            div("type-card") {
                                                style = "background: white; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px;"
                                                h3("type-title") { +"Breadcrumbs Basados en Rutas" }
                                                ul("type-features") {
                                                    li { +"Derivados directamente de la URL actual" }
                                                    li { +"Muy utilizados en aplicaciones SPA" }
                                                    li { +"Flexibles y adaptables" }
                                                    li { +"Dependen de la estructura de enrutamiento" }
                                                }
                                                div("type-tag path") {
                                                    style = "display: inline-block; padding: 4px 10px; border-radius: 4px; font-size: 0.7rem; background: #fafafa; color: #616161;"
                                                    +"Común en frameworks modernos"
                                                }
                                            }
                                        }

                                        div("code-example") {
                                            h3("code-title") { +"Ejemplo de Implementación" }
                                            div("code-block") {
                                                style = "background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;"
                                                pre {
                                                    style = "margin: 0; padding: 20px; overflow-x: auto;"
                                                    code {
                                                        style = "font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', monospace; font-size: 0.85rem; color: #424242;"
                                                        +"""<!-- Estructura HTML de breadcrumbs -->
<nav class="breadcrumb-nav">
  <ol class="breadcrumb-list">
    <li class="breadcrumb-item">
      <a href="/" class="breadcrumb-link">Inicio</a>
    </li>
    <li class="breadcrumb-separator">/</li>
    <li class="breadcrumb-item">
      <a href="/unidad1" class="breadcrumb-link">Unidad 1</a>
    </li>
    <li class="breadcrumb-separator">/</li>
    <li class="breadcrumb-item">
      <a href="/breadcrumbs" class="breadcrumb-link">Breadcrumbs</a>
    </li>
    <li class="breadcrumb-separator">/</li>
    <li class="breadcrumb-item active">
      <span class="breadcrumb-text">Nivel 2</span>
    </li>
  </ol>
</nav>"""
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    div("card-footer") {
                                        div("action-buttons") {
                                            a(href = "/breadcrumbs", classes = "action-button secondary") {
                                                +"Volver al Nivel 1"
                                            }
                                            a(href = "/breadcrumbs/detalle/configuracion", classes = "action-button primary") {
                                                +"Continuar al Nivel 3"
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer
                            footer("main-footer") {
                                div("footer-content") {
                                    p("footer-text") { +"© 2024 Sistema de Breadcrumbs - Unidad 1" }
                                    div("footer-links") {
                                        a(href = "#") { +"Documentación" }
                                        a(href = "#") { +"Acerca de" }
                                        a(href = "#") { +"Contacto" }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== NIVEL 3 DE BREADCRUMBS ==========
            get("/breadcrumbs/detalle/configuracion") {
                println("Sirviendo Nivel 3 de Breadcrumbs")

                try {
                    call.respondHtml {
                        head {
                            title { +"Nivel 3 - Configuración de Breadcrumbs" }
                            style { unsafe { +getMinimalistCSS() } }
                        }
                        body {
                            div("page-wrapper") {
                                // Header
                                header("main-header") {
                                    div("header-content") {
                                        h1("site-logo") {
                                            a(href = "/") { +"Breadcrumbs" }
                                        }
                                        nav("primary-nav") {
                                            a(href = "/", classes = "nav-item") { +"Inicio" }
                                            a(href = "/neon-conexion", classes = "nav-item") { +"Neon" }
                                            a(href = "/breadcrumbs", classes = "nav-item") { +"Nivel 1" }
                                            a(href = "/breadcrumbs/detalle", classes = "nav-item") { +"Nivel 2" }
                                            a(href = "#", classes = "nav-item active") { +"Nivel 3" }
                                            a(href = "/unidad2/carrusel", classes = "nav-item") { +"Carrusel" }
                                            a(href = "/unidad2/formulario", classes = "nav-item") { +"Formulario" }
                                            a(href = "/unidad2/crud-personas", classes = "nav-item") { +"CRUD" }
                                        }
                                    }
                                }

                                // Breadcrumbs jerárquicos
                                nav("breadcrumb-nav") {
                                    ol("breadcrumb-list") {
                                        li("breadcrumb-item") {
                                            a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                        }
                                        li("breadcrumb-separator") { +"/" }
                                        li("breadcrumb-item") {
                                            a(href = "#", classes = "breadcrumb-link") { +"Unidad 1" }
                                        }
                                        li("breadcrumb-separator") { +"/" }
                                        li("breadcrumb-item") {
                                            a(href = "/breadcrumbs", classes = "breadcrumb-link") { +"Nivel 1" }
                                        }
                                        li("breadcrumb-separator") { +"/" }
                                        li("breadcrumb-item") {
                                            a(href = "/breadcrumbs/detalle", classes = "breadcrumb-link") { +"Nivel 2" }
                                        }
                                        li("breadcrumb-separator") { +"/" }
                                        li("breadcrumb-item active") {
                                            span("breadcrumb-text") { +"Nivel 3" }
                                        }
                                    }
                                }

                                // Main Content
                                main("content-area") {
                                    div("content-card") {
                                        div("card-header") {
                                            h2("card-title") { +"Configuración Avanzada" }
                                            div("card-subtitle") { +"Personalización del sistema" }
                                        }

                                        div("card-body") {
                                            p("card-text") {
                                                +"¡Felicidades! Has completado exitosamente la navegación a través de los tres niveles del sistema. En esta sección final, puedes explorar las opciones de personalización disponibles."
                                            }

                                            div("configuration-panel") {
                                                style = "display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; margin: 24px 0;"

                                                div("config-section") {
                                                    style = "background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px;"
                                                    h3("config-title") { +"Personalización Visual" }
                                                    div("config-options") {
                                                        style = "display: flex; flex-direction: column; gap: 16px;"

                                                        div("config-group") {
                                                            style = "display: flex; flex-direction: column; gap: 6px;"
                                                            label("config-label") {
                                                                attributes["for"] = "separator"
                                                                +"Separador de Niveles:"
                                                            }
                                                            select {
                                                                attributes["id"] = "separator"
                                                                classes = setOf("config-select")
                                                                style = "padding: 8px 10px; border: 1px solid #bdbdbd; border-radius: 4px; background: white;"
                                                                option {
                                                                    attributes["value"] = "/"
                                                                    +"/ (Barra diagonal)"
                                                                }
                                                                option {
                                                                    attributes["value"] = ">"
                                                                    +"> (Mayor que)"
                                                                }
                                                                option {
                                                                    attributes["value"] = "›"
                                                                    +"› (Flecha delgada)"
                                                                }
                                                                option {
                                                                    attributes["value"] = "→"
                                                                    +"→ (Flecha)"
                                                                }
                                                                option {
                                                                    attributes["value"] = "»"
                                                                    +"» (Doble ángulo)"
                                                                }
                                                            }
                                                        }

                                                        div("config-group") {
                                                            style = "display: flex; flex-direction: column; gap: 6px;"
                                                            label("config-label") {
                                                                attributes["for"] = "primaryColor"
                                                                +"Color Primario:"
                                                            }
                                                            div("color-picker") {
                                                                style = "display: flex; align-items: center; gap: 10px;"
                                                                input(type = InputType.color) {
                                                                    attributes["id"] = "primaryColor"
                                                                    attributes["value"] = "#212121"
                                                                    style = "width: 50px; height: 35px; border: 1px solid #bdbdbd; border-radius: 4px;"
                                                                }
                                                                span("color-value") {
                                                                    style = "font-family: 'SF Mono', Monaco, monospace; font-size: 0.85rem; color: #616161; background: white; padding: 6px 10px; border: 1px solid #bdbdbd; border-radius: 4px; min-width: 80px;"
                                                                    +"#212121"
                                                                }
                                                            }
                                                        }

                                                        div("config-group") {
                                                            style = "display: flex; flex-direction: column; gap: 6px;"
                                                            label("config-label") {
                                                                attributes["for"] = "fontSize"
                                                                +"Tamaño de Fuente:"
                                                            }
                                                            div("slider-container") {
                                                                style = "display: flex; align-items: center; gap: 12px;"
                                                                input(type = InputType.range) {
                                                                    attributes["id"] = "fontSize"
                                                                    attributes["min"] = "12"
                                                                    attributes["max"] = "20"
                                                                    attributes["value"] = "14"
                                                                    style = "flex: 1; height: 4px; border-radius: 2px; background: #e0e0e0;"
                                                                }
                                                                span("slider-value") {
                                                                    style = "font-family: 'SF Mono', Monaco, monospace; font-size: 0.85rem; color: #616161; min-width: 40px;"
                                                                    +"14px"
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                div("config-section") {
                                                    style = "background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px;"
                                                    h3("config-title") { +"Opciones de Comportamiento" }
                                                    div("config-switches") {
                                                        style = "display: flex; flex-direction: column; gap: 12px;"

                                                        div("switch-group") {
                                                            style = "display: flex; align-items: center; gap: 10px;"
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "animations"
                                                                attributes["checked"] = "checked"
                                                                style = "width: 18px; height: 18px; border: 1px solid #bdbdbd; border-radius: 3px;"
                                                            }
                                                            label {
                                                                attributes["for"] = "animations"
                                                                span("switch-label") { +"Habilitar animaciones" }
                                                            }
                                                        }

                                                        div("switch-group") {
                                                            style = "display: flex; align-items: center; gap: 10px;"
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "responsive"
                                                                attributes["checked"] = "checked"
                                                                style = "width: 18px; height: 18px; border: 1px solid #bdbdbd; border-radius: 3px;"
                                                            }
                                                            label {
                                                                attributes["for"] = "responsive"
                                                                span("switch-label") { +"Diseño responsive" }
                                                            }
                                                        }

                                                        div("switch-group") {
                                                            style = "display: flex; align-items: center; gap: 10px;"
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "icons"
                                                                attributes["checked"] = "checked"
                                                                style = "width: 18px; height: 18px; border: 1px solid #bdbdbd; border-radius: 3px;"
                                                            }
                                                            label {
                                                                attributes["for"] = "icons"
                                                                span("switch-label") { +"Mostrar indicadores visuales" }
                                                            }
                                                        }

                                                        div("switch-group") {
                                                            style = "display: flex; align-items: center; gap: 10px;"
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "truncate"
                                                                attributes["checked"] = "checked"
                                                                style = "width: 18px; height: 18px; border: 1px solid #bdbdbd; border-radius: 3px;"
                                                            }
                                                            label {
                                                                attributes["for"] = "truncate"
                                                                span("switch-label") { +"Truncar textos largos" }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            div("completion-banner") {
                                                style = "background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; padding: 24px; margin: 24px 0; text-align: center;"
                                                div("banner-content") {
                                                    h3("banner-title") { +"Navegación Completada Exitosamente" }
                                                    p("banner-text") {
                                                        +"Has demostrado el funcionamiento completo de un sistema de breadcrumbs implementado con las mejores prácticas de desarrollo web moderno."
                                                    }
                                                    div("completion-stats") {
                                                        style = "display: flex; justify-content: center; gap: 24px; flex-wrap: wrap;"
                                                        div("stat-item") {
                                                            style = "display: flex; flex-direction: column; align-items: center;"
                                                            span("stat-number") {
                                                                style = "font-size: 1.5rem; font-weight: 300; color: #000000; margin-bottom: 4px;"
                                                                +"3"
                                                            }
                                                            span("stat-label") {
                                                                style = "font-size: 0.85rem; color: #616161;"
                                                                +"Niveles Navegados"
                                                            }
                                                        }
                                                        div("stat-item") {
                                                            style = "display: flex; flex-direction: column; align-items: center;"
                                                            span("stat-number") {
                                                                style = "font-size: 1.5rem; font-weight: 300; color: #000000; margin-bottom: 4px;"
                                                                +"100%"
                                                            }
                                                            span("stat-label") {
                                                                style = "font-size: 0.85rem; color: #616161;"
                                                                +"Funcionalidad Comprobada"
                                                            }
                                                        }
                                                        div("stat-item") {
                                                            style = "display: flex; flex-direction: column; align-items: center;"
                                                            span("stat-number") {
                                                                style = "font-size: 1.5rem; font-weight: 300; color: #000000; margin-bottom: 4px;"
                                                                +"✓"
                                                            }
                                                            span("stat-label") {
                                                                style = "font-size: 0.85rem; color: #616161;"
                                                                +"Sistema Validado"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        div("card-footer") {
                                            div("action-buttons") {
                                                a(href = "/breadcrumbs/detalle", classes = "action-button secondary") {
                                                    +"Volver al Nivel 2"
                                                }
                                                a(href = "/", classes = "action-button primary") {
                                                    +"Volver al Inicio"
                                                }
                                            }
                                        }
                                    }
                                }

                                // Footer
                                footer("main-footer") {
                                    div("footer-content") {
                                        p("footer-text") { +"© 2024 Sistema de Breadcrumbs - Unidad 1" }
                                        div("footer-links") {
                                            a(href = "#") { +"Documentación" }
                                            a(href = "#") { +"Acerca de" }
                                            a(href = "#") { +"Contacto" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    println("Nivel 3 servido exitosamente")
                } catch (e: Exception) {
                    println("ERROR en Nivel 3: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error interno del servidor")
                }
            }

            // Rutas de error de prueba
            get("/error/400") {
                call.respond(HttpStatusCode.BadRequest, "Solicitud incorrecta de prueba")
            }

            get("/error/401") {
                call.respond(HttpStatusCode.Unauthorized, "No autorizado de prueba")
            }

            get("/error/403") {
                call.respond(HttpStatusCode.Forbidden, "Acceso denegado de prueba")
            }

            get("/error/404") {
                call.respond(HttpStatusCode.NotFound, "Página no encontrada de prueba")
            }

            get("/error/500") {
                call.respond(HttpStatusCode.InternalServerError, "Error interno de prueba")
            }

            // Ruta de prueba simple
            get("/test") {
                call.respondText("Servidor funcionando correctamente", ContentType.Text.Plain)
            }

            // Ruta de salud del servidor
            get("/health") {
                call.respondText("""{"status": "healthy", "timestamp": "${Instant.now()}", "service": "breadcrumbs-system"}""", ContentType.Application.Json)
            }

            // Ruta de información del servidor
            get("/info") {
                call.respondText("""
                {
                    "name": "Sistema de Breadcrumbs",
                    "version": "1.0.0",
                    "description": "Servidor de ejemplo con manejo de errores y reCAPTCHA",
                    "timestamp": "${Instant.now()}"
                }
                """.trimIndent(), ContentType.Application.Json)
            }
        }
    }.start(wait = true)
}

fun Application.configureDatabase() {
    val host = "ep-green-thunder-aifcls8i-pooler.c-4.us-east-1.aws.neon.tech"
    val dbName = "neondb"
    val user = "neondb_owner"
    val password = "npg_hJaWEqNS6AV5"
    val jdbcUrl = "jdbc:postgresql://$host/$dbName?sslmode=require"

    try {
        Database.connect(
            url = jdbcUrl,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )
        println("✅ Conectado exitosamente a Neon PostgreSQL")

        // Crear tablas si no existen
        transaction {
            SchemaUtils.create(PalabrasGuardadasTable)
            SchemaUtils.create(PersonasTable)
            println("✅ Tablas 'palabras_guardadas' y 'personas' verificadas/creadas")
        }

    } catch (e: Exception) {
        println("❌ ERROR conectando a PostgreSQL: ${e.message}")
    }
}

// Función CSS minimalista base
fun getMinimalistCSS(): String {
    return """
    /* ===== RESET Y TIPOGRAFÍA ===== */
    * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    }
    
    body {
        background: #ffffff;
        color: #000000;
        line-height: 1.6;
        min-height: 100vh;
    }
    
    /* ===== LAYOUT PRINCIPAL ===== */
    .page-wrapper {
        max-width: 1200px;
        margin: 0 auto;
        background: white;
        min-height: 100vh;
    }
    
    /* ===== HEADER MINIMALISTA ===== */
    .main-header {
        background: #ffffff;
        border-bottom: 1px solid #e0e0e0;
        padding: 0 30px;
    }
    
    .header-content {
        max-width: 1200px;
        margin: 0 auto;
        padding: 20px 0;
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 20px;
    }
    
    .site-logo a {
        color: #000000;
        text-decoration: none;
        font-size: 1.5rem;
        font-weight: 300;
        letter-spacing: -0.5px;
    }
    
    .primary-nav {
        display: flex;
        gap: 4px;
        flex-wrap: wrap;
    }
    
    .nav-item {
        color: #616161;
        text-decoration: none;
        font-weight: 400;
        padding: 8px 16px;
        border-radius: 4px;
        transition: all 0.2s;
        font-size: 0.9rem;
    }
    
    .nav-item:hover {
        background: #f5f5f5;
        color: #000000;
    }
    
    .nav-item.active {
        background: #212121;
        color: white;
        font-weight: 400;
    }
    
    /* ===== BREADCRUMBS MINIMALISTAS ===== */
    .breadcrumb-nav {
        background: #fafafa;
        border-bottom: 1px solid #e0e0e0;
        padding: 12px 30px;
    }
    
    .breadcrumb-list {
        list-style: none;
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 6px;
    }
    
    .breadcrumb-item {
        display: flex;
        align-items: center;
    }
    
    .breadcrumb-link {
        color: #424242;
        text-decoration: none;
        font-weight: 400;
        font-size: 0.85rem;
        padding: 4px 8px;
        border-radius: 4px;
        transition: all 0.2s;
    }
    
    .breadcrumb-link:hover {
        background: #eeeeee;
        color: #000000;
    }
    
    .breadcrumb-separator {
        color: #bdbdbd;
        font-weight: 300;
        padding: 0 4px;
    }
    
    .breadcrumb-text {
        color: #212121;
        font-weight: 500;
        font-size: 0.85rem;
        padding: 4px 8px;
    }
    
    /* ===== ÁREA DE CONTENIDO ===== */
    .content-area {
        padding: 30px;
    }
    
    .content-card {
        background: white;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        overflow: hidden;
    }
    
    .card-header {
        background: #212121;
        color: white;
        padding: 24px;
    }
    
    .card-title {
        font-size: 1.75rem;
        font-weight: 300;
        margin-bottom: 6px;
    }
    
    .card-subtitle {
        font-size: 0.95rem;
        opacity: 0.8;
        font-weight: 300;
    }
    
    .card-body {
        padding: 24px;
    }
    
    .card-text {
        font-size: 1.05rem;
        line-height: 1.7;
        color: #424242;
        margin-bottom: 24px;
    }
    
    /* ===== COMPONENTES ESPECIALES ===== */
    .highlight-section {
        background: #f5f5f5;
        border: 1px solid #e0e0e0;
        padding: 20px;
        border-radius: 6px;
        margin: 24px 0;
    }
    
    .highlight-title {
        font-size: 1.125rem;
        font-weight: 500;
        color: #000000;
        margin-bottom: 12px;
    }
    
    .benefit-item {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        margin-bottom: 10px;
    }
    
    .benefit-icon {
        font-weight: 300;
        font-size: 1rem;
        margin-top: 2px;
        color: #616161;
    }
    
    .benefit-text {
        flex: 1;
        font-size: 0.95rem;
        color: #424242;
    }
    
    .info-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 20px;
        margin: 24px 0;
    }
    
    .info-block {
        background: #fafafa;
        padding: 20px;
        border-radius: 6px;
        border: 1px solid #e0e0e0;
        transition: border-color 0.2s;
    }
    
    .info-block:hover {
        border-color: #9e9e9e;
    }
    
    .info-title {
        font-size: 1.05rem;
        font-weight: 500;
        color: #000000;
        margin-bottom: 10px;
    }
    
    .info-text {
        color: #616161;
        line-height: 1.6;
        font-size: 0.95rem;
    }
    
    /* ===== EJEMPLO DE CÓDIGO ===== */
    .code-example {
        margin: 24px 0;
    }
    
    .code-title {
        font-size: 1.125rem;
        font-weight: 500;
        color: #000000;
        margin-bottom: 12px;
    }
    
    .code-block {
        background: #fafafa;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        overflow: hidden;
    }
    
    .code-block pre {
        margin: 0;
        padding: 20px;
        overflow-x: auto;
    }
    
    .code-block code {
        font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', monospace;
        font-size: 0.85rem;
        color: #424242;
        line-height: 1.6;
    }
    
    /* ===== BOTONES DE ACCIÓN ===== */
    .card-footer {
        background: #fafafa;
        padding: 20px 24px;
        border-top: 1px solid #e0e0e0;
    }
    
    .action-buttons {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        flex-wrap: wrap;
    }
    
    .action-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 10px 24px;
        border-radius: 4px;
        text-decoration: none;
        font-weight: 400;
        font-size: 0.95rem;
        transition: all 0.2s;
        border: 1px solid transparent;
    }
    
    .action-button.secondary {
        background: white;
        color: #212121;
        border-color: #bdbdbd;
    }
    
    .action-button.secondary:hover {
        background: #f5f5f5;
        border-color: #9e9e9e;
    }
    
    .action-button.primary {
        background: #212121;
        color: white;
    }
    
    .action-button.primary:hover {
        background: #424242;
    }
    
    /* ===== FOOTER ===== */
    .main-footer {
        background: #fafafa;
        border-top: 1px solid #e0e0e0;
        color: #616161;
        padding: 24px 30px;
        margin-top: 30px;
    }
    
    .footer-content {
        max-width: 1200px;
        margin: 0 auto;
        text-align: center;
    }
    
    .footer-text {
        font-size: 0.85rem;
        margin-bottom: 12px;
    }
    
    .footer-links {
        display: flex;
        justify-content: center;
        gap: 20px;
        flex-wrap: wrap;
    }
    
    .footer-links a {
        color: #616161;
        text-decoration: none;
        font-size: 0.85rem;
        transition: color 0.2s;
    }
    
    .footer-links a:hover {
        color: #212121;
    }
    
    /* ===== RESPONSIVE DESIGN ===== */
    @media (max-width: 768px) {
        .header-content {
            flex-direction: column;
            text-align: center;
            padding: 16px 0;
        }
        
        .primary-nav {
            justify-content: center;
        }
        
        .content-area {
            padding: 20px;
        }
        
        .card-body {
            padding: 20px;
        }
        
        .action-buttons {
            flex-direction: column;
        }
        
        .action-button {
            width: 100%;
        }
        
        .breadcrumb-nav {
            padding: 12px 20px;
        }
        
        .main-header {
            padding: 0 20px;
        }
    }
    """
}

// Función CSS para el carrusel
fun getMinimalistCSSConCarrusel(): String {
    return getMinimalistCSS() + """
    /* ===== CARRUSEL CSS PURO CON ANIMACIÓN AUTOMÁTICA ===== */
    .carousel-section {
        margin: 40px 0;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        overflow: hidden;
    }
    
    .carousel-header {
        background: #fafafa;
        padding: 20px;
        border-bottom: 1px solid #e0e0e0;
    }
    
    .carousel-title {
        font-size: 1.25rem;
        font-weight: 500;
        color: #000000;
        margin-bottom: 8px;
    }
    
    .carousel-subtitle {
        font-size: 0.95rem;
        color: #616161;
    }
    
    .carousel-container {
        position: relative;
        width: 100%;
        height: 400px;
        overflow: hidden;
    }
    
    .carousel-track {
        display: flex;
        width: 400%;
        height: 100%;
        animation: carousel-auto-slide 20s infinite;
    }
    
    .carousel-slide {
        width: 25%;
        height: 100%;
        flex-shrink: 0;
        position: relative;
        opacity: 1;
        transition: opacity 0.5s ease-in-out;
    }
    
    .carousel-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }
    
    .carousel-caption {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        background: rgba(0, 0, 0, 0.7);
        color: white;
        padding: 15px 20px;
        opacity: 0;
        transform: translateY(20px);
        transition: all 0.5s ease 0.3s;
    }
    
    .carousel-caption h3 {
        font-size: 1.125rem;
        font-weight: 400;
        margin-bottom: 5px;
    }
    
    .carousel-caption p {
        font-size: 0.9rem;
        opacity: 0.9;
        margin: 0;
    }
    
    .carousel-slide:hover .carousel-caption {
        opacity: 1;
        transform: translateY(0);
    }
    
    /* Animación automática del carrusel */
    @keyframes carousel-auto-slide {
        0%, 20% {
            transform: translateX(0%);
        }
        25%, 45% {
            transform: translateX(-25%);
        }
        50%, 70% {
            transform: translateX(-50%);
        }
        75%, 95% {
            transform: translateX(-75%);
        }
        100% {
            transform: translateX(0%);
        }
    }
    
    /* Controles manuales */
    .carousel-controls {
        position: absolute;
        top: 50%;
        left: 0;
        right: 0;
        transform: translateY(-50%);
        display: flex;
        justify-content: space-between;
        padding: 0 20px;
        pointer-events: none;
        z-index: 10;
    }
    
    .carousel-btn {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid #e0e0e0;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s;
        pointer-events: auto;
        color: #212121;
        font-size: 1.2rem;
        position: absolute;
        top: 50%;
        transform: translateY(-50%);
        opacity: 0;
        transition: opacity 0.3s;
    }
    
    .carousel-container:hover .carousel-btn {
        opacity: 1;
    }
    
    .carousel-btn:hover {
        background: #ffffff;
        border-color: #9e9e9e;
        transform: translateY(-50%) scale(1.1);
    }
    
    .prev-btn {
        left: 20px;
    }
    .next-btn {
        right: 20px;
    }
    
    /* Indicadores */
    .carousel-indicators {
        display: flex;
        justify-content: center;
        gap: 10px;
        padding: 20px;
        background: #fafafa;
        border-top: 1px solid #e0e0e0;
        position: relative;
        z-index: 10;
    }
    
    .carousel-dot {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: #bdbdbd;
        border: none;
        cursor: pointer;
        transition: background 0.3s;
        padding: 0;
        display: block;
        position: relative;
    }
    
    .carousel-dot:hover {
        background: #212121;
    }
    
    /* Animación de los indicadores */
    .carousel-dot::after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        border-radius: 50%;
        background: #212121;
        opacity: 0;
        animation: dot-progress 20s linear infinite;
    }
    
    .carousel-dot:nth-child(1)::after {
        animation-delay: 0s;
    }
    
    .carousel-dot:nth-child(2)::after {
        animation-delay: 5s;
    }
    
    .carousel-dot:nth-child(3)::after {
        animation-delay: 10s;
    }
    
    .carousel-dot:nth-child(4)::after {
        animation-delay: 15s;
    }
    
    @keyframes dot-progress {
        0%, 24.99% {
            opacity: 1;
        }
        25%, 100% {
            opacity: 0;
        }
    }
    
    /* Barra de progreso */
    .carousel-progress {
        height: 3px;
        background: #e0e0e0;
        overflow: hidden;
        margin-top: -1px;
        position: relative;
    }
    
    .progress-bar {
        height: 100%;
        width: 100%;
        background: #212121;
        position: absolute;
        left: -100%;
        animation: progress-animation 20s linear infinite;
    }
    
    @keyframes progress-animation {
        0% {
            left: -100%;
        }
        100% {
            left: 100%;
        }
    }
    
    @media (max-width: 768px) {
        .carousel-container {
            height: 300px;
        }
        
        .carousel-caption h3 {
            font-size: 1rem;
        }
        
        .carousel-caption p {
            font-size: 0.85rem;
        }
        
        .carousel-btn {
            width: 35px;
            height: 35px;
            font-size: 1rem;
        }
    }
    """
}

// Función CSS para el formulario tradicional
fun getMinimalistCSSConFormulario(): String {
    return getMinimalistCSS() + """
    /* ===== FORMULARIO MINIMALISTA ===== */
    .form-container {
        background: #ffffff;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        padding: 40px;
    }
    
    .form-header {
        text-align: center;
        margin-bottom: 30px;
    }
    
    .form-title {
        font-size: 1.75rem;
        font-weight: 300;
        color: #000000;
        margin-bottom: 10px;
    }
    
    .form-subtitle {
        color: #616161;
        font-size: 1rem;
        max-width: 600px;
        margin: 0 auto;
    }
    
    .form-wrapper {
        max-width: 600px;
        margin: 0 auto;
    }
    
    .form-group {
        margin-bottom: 20px;
        position: relative;
    }
    
    .form-label {
        display: block;
        margin-bottom: 6px;
        font-weight: 500;
        color: #212121;
        font-size: 0.9rem;
    }
    
    .required::after {
        content: ' *';
        color: #d32f2f;
    }
    
    .form-input {
        width: 100%;
        padding: 12px 14px;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        font-size: 0.95rem;
        color: #212121;
        background: white;
        transition: all 0.3s ease;
    }
    
    .form-input:focus {
        outline: none;
        border-color: #212121;
        box-shadow: 0 0 0 2px rgba(33, 33, 33, 0.1);
    }
    
    /* ===== VALIDACIONES EN TIEMPO REAL CON HTML5/CSS ===== */
    .form-input:valid:not(:placeholder-shown) {
        border-color: #4caf50;
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%234caf50' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: right 12px center;
        background-size: 20px;
        padding-right: 40px;
    }
    
    .form-input:invalid:not(:placeholder-shown) {
        border-color: #f44336;
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%23f44336' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'%3E%3C/circle%3E%3Cline x1='12' y1='8' x2='12' y2='12'%3E%3C/line%3E%3Cline x1='12' y1='16' x2='12.01' y2='16'%3E%3C/line%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: right 12px center;
        background-size: 20px;
        padding-right: 40px;
    }
    
    .real-time-error {
        color: #f44336;
        font-size: 0.8rem;
        margin-top: 4px;
        display: none;
    }
    
    .form-input:invalid:not(:placeholder-shown) + .real-time-error {
        display: block;
        animation: fadeIn 0.3s ease;
    }
    
    .form-input:valid:not(:placeholder-shown) + .real-time-error {
        display: none;
    }
    
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(-5px); }
        to { opacity: 1; transform: translateY(0); }
    }
    
    .field-strength {
        height: 3px;
        background: #e0e0e0;
        border-radius: 2px;
        margin-top: 4px;
        overflow: hidden;
        position: relative;
    }
    
    .field-strength::after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        height: 100%;
        width: 0;
        background: #4caf50;
        transition: width 0.3s ease;
    }
    
    .form-input:focus + .field-strength::after {
        width: 100%;
    }
    
    .form-input:valid:not(:placeholder-shown) + .field-strength::after {
        background: #4caf50;
        width: 100%;
    }
    
    .form-input:invalid:not(:placeholder-shown) + .field-strength::after {
        background: #f44336;
        width: 100%;
    }
    
    .word-counter {
        position: absolute;
        right: 12px;
        bottom: -20px;
        font-size: 0.75rem;
        color: #757575;
        background: white;
        padding: 2px 6px;
        border-radius: 3px;
        border: 1px solid #e0e0e0;
    }
    
    .word-counter.warning {
        color: #ff9800;
        border-color: #ff9800;
    }
    
    .word-counter.error {
        color: #f44336;
        border-color: #f44336;
    }
    
    .checkbox-group input[type="checkbox"]:invalid {
        outline: 2px solid #f44336;
        border-radius: 3px;
    }
    
    .form-hint {
        font-size: 0.8rem;
        color: #757575;
        margin-top: 4px;
        display: flex;
        align-items: center;
        gap: 4px;
    }
    
    .error-message {
        color: #d32f2f;
        font-size: 0.8rem;
        margin-top: 4px;
        display: block;
    }
    
    .textarea-field {
        width: 100%;
        padding: 12px 14px;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        font-size: 0.95rem;
        color: #212121;
        background: white;
        resize: vertical;
        min-height: 100px;
        font-family: inherit;
        transition: border-color 0.2s;
    }
    
    .textarea-field:focus {
        outline: none;
        border-color: #212121;
    }
    
    .recaptcha-container {
        background: #fafafa;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        padding: 15px;
        margin-bottom: 15px;
    }
    
    .recaptcha-badge {
        background: #212121;
        color: white;
        padding: 10px 14px;
        border-radius: 4px;
        margin-bottom: 12px;
        text-align: center;
        font-size: 0.85rem;
    }
    
    .recaptcha-badge a {
        color: #bdbdbd;
        text-decoration: underline;
    }
    
    .recaptcha-badge a:hover {
        color: white;
    }
    
    .form-buttons {
        display: flex;
        gap: 12px;
        margin-top: 25px;
    }
    
    .submit-btn {
        flex: 1;
        padding: 14px;
        background: #212121;
        color: white;
        border: none;
        border-radius: 4px;
        font-size: 1rem;
        font-weight: 400;
        cursor: pointer;
        transition: background 0.2s;
    }
    
    .submit-btn:hover {
        background: #424242;
    }
    
    .reset-btn {
        flex: 1;
        padding: 14px;
        background: #f5f5f5;
        color: #212121;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        font-size: 1rem;
        font-weight: 400;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .reset-btn:hover {
        background: #eeeeee;
    }
    
    .form-footer {
        text-align: center;
        margin-top: 25px;
        padding-top: 20px;
        border-top: 1px solid #e0e0e0;
        color: #757575;
        font-size: 0.85rem;
    }
    
    .checkbox-group {
        display: flex;
        align-items: center;
        gap: 8px;
    }
    
    .checkbox-group input[type="checkbox"] {
        width: 18px;
        height: 18px;
    }
    
    .success-message {
        background: #d4edda;
        color: #155724;
        padding: 12px 16px;
        border-radius: 4px;
        margin-bottom: 20px;
        border: 1px solid #c3e6cb;
        animation: fadeIn 0.5s ease;
    }
    
    @media (max-width: 768px) {
        .form-container {
            padding: 20px;
        }
        
        .form-title {
            font-size: 1.5rem;
        }
        
        .form-buttons {
            flex-direction: column;
        }
    }
    """
}

// Función CSS para el CRUD
fun getMinimalistCSSConCRUD(): String {
    return getMinimalistCSS() + """
    /* ===== ESTILOS PARA CRUD ===== */
    .crud-filters {
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
        align-items: center;
        flex-wrap: wrap;
    }
    
    .crud-table-container {
        overflow-x: auto;
        margin-bottom: 20px;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
    }
    
    .crud-table {
        width: 100%;
        border-collapse: collapse;
        min-width: 800px;
    }
    
    .crud-table th {
        padding: 12px;
        text-align: left;
        border-bottom: 2px solid #e0e0e0;
        font-weight: 600;
        color: #212121;
        background: #f5f5f5;
    }
    
    .crud-table td {
        padding: 12px;
        border-bottom: 1px solid #e0e0e0;
    }
    
    .crud-table tr:hover {
        background: #fafafa;
    }
    
    .crud-actions {
        display: flex;
        justify-content: flex-end;
        margin-bottom: 20px;
    }
    
    /* Estados */
    .estado-activo {
        background: #e8f5e8;
        color: #2e7d32;
        padding: 4px 8px;
        border-radius: 12px;
        font-size: 0.85rem;
        display: inline-block;
    }
    
    .estado-inactivo {
        background: #ffebee;
        color: #c62828;
        padding: 4px 8px;
        border-radius: 12px;
        font-size: 0.85rem;
        display: inline-block;
    }
    
    /* Modal */
    .crud-modal {
        display: none;
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.5);
        z-index: 2000;
        align-items: center;
        justify-content: center;
    }
    
    .crud-modal-content {
        background: white;
        border-radius: 8px;
        max-width: 500px;
        width: 90%;
        max-height: 90vh;
        overflow-y: auto;
        position: relative;
    }
    
    .crud-modal-header {
        padding: 20px;
        border-bottom: 1px solid #e0e0e0;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    
    .crud-modal-title {
        font-size: 1.25rem;
        font-weight: 500;
        color: #212121;
        margin: 0;
    }
    
    .crud-modal-body {
        padding: 20px;
    }
    
    /* Formulario dentro del modal */
    .form-group {
        margin-bottom: 15px;
    }
    
    .form-label {
        display: block;
        margin-bottom: 6px;
        font-weight: 500;
        color: #212121;
        font-size: 0.9rem;
    }
    
    .form-label.required::after {
        content: ' *';
        color: #d32f2f;
    }
    
    .form-input {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        font-size: 0.95rem;
        color: #212121;
        background: white;
        transition: all 0.3s ease;
    }
    
    .form-input:focus {
        outline: none;
        border-color: #212121;
        box-shadow: 0 0 0 2px rgba(33, 33, 33, 0.1);
    }
    
    .form-input.error {
        border-color: #f44336;
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%23f44336' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'%3E%3C/circle%3E%3Cline x1='12' y1='8' x2='12' y2='12'%3E%3C/line%3E%3Cline x1='12' y1='16' x2='12.01' y2='16'%3E%3C/line%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: right 12px center;
        background-size: 20px;
        padding-right: 40px;
    }
    
    .form-input.valid {
        border-color: #4caf50;
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%234caf50' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: right 12px center;
        background-size: 20px;
        padding-right: 40px;
    }
    
    .real-time-error {
        color: #f44336;
        font-size: 0.8rem;
        margin-top: 4px;
        display: none;
    }
    
    .real-time-error[style*="display: block"] {
        display: block;
        animation: fadeIn 0.3s ease;
    }
    
    .form-buttons {
        display: flex;
        gap: 10px;
        margin-top: 20px;
    }
    
    .submit-btn {
        flex: 2;
        padding: 12px;
        background: #212121;
        color: white;
        border: none;
        border-radius: 4px;
        font-size: 1rem;
        cursor: pointer;
        transition: background 0.2s;
    }
    
    .submit-btn:hover {
        background: #424242;
    }
    
    .submit-btn:disabled {
        background: #9e9e9e;
        cursor: not-allowed;
    }
    
    .reset-btn {
        flex: 1;
        padding: 12px;
        background: #f5f5f5;
        color: #212121;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .reset-btn:hover {
        background: #eeeeee;
    }
    
    /* Mensajes */
    .success-message {
        background: #d4edda;
        color: #155724;
        padding: 12px 16px;
        border-radius: 4px;
        margin-bottom: 20px;
        border: 1px solid #c3e6cb;
        animation: fadeIn 0.5s ease;
    }
    
    .error-message {
        background: #ffebee;
        color: #d32f2f;
        padding: 12px 16px;
        border-radius: 4px;
        margin-bottom: 20px;
        border: 1px solid #ffcdd2;
        animation: fadeIn 0.5s ease;
    }
    
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(-5px); }
        to { opacity: 1; transform: translateY(0); }
    }
    
    /* Responsive */
    @media (max-width: 768px) {
        .crud-filters {
            flex-direction: column;
            align-items: stretch;
        }
        
        .crud-filters input {
            width: 100%;
        }
        
        .crud-actions {
            justify-content: center;
        }
        
        .crud-modal-content {
            width: 95%;
        }
    }
    """
}

// Configuración del manejo de errores
fun Application.configureStatusPages() {
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        // Manejo de error 404 - Ruta no encontrada
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondHtml(status) {
                head {
                    title { +"404 - Página No Encontrada" }
                    style { unsafe { +getMinimalistErrorCSS() } }
                }
                body {
                    div("error-container") {
                        div("error-content") {
                            h1("error-code") { +"404" }
                            h2("error-title") { +"Página No Encontrada" }
                            p("error-message") {
                                +"La página que estás buscando no existe o ha sido movida."
                            }
                            div("error-details") {
                                p {
                                    +"Ruta solicitada: "
                                    strong { +call.request.uri }
                                }
                                p {
                                    +"Método: "
                                    strong { +call.request.httpMethod.value }
                                }
                                p {
                                    +"Timestamp: "
                                    strong { +Instant.now().toString() }
                                }
                            }
                            div("action-buttons") {
                                a(href = "/", classes = "error-button primary") {
                                    +"Volver al Inicio"
                                }
                                a(href = "/breadcrumbs", classes = "error-button secondary") {
                                    +"Ir a Breadcrumbs"
                                }
                            }
                            div("error-help") {
                                p { +"Si crees que esto es un error, por favor contacta al administrador." }
                            }
                        }
                    }
                }
            }
        }

        // Manejo de error 400 - Bad Request
        status(HttpStatusCode.BadRequest) { call, status ->
            call.respondHtml(status) {
                head {
                    title { +"400 - Solicitud Incorrecta" }
                    style { unsafe { +getMinimalistErrorCSS() } }
                }
                body {
                    div("error-container") {
                        div("error-content") {
                            h1("error-code") { +"400" }
                            h2("error-title") { +"Solicitud Incorrecta" }
                            p("error-message") {
                                +"La solicitud no pudo ser procesada por el servidor debido a un error en la sintaxis."
                            }
                            div("action-buttons") {
                                a(href = "/", classes = "error-button primary") {
                                    +"Volver al Inicio"
                                }
                                a(href = "javascript:history.back()", classes = "error-button secondary") {
                                    +"Regresar"
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manejo de error 500 - Internal Server Error
        status(HttpStatusCode.InternalServerError) { call, status ->
            call.respondHtml(status) {
                head {
                    title { +"500 - Error del Servidor" }
                    style { unsafe { +getMinimalistErrorCSS() } }
                }
                body {
                    div("error-container") {
                        div("error-content") {
                            h1("error-code") { +"500" }
                            h2("error-title") { +"Error Interno del Servidor" }
                            p("error-message") {
                                +"Ha ocurrido un error interno en el servidor. Por favor, intenta nuevamente más tarde."
                            }
                            div("action-buttons") {
                                a(href = "/", classes = "error-button primary") {
                                    +"Volver al Inicio"
                                }
                                button(classes = "error-button secondary") {
                                    attributes["onclick"] = "location.reload()"
                                    +"Reintentar"
                                }
                            }
                            div("error-help") {
                                p { +"Si el problema persiste, por favor contacta al administrador del sistema." }
                            }
                        }
                    }
                }
            }
        }

        // Manejo de error 403 - Forbidden
        status(HttpStatusCode.Forbidden) { call, status ->
            call.respondHtml(status) {
                head {
                    title { +"403 - Acceso Denegado" }
                    style { unsafe { +getMinimalistErrorCSS() } }
                }
                body {
                    div("error-container") {
                        div("error-content") {
                            h1("error-code") { +"403" }
                            h2("error-title") { +"Acceso Denegado" }
                            p("error-message") {
                                +"No tienes permisos para acceder a este recurso."
                            }
                            div("action-buttons") {
                                a(href = "/", classes = "error-button primary") {
                                    +"Volver al Inicio"
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manejo de error 401 - Unauthorized
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respondHtml(status) {
                head {
                    title { +"401 - No Autorizado" }
                    style { unsafe { +getMinimalistErrorCSS() } }
                }
                body {
                    div("error-container") {
                        div("error-content") {
                            h1("error-code") { +"401" }
                            h2("error-title") { +"No Autorizado" }
                            p("error-message") {
                                +"Debes autenticarte para acceder a este recurso."
                            }
                            div("action-buttons") {
                                a(href = "/", classes = "error-button primary") {
                                    +"Volver al Inicio"
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manejo de excepciones generales
        exception<Throwable> { call, cause ->
            println("ERROR NO MANEJADO: ${cause.message}")
            println("Stack trace: ${cause.stackTraceToString()}")

            call.respondHtml(HttpStatusCode.InternalServerError) {
                head {
                    title { +"500 - Error del Sistema" }
                    style { unsafe { +getMinimalistErrorCSS() } }
                }
                body {
                    div("error-container") {
                        div("error-content") {
                            h1("error-code") { +"500" }
                            h2("error-title") { +"Error del Sistema" }
                            p("error-message") {
                                +"Ha ocurrido un error inesperado en el sistema."
                            }

                            // Solo mostrar detalles del error en modo desarrollo
                            val isDevelopment = System.getenv("ENVIRONMENT") == "development"
                            if (isDevelopment) {
                                div("error-details") {
                                    p {
                                        +"Error: "
                                        strong { +cause.message.toString() }
                                    }
                                    pre("error-stack") {
                                        +cause.stackTraceToString()
                                    }
                                }
                            } else {
                                div("error-details") {
                                    p {
                                        +"Error ID: "
                                        strong { +Instant.now().toEpochMilli().toString() }
                                    }
                                }
                            }

                            div("action-buttons") {
                                a(href = "/", classes = "error-button primary") {
                                    +"Volver al Inicio"
                                }
                                button(classes = "error-button secondary") {
                                    attributes["onclick"] = "location.reload()"
                                    +"Reintentar"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Función CSS minimalista para páginas de error
fun getMinimalistErrorCSS(): String {
    return """
    * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    }
    
    body {
        background: #ffffff;
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
    }
    
    .error-container {
        max-width: 800px;
        width: 100%;
    }
    
    .error-content {
        background: #ffffff;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 40px;
        text-align: center;
    }
    
    .error-code {
        font-size: 6rem;
        font-weight: 300;
        color: #000000;
        line-height: 1;
        margin-bottom: 16px;
    }
    
    .error-title {
        font-size: 2rem;
        font-weight: 400;
        color: #212121;
        margin-bottom: 12px;
    }
    
    .error-message {
        font-size: 1.125rem;
        color: #616161;
        line-height: 1.6;
        max-width: 600px;
        margin: 0 auto 24px;
    }
    
    .error-details {
        background: #fafafa;
        border-radius: 4px;
        padding: 20px;
        margin: 24px 0;
        text-align: left;
        border: 1px solid #e0e0e0;
    }
    
    .error-details p {
        margin-bottom: 10px;
        color: #616161;
        font-size: 0.95rem;
    }
    
    .error-details strong {
        color: #212121;
        font-weight: 500;
    }
    
    .error-stack {
        background: #f5f5f5;
        color: #424242;
        padding: 16px;
        border-radius: 4px;
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.85rem;
        overflow-x: auto;
        text-align: left;
        margin-top: 12px;
        white-space: pre-wrap;
        word-wrap: break-word;
    }
    
    .action-buttons {
        display: flex;
        gap: 12px;
        justify-content: center;
        margin-top: 24px;
        flex-wrap: wrap;
    }
    
    .error-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 12px 24px;
        border-radius: 4px;
        text-decoration: none;
        font-weight: 400;
        font-size: 1rem;
        transition: all 0.2s;
        border: 1px solid transparent;
        cursor: pointer;
        min-width: 160px;
    }
    
    .error-button.primary {
        background: #212121;
        color: white;
    }
    
    .error-button.primary:hover {
        background: #424242;
    }
    
    .error-button.secondary {
        background: white;
        color: #212121;
        border-color: #bdbdbd;
    }
    
    .error-button.secondary:hover {
        background: #f5f5f5;
    }
    
    .error-help {
        margin-top: 24px;
        padding-top: 20px;
        border-top: 1px solid #e0e0e0;
        color: #757575;
        font-size: 0.9rem;
    }
    
    @media (max-width: 768px) {
        .error-content {
            padding: 30px 20px;
        }
        
        .error-code {
            font-size: 4rem;
        }
        
        .error-title {
            font-size: 1.5rem;
        }
        
        .action-buttons {
            flex-direction: column;
        }
        
        .error-button {
            width: 100%;
        }
    }
    
    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
    
    .error-content {
        animation: fadeIn 0.5s ease-out;
    }
    """
}