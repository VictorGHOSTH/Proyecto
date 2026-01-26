package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.html.*
import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.html.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val status: Int,
    val path: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

fun Application.configureErrorHandling() {
    install(StatusPages) {
        // ========== ERRORES JSON (API) ==========

        // Error 400 - Bad Request (JSON)
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "Bad Request",
                    message = cause.message ?: "Solicitud inválida",
                    status = 400,
                    path = call.request.uri
                )
            )
        }

        // Error 404 - Not Found (JSON)
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = "Not Found",
                    message = cause.message ?: "Recurso no encontrado",
                    status = 404,
                    path = call.request.uri
                )
            )
        }

        // Error 500 - Internal Server Error (genérico - JSON)
        exception<Throwable> { call, cause ->
            val status = when (cause) {
                is BadRequestException -> HttpStatusCode.BadRequest
                is NotFoundException -> HttpStatusCode.NotFound
                else -> HttpStatusCode.InternalServerError
            }

            val errorMessage = if (status == HttpStatusCode.InternalServerError) {
                "Ocurrió un error interno en el servidor"
            } else {
                cause.message ?: "Error desconocido"
            }

            call.respond(
                status,
                ErrorResponse(
                    error = status.description,
                    message = errorMessage,
                    status = status.value,
                    path = call.request.uri
                )
            )

            // Log del error real
            println("❌ ERROR [${call.request.uri}]: ${cause.message}")
            cause.printStackTrace()
        }

        // ========== ERRORES HTML (Páginas web) ==========

        // Error 404 - Not Found (HTML)
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondHtml(HttpStatusCode.NotFound) {
                head {
                    title { +"404 - No encontrado" }
                    style {
                        unsafe {
                            +"""
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                height: 100vh;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                margin: 0;
                                color: white;
                            }
                            .error-container {
                                text-align: center;
                                background: rgba(255, 255, 255, 0.1);
                                backdrop-filter: blur(10px);
                                padding: 3rem;
                                border-radius: 20px;
                                box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                                max-width: 500px;
                            }
                            h1 {
                                font-size: 4rem;
                                margin: 0;
                                color: #ff6b6b;
                            }
                            h2 {
                                margin-top: 0;
                                font-weight: 300;
                            }
                            p {
                                font-size: 1.1rem;
                                opacity: 0.9;
                            }
                            .home-button {
                                display: inline-block;
                                margin-top: 2rem;
                                padding: 12px 30px;
                                background: #4CAF50;
                                color: white;
                                text-decoration: none;
                                border-radius: 50px;
                                font-weight: bold;
                                transition: transform 0.3s, background 0.3s;
                            }
                            .home-button:hover {
                                background: #45a049;
                                transform: translateY(-3px);
                            }
                            """
                        }
                    }
                }
                body {
                    div(classes = "error-container") {
                        h1 { +"404" }
                        h2 { +"¡Ups! Página no encontrada" }
                        p { +"La página que buscas no existe o ha sido movida." }
                        p { +"Error en: ${call.request.uri}" }
                        a(classes = "home-button", href = "/") {
                            +"Volver al inicio"
                        }
                    }
                }
            }
        }

        // Error 500 - Internal Server Error (HTML)
        status(HttpStatusCode.InternalServerError) { call, _ ->
            call.respondHtml(HttpStatusCode.InternalServerError) {
                head {
                    title { +"500 - Error interno" }
                    style {
                        unsafe {
                            +"""
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                                height: 100vh;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                margin: 0;
                                color: white;
                            }
                            .error-container {
                                text-align: center;
                                background: rgba(255, 255, 255, 0.1);
                                backdrop-filter: blur(10px);
                                padding: 3rem;
                                border-radius: 20px;
                                box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                                max-width: 500px;
                            }
                            h1 {
                                font-size: 4rem;
                                margin: 0;
                                color: #ffd166;
                            }
                            h2 {
                                margin-top: 0;
                                font-weight: 300;
                            }
                            p {
                                font-size: 1.1rem;
                                opacity: 0.9;
                            }
                            .error-id {
                                background: rgba(255,255,255,0.2);
                                padding: 10px;
                                border-radius: 5px;
                                font-family: monospace;
                                margin: 1rem 0;
                            }
                            .actions {
                                margin-top: 2rem;
                                display: flex;
                                gap: 1rem;
                                justify-content: center;
                            }
                            .button {
                                padding: 12px 25px;
                                background: #4CAF50;
                                color: white;
                                text-decoration: none;
                                border-radius: 50px;
                                font-weight: bold;
                                transition: transform 0.3s, background 0.3s;
                            }
                            .button:hover {
                                transform: translateY(-3px);
                            }
                            .button-secondary {
                                background: #2196F3;
                            }
                            """
                        }
                    }
                }
                body {
                    div(classes = "error-container") {
                        h1 { +"500" }
                        h2 { +"Error interno del servidor" }
                        p { +"Algo salió mal en nuestro servidor. Nuestro equipo ha sido notificado." }
                        div(classes = "error-id") {
                            +"ID: ${System.currentTimeMillis()}"
                        }
                        div(classes = "actions") {
                            a(classes = "button", href = "/") {
                                +"Volver al inicio"
                            }
                            a(classes = "button button-secondary", href = "javascript:location.reload()") {
                                +"Reintentar"
                            }
                        }
                    }
                }
            }
        }

        // Error 400 - Bad Request (HTML) - Opcional
        status(HttpStatusCode.BadRequest) { call, _ ->
            call.respondHtml(HttpStatusCode.BadRequest) {
                head {
                    title { +"400 - Solicitud incorrecta" }
                    style {
                        unsafe {
                            +"""
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                background: linear-gradient(135deg, #fad961 0%, #f76b1c 100%);
                                height: 100vh;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                margin: 0;
                                color: white;
                            }
                            .error-container {
                                text-align: center;
                                background: rgba(255, 255, 255, 0.1);
                                backdrop-filter: blur(10px);
                                padding: 3rem;
                                border-radius: 20px;
                                box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                                max-width: 500px;
                            }
                            h1 {
                                font-size: 4rem;
                                margin: 0;
                                color: #ffd166;
                            }
                            h2 {
                                margin-top: 0;
                                font-weight: 300;
                            }
                            p {
                                font-size: 1.1rem;
                                opacity: 0.9;
                            }
                            .home-button {
                                display: inline-block;
                                margin-top: 2rem;
                                padding: 12px 30px;
                                background: #2196F3;
                                color: white;
                                text-decoration: none;
                                border-radius: 50px;
                                font-weight: bold;
                                transition: transform 0.3s, background 0.3s;
                            }
                            .home-button:hover {
                                background: #1976D2;
                                transform: translateY(-3px);
                            }
                            """
                        }
                    }
                }
                body {
                    div(classes = "error-container") {
                        h1 { +"400" }
                        h2 { +"Solicitud incorrecta" }
                        p { +"La solicitud que envió contiene errores o parámetros inválidos." }
                        p { +"Error en: ${call.request.uri}" }
                        a(classes = "home-button", href = "/") {
                            +"Volver al inicio"
                        }
                    }
                }
            }
        }
    }
}