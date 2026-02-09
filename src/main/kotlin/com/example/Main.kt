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

fun main() {
    println("Iniciando servidor en puerto 8080...")

    embeddedServer(Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = "0.0.0.0"
    ) {
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
                                
                                /* ===== NIVELES ===== */
                                .levels-container {
                                    display: grid;
                                    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                                    gap: 20px;
                                    margin: 30px 0;
                                }
                                
                                .level-card {
                                    background: #ffffff;
                                    border: 1px solid #e0e0e0;
                                    border-radius: 6px;
                                    padding: 25px;
                                    text-align: center;
                                    text-decoration: none;
                                    color: #212121;
                                    transition: all 0.2s ease;
                                    position: relative;
                                }
                                
                                .level-card:hover {
                                    border-color: #9e9e9e;
                                    transform: translateY(-2px);
                                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
                                }
                                
                                .level-number {
                                    display: inline-block;
                                    background: #212121;
                                    color: white;
                                    width: 40px;
                                    height: 40px;
                                    border-radius: 50%;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 1.125rem;
                                    font-weight: 400;
                                    margin: 0 auto 15px;
                                }
                                
                                .level-title {
                                    font-size: 1.25rem;
                                    font-weight: 500;
                                    margin-bottom: 10px;
                                    color: #000000;
                                }
                                
                                .level-description {
                                    color: #616161;
                                    font-size: 0.95rem;
                                    line-height: 1.5;
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
                                }
                                
                                /* Ocultar inputs radio */
                                .carousel-track input[type="radio"] {
                                    
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
                                
                                .carousel-track {
                                    animation: carousel-auto-slide 20s infinite;
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
                                
                                /* Posicionar botones */
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
                                    transition: border-color 0.2s;
                                }
                                
                                .form-input:focus {
                                    outline: none;
                                    border-color: #212121;
                                }
                                
                                .form-input.error {
                                    border-color: #d32f2f;
                                }
                                
                                .form-input.success {
                                    border-color: #388e3c;
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
                                    display: none;
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
                                
                                /* ===== reCAPTCHA ===== */
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
                                
                                /* ===== BOTONES DEL FORMULARIO ===== */
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
                                
                                /* ===== RESULTADOS ===== */
                                .form-footer {
                                    text-align: center;
                                    margin-top: 25px;
                                    padding-top: 20px;
                                    border-top: 1px solid #e0e0e0;
                                    color: #757575;
                                    font-size: 0.85rem;
                                }
                                
                                .form-result {
                                    margin-top: 25px;
                                    padding: 20px;
                                    background: #fafafa;
                                    border-radius: 4px;
                                    border: 1px solid #e0e0e0;
                                    display: none;
                                }
                                
                                .result-title {
                                    font-size: 1rem;
                                    font-weight: 500;
                                    color: #212121;
                                    margin-bottom: 10px;
                                }
                                
                                .result-content {
                                    color: #424242;
                                    font-family: 'SF Mono', Monaco, monospace;
                                    background: white;
                                    padding: 15px;
                                    border-radius: 4px;
                                    border: 1px solid #e0e0e0;
                                    font-size: 0.85rem;
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
                                    
                                    .levels-container {
                                        grid-template-columns: 1fr;
                                    }
                                    
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
                                    
                                    .form-title {
                                        font-size: 1.5rem;
                                    }
                                    
                                    .form-buttons {
                                        flex-direction: column;
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
                                """
                            }
                        }
                        // CARGAR reCAPTCHA
                        script {
                            src = "https://www.google.com/recaptcha/api.js"
                            async = true
                            defer = true
                        }
                        script {
                            unsafe {
                                +"""
                                // Variable para almacenar la respuesta del reCAPTCHA
                                let recaptchaToken = '';
                                let recaptchaVerified = false;
                                
                                // Callback cuando reCAPTCHA es exitoso
                                function onRecaptchaSuccess(response) {
                                    console.log('reCAPTCHA verificado exitosamente:', response);
                                    recaptchaToken = response;
                                    recaptchaVerified = true;
                                    
                                    // Quitar error si existía
                                    document.getElementById('recaptchaError').style.display = 'none';
                                }
                                
                                // Callback cuando reCAPTCHA expira
                                function onRecaptchaExpired() {
                                    console.log('reCAPTCHA expirado');
                                    recaptchaToken = '';
                                    recaptchaVerified = false;
                                }
                                
                                // Validar reCAPTCHA
                                function validateRecaptcha() {
                                    const recaptchaError = document.getElementById('recaptchaError');
                                    
                                    if (!recaptchaVerified || !recaptchaToken) {
                                        recaptchaError.textContent = 'Por favor, complete la verificación de seguridad';
                                        recaptchaError.style.display = 'block';
                                        return false;
                                    }
                                    
                                    recaptchaError.style.display = 'none';
                                    return true;
                                }
                                
                                // Resetear reCAPTCHA
                                function resetRecaptcha() {
                                    if (typeof grecaptcha !== 'undefined') {
                                        grecaptcha.reset();
                                    }
                                    recaptchaToken = '';
                                    recaptchaVerified = false;
                                }
                                
                                // ===== VALIDACIONES EN TIEMPO REAL =====
                                
                                // Validar nombre en tiempo real (SOLO LETRAS)
                                function validarNombreEnTiempoReal() {
                                    const nombreInput = document.getElementById('nombre');
                                    const nombreError = document.getElementById('nombreError');
                                    const nombreRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]*$/;
                                    
                                    const valor = nombreInput.value;
                                    
                                    if (valor && !nombreRegex.test(valor)) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'SOLO LETRAS Y ESPACIOS';
                                        nombreError.style.display = 'block';
                                    } else if (valor.length > 40) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'Máximo 40 caracteres';
                                        nombreError.style.display = 'block';
                                    } else if (valor && nombreRegex.test(valor)) {
                                        nombreInput.classList.remove('error');
                                        nombreInput.classList.add('success');
                                        nombreError.style.display = 'none';
                                    } else {
                                        nombreInput.classList.remove('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.style.display = 'none';
                                    }
                                }
                                
                                // Validar teléfono en tiempo real (SOLO NÚMEROS)
                                function validarTelefonoEnTiempoReal() {
                                    const telefonoInput = document.getElementById('telefono');
                                    const telefonoError = document.getElementById('telefonoError');
                                    
                                    let valor = telefonoInput.value;
                                    
                                    // Remover todo excepto números
                                    valor = valor.replace(/\\D/g, '');
                                    telefonoInput.value = valor;
                                    
                                    if (valor && !/^\\d+$/.test(valor)) {
                                        telefonoInput.classList.add('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.textContent = 'SOLO NÚMEROS';
                                        telefonoError.style.display = 'block';
                                    } else if (valor.length > 10) {
                                        telefonoInput.value = valor.slice(0, 10);
                                        telefonoInput.classList.add('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.textContent = 'Máximo 10 dígitos';
                                        telefonoError.style.display = 'block';
                                    } else if (valor.length === 10) {
                                        telefonoInput.classList.remove('error');
                                        telefonoInput.classList.add('success');
                                        telefonoError.style.display = 'none';
                                    } else if (valor) {
                                        telefonoInput.classList.remove('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.style.display = 'none';
                                    } else {
                                        telefonoInput.classList.remove('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.style.display = 'none';
                                    }
                                }
                                
                                // Validar mensaje en tiempo real (SOLO LETRAS, MÁXIMO 20 PALABRAS)
                                function validarMensajeEnTiempoReal() {
                                    const mensajeTextarea = document.getElementById('mensaje');
                                    const mensajeError = document.getElementById('mensajeError');
                                    const contadorPalabras = document.getElementById('contadorPalabras');
                                    
                                    const valor = mensajeTextarea.value;
                                    const soloLetrasRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]*$/;
                                    
                                    // Contar palabras
                                    const palabras = valor.trim().split(/\\s+/).filter(word => word.length > 0);
                                    if (contadorPalabras) {
                                        contadorPalabras.textContent = 'Palabras: ' + palabras.length + '/20';
                                    }
                                    
                                    if (valor && !soloLetrasRegex.test(valor)) {
                                        mensajeTextarea.classList.add('error');
                                        mensajeTextarea.classList.remove('success');
                                        mensajeError.textContent = 'SOLO LETRAS Y ESPACIOS';
                                        mensajeError.style.display = 'block';
                                        if (contadorPalabras) {
                                            contadorPalabras.style.color = '#d32f2f';
                                        }
                                    } else if (palabras.length > 20) {
                                        mensajeTextarea.classList.add('error');
                                        mensajeTextarea.classList.remove('success');
                                        mensajeError.textContent = 'Máximo 20 palabras. Actual: ' + palabras.length;
                                        mensajeError.style.display = 'block';
                                        if (contadorPalabras) {
                                            contadorPalabras.style.color = '#d32f2f';
                                        }
                                    } else if (valor && soloLetrasRegex.test(valor) && palabras.length <= 20) {
                                        mensajeTextarea.classList.remove('error');
                                        mensajeTextarea.classList.add('success');
                                        mensajeError.style.display = 'none';
                                        if (contadorPalabras) {
                                            contadorPalabras.style.color = '#757575';
                                        }
                                    } else {
                                        mensajeTextarea.classList.remove('error');
                                        mensajeTextarea.classList.remove('success');
                                        mensajeError.style.display = 'none';
                                        if (contadorPalabras) {
                                            contadorPalabras.style.color = '#757575';
                                        }
                                    }
                                }
                                
                                // ===== FUNCIÓN PRINCIPAL DE VALIDACIÓN (AL ENVIAR) =====
                                function validateForm(event) {
                                    event.preventDefault();
                                    console.log('Validando formulario...');
                                    let isValid = true;
                                    const formData = {};
                                    
                                    // ===== VALIDACIÓN DE NOMBRE (SOLO LETRAS) =====
                                    const nombreInput = document.getElementById('nombre');
                                    const nombreError = document.getElementById('nombreError');
                                    const nombreRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]+$/;
                                    
                                    if (!nombreInput.value.trim()) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'Este campo es obligatorio';
                                        nombreError.style.display = 'block';
                                        isValid = false;
                                    } else if (!nombreRegex.test(nombreInput.value)) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'SOLO LETRAS (no números ni caracteres especiales)';
                                        nombreError.style.display = 'block';
                                        isValid = false;
                                    } else if (nombreInput.value.trim().length > 40) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'Máximo 40 caracteres';
                                        nombreError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        nombreInput.classList.remove('error');
                                        nombreInput.classList.add('success');
                                        nombreError.style.display = 'none';
                                        formData.nombre = nombreInput.value.trim();
                                    }
                                    
                                    // ===== VALIDACIÓN DE EMAIL =====
                                    const emailInput = document.getElementById('email');
                                    const emailError = document.getElementById('emailError');
                                    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$/;
                                    
                                    if (!emailInput.value.trim()) {
                                        emailInput.classList.add('error');
                                        emailInput.classList.remove('success');
                                        emailError.textContent = 'Este campo es obligatorio';
                                        emailError.style.display = 'block';
                                        isValid = false;
                                    } else if (!emailRegex.test(emailInput.value)) {
                                        emailInput.classList.add('error');
                                        emailInput.classList.remove('success');
                                        emailError.textContent = 'Formato de email inválido (ejemplo: usuario@dominio.com)';
                                        emailError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        emailInput.classList.remove('error');
                                        emailInput.classList.add('success');
                                        emailError.style.display = 'none';
                                        formData.email = emailInput.value.trim();
                                    }
                                    
                                    // ===== VALIDACIÓN DE TELÉFONO (SOLO NÚMEROS) =====
                                    const telefonoInput = document.getElementById('telefono');
                                    const telefonoError = document.getElementById('telefonoError');
                                    
                                    if (!telefonoInput.value.trim()) {
                                        telefonoInput.classList.add('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.textContent = 'Este campo es obligatorio';
                                        telefonoError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        const telefonoLimpio = telefonoInput.value.replace(/\\D/g, '');
                                        
                                        if (telefonoLimpio.length !== 10) {
                                            telefonoInput.classList.add('error');
                                            telefonoInput.classList.remove('success');
                                            telefonoError.textContent = 'Debe tener exactamente 10 dígitos (actual: ' + telefonoLimpio.length + ')';
                                            telefonoError.style.display = 'block';
                                            isValid = false;
                                        } else if (!/^\\d+$/.test(telefonoLimpio)) {
                                            telefonoInput.classList.add('error');
                                            telefonoInput.classList.remove('success');
                                            telefonoError.textContent = 'SOLO NÚMEROS (no letras ni caracteres especiales)';
                                            telefonoError.style.display = 'block';
                                            isValid = false;
                                        } else {
                                            telefonoInput.classList.remove('error');
                                            telefonoInput.classList.add('success');
                                            telefonoError.style.display = 'none';
                                            formData.telefono = telefonoLimpio;
                                        }
                                    }
                                    
                                    // ===== VALIDACIÓN DE MENSAJE (SOLO LETRAS, MÁXIMO 20 PALABRAS) =====
                                    const mensajeTextarea = document.getElementById('mensaje');
                                    const mensajeError = document.getElementById('mensajeError');
                                    
                                    if (mensajeTextarea.value.trim()) {
                                        const mensaje = mensajeTextarea.value.trim();
                                        const palabras = mensaje.split(/\\s+/).filter(word => word.length > 0);
                                        const soloLetrasRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\\s]+$/;
                                        
                                        if (!soloLetrasRegex.test(mensaje)) {
                                            mensajeTextarea.classList.add('error');
                                            mensajeTextarea.classList.remove('success');
                                            mensajeError.textContent = 'SOLO LETRAS Y ESPACIOS (no números ni caracteres especiales)';
                                            mensajeError.style.display = 'block';
                                            isValid = false;
                                        } else if (palabras.length > 20) {
                                            mensajeTextarea.classList.add('error');
                                            mensajeTextarea.classList.remove('success');
                                            mensajeError.textContent = 'Máximo 20 palabras. Actual: ' + palabras.length + ' palabras';
                                            mensajeError.style.display = 'block';
                                            isValid = false;
                                        } else {
                                            mensajeTextarea.classList.remove('error');
                                            mensajeTextarea.classList.add('success');
                                            mensajeError.style.display = 'none';
                                            formData.mensaje = mensaje;
                                            formData.palabrasMensaje = palabras.length;
                                        }
                                    } else {
                                        mensajeTextarea.classList.remove('error');
                                        mensajeTextarea.classList.remove('success');
                                        mensajeError.style.display = 'none';
                                    }
                                    
                                    // ===== VALIDACIÓN DE reCAPTCHA =====
                                    if (!validateRecaptcha()) {
                                        isValid = false;
                                    }
                                    
                                    // ===== VALIDACIÓN DE TÉRMINOS =====
                                    const terminosCheckbox = document.getElementById('terminos');
                                    const terminosError = document.getElementById('terminosError');
                                    
                                    if (!terminosCheckbox.checked) {
                                        terminosError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        terminosError.style.display = 'none';
                                        formData.terminosAceptados = true;
                                    }
                                    
                                    formData.recaptchaToken = recaptchaToken;
                                    
                                    if (isValid) {
                                        mostrarResultados(formData);
                                        setTimeout(resetRecaptcha, 1000);
                                    } else {
                                        console.log('Formulario inválido');
                                        const primerError = document.querySelector('.form-input.error, .textarea-field.error');
                                        if (primerError) {
                                            primerError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                            primerError.focus();
                                        }
                                    }
                                    
                                    return false;
                                }
                                
                                function mostrarResultados(data) {
                                    const resultadoDiv = document.getElementById('formResultado');
                                    const resultadoContent = document.getElementById('resultadoContent');
                                    
                                    const mensajeTexto = data.mensaje ? data.mensaje : 'No proporcionado';
                                    const palabrasTexto = data.palabrasMensaje ? data.palabrasMensaje + ' palabras' : 'N/A';
                                    const terminosTexto = data.terminosAceptados ? 'Sí' : 'No';
                                    const fechaTexto = new Date().toLocaleString();
                                    
                                    let resultadoHTML = '<div class="result-item">';
                                    resultadoHTML += '<strong>Nombre:</strong> ' + data.nombre + '</div>';
                                    resultadoHTML += '<div class="result-item"><strong>Email:</strong> ' + data.email + '</div>';
                                    resultadoHTML += '<div class="result-item"><strong>Teléfono:</strong> ' + data.telefono + '</div>';
                                    resultadoHTML += '<div class="result-item"><strong>Mensaje:</strong> ' + mensajeTexto + '</div>';
                                    resultadoHTML += '<div class="result-item"><strong>Palabras en mensaje:</strong> ' + palabrasTexto + '</div>';
                                    resultadoHTML += '<div class="result-item"><strong>reCAPTCHA verificado:</strong> Sí</div>';
                                    resultadoHTML += '<div class="result-item"><strong>Términos aceptados:</strong> ' + terminosTexto + '</div>';
                                    resultadoHTML += '<div class="result-item"><strong>Fecha de envío:</strong> ' + fechaTexto + '</div>';
                                    
                                    resultadoContent.innerHTML = resultadoHTML;
                                    resultadoDiv.style.display = 'block';
                                    resultadoDiv.scrollIntoView({ behavior: 'smooth' });
                                    
                                    resultadoDiv.style.animation = 'pulse 0.5s ease-in-out';
                                    setTimeout(() => {
                                        resultadoDiv.style.animation = '';
                                    }, 500);
                                }
                                
                                function resetForm() {
                                    const form = document.getElementById('formularioBasico');
                                    form.reset();
                                    
                                    const inputs = form.querySelectorAll('.form-input, .textarea-field');
                                    for (let i = 0; i < inputs.length; i++) {
                                        inputs[i].classList.remove('error', 'success');
                                    }
                                    
                                    const errors = form.querySelectorAll('.error-message');
                                    for (let i = 0; i < errors.length; i++) {
                                        errors[i].style.display = 'none';
                                    }
                                    
                                    resetRecaptcha();
                                    
                                    const resultadoDiv = document.getElementById('formResultado');
                                    resultadoDiv.style.display = 'none';
                                    
                                    return false;
                                }
                                
                                // Inicializar cuando se carga la página
                                document.addEventListener('DOMContentLoaded', function() {
                                    console.log('DOM cargado');
                                    
                                    // Agregar estilos para animaciones
                                    const style = document.createElement('style');
                                    style.textContent = '@keyframes pulse {0% { transform: scale(1); }50% { transform: scale(1.02); }100% { transform: scale(1); }}.result-item {margin-bottom: 8px;padding-bottom: 8px;border-bottom: 1px solid #e0e0e0;}.result-item:last-child {border-bottom: none;margin-bottom: 0;}';
                                    document.head.appendChild(style);
                                    
                                    // Crear contador de palabras para el mensaje
                                    const mensajeTextarea = document.getElementById('mensaje');
                                    const contadorPalabras = document.createElement('div');
                                    contadorPalabras.className = 'form-hint';
                                    contadorPalabras.id = 'contadorPalabras';
                                    contadorPalabras.textContent = 'Palabras: 0/20';
                                    mensajeTextarea.parentNode.insertBefore(contadorPalabras, mensajeTextarea.nextSibling);
                                    
                                    // Configurar eventos en tiempo real
                                    const nombreInput = document.getElementById('nombre');
                                    nombreInput.addEventListener('input', validarNombreEnTiempoReal);
                                    nombreInput.addEventListener('blur', validarNombreEnTiempoReal);
                                    
                                    const telefonoInput = document.getElementById('telefono');
                                    telefonoInput.addEventListener('input', validarTelefonoEnTiempoReal);
                                    telefonoInput.addEventListener('blur', validarTelefonoEnTiempoReal);
                                    
                                    mensajeTextarea.addEventListener('input', validarMensajeEnTiempoReal);
                                    mensajeTextarea.addEventListener('blur', validarMensajeEnTiempoReal);
                                    
                                    // Email también en tiempo real
                                    const emailInput = document.getElementById('email');
                                    emailInput.addEventListener('blur', function() {
                                        const emailError = document.getElementById('emailError');
                                        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$/;
                                        
                                        if (this.value && !emailRegex.test(this.value)) {
                                            this.classList.add('error');
                                            this.classList.remove('success');
                                            emailError.textContent = 'Formato de email inválido';
                                            emailError.style.display = 'block';
                                        } else if (this.value) {
                                            this.classList.remove('error');
                                            this.classList.add('success');
                                            emailError.style.display = 'none';
                                        } else {
                                            this.classList.remove('error');
                                            this.classList.remove('success');
                                            emailError.style.display = 'none';
                                        }
                                    });
                                    
                                    // Verificar reCAPTCHA
                                    setTimeout(function() {
                                        console.log('reCAPTCHA disponible:', typeof grecaptcha !== 'undefined');
                                    }, 1000);
                                    
                                    // Nota: El carrusel ahora funciona automáticamente con CSS puro
                                    console.log('Carrusel automático configurado con CSS puro');
                                });
                                """
                            }
                        }
                    }
                    body {
                        div("main-container") {
                            // ===== HERO SECTION MINIMALISTA =====
                            div("hero-container") {
                                div("header-section") {
                                    h1("main-title") { +"Sistema de Breadcrumbs" }
                                    p("subtitle") { +"Un ejemplo práctico de navegación jerárquica implementado con tecnología moderna" }
                                }

                                div("levels-container") {
                                    a(classes = "level-card", href = "/breadcrumbs") {
                                        div("level-number") { +"1" }
                                        h3("level-title") { +"Nivel 1 - Introducción" }
                                        p("level-description") { +"Conceptos básicos y fundamentos de los sistemas de breadcrumbs" }
                                    }

                                    a(classes = "level-card", href = "/breadcrumbs/detalle") {
                                        div("level-number") { +"2" }
                                        h3("level-title") { +"Nivel 2 - Detalles" }
                                        p("level-description") { +"Análisis técnico y casos de uso de navegación jerárquica" }
                                    }

                                    a(classes = "level-card", href = "/breadcrumbs/detalle/configuracion") {
                                        div("level-number") { +"3" }
                                        h3("level-title") { +"Nivel 3 - Configuración" }
                                        p("level-description") { +"Personalización y configuración avanzada del sistema" }
                                    }
                                }

                                div("features-list") {
                                    h4("features-title") { +"Características del Sistema" }
                                    div("features-grid") {
                                        div("feature-item") { +"Navegación jerárquica clara" }
                                        div("feature-item") { +"Diseño responsive adaptativo" }
                                        div("feature-item") { +"Interfaz moderna y limpia" }
                                        div("feature-item") { +"Código mantenible y escalable" }
                                    }
                                }

                                a(href = "/breadcrumbs", classes = "start-button") {
                                    +"Iniciar Navegación"
                                }
                            }

                            // ===== CARRUSEL DE IMÁGENES CON ANIMACIÓN AUTOMÁTICA CSS =====
                            div("carousel-section") {
                                div("carousel-header") {
                                    h3("carousel-title") { +"Galería de Ejemplos" }
                                    p("carousel-subtitle") { +"Explora diferentes implementaciones de breadcrumbs" }
                                }

                                div("carousel-container") {
                                    // El carrusel ahora funciona automáticamente con CSS animations
                                    div("carousel-track") {
                                        // Slide 1
                                        div("carousel-slide") {
                                            img(src = "https://images.unsplash.com/photo-1551650975-87deedd944c3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                classes = "carousel-image") {
                                                attributes["alt"] = "Ejemplo de breadcrumbs en e-commerce"
                                            }
                                            div("carousel-caption") {
                                                h3 { +"E-commerce" }
                                                p { +"Breadcrumbs en tiendas online mostrando categorías de productos" }
                                            }
                                        }

                                        // Slide 2
                                        div("carousel-slide") {
                                            img(src = "https://images.unsplash.com/photo-1555099962-4199c345e5dd?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                classes = "carousel-image") {
                                                attributes["alt"] = "Breadcrumbs en documentación técnica"
                                            }
                                            div("carousel-caption") {
                                                h3 { +"Documentación" }
                                                p { +"Navegación jerárquica en manuales y documentación técnica" }
                                            }
                                        }

                                        // Slide 3
                                        div("carousel-slide") {
                                            img(src = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                classes = "carousel-image") {
                                                attributes["alt"] = "Breadcrumbs en blogs"
                                            }
                                            div("carousel-caption") {
                                                h3 { +"Blogs y Noticias" }
                                                p { +"Estructura de categorías y etiquetas en publicaciones" }
                                            }
                                        }

                                        // Slide 4
                                        div("carousel-slide") {
                                            img(src = "https://images.unsplash.com/photo-1555949963-aa79dcee981c?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
                                                classes = "carousel-image") {
                                                attributes["alt"] = "Breadcrumbs en dashboards"
                                            }
                                            div("carousel-caption") {
                                                h3 { +"Dashboards" }
                                                p { +"Navegación en paneles de control y aplicaciones empresariales" }
                                            }
                                        }
                                    }

                                    // Controles (opcionales, solo aparecen al hacer hover)
                                    div("carousel-controls") {
                                        // Los controles ahora son puramente decorativos o podrían añadirse con JS simple si se desea
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

                            // ===== FORMULARIO BÁSICO MINIMALISTA =====
                            div("form-container") {
                                div("form-header") {
                                    h2("form-title") { +"Formulario de Contacto" }
                                    p("form-subtitle") { +"Complete el formulario con sus datos básicos" }
                                }

                                div("form-wrapper") {
                                    form {
                                        attributes["id"] = "formularioBasico"
                                        attributes["onsubmit"] = "return validateForm(event);"

                                        // Campo 1: Nombre completo
                                        div("form-group") {
                                            label(classes = "form-label required") {
                                                attributes["for"] = "nombre"
                                                +"Nombre completo"
                                            }
                                            input(type = InputType.text, classes = "form-input") {
                                                attributes["id"] = "nombre"
                                                attributes["placeholder"] = "Ingrese su nombre completo (solo letras)"
                                                attributes["maxlength"] = "40"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "nombreError"
                                                +"Solo letras, máximo 40 caracteres"
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
                                                attributes["placeholder"] = "ejemplo@correo.com"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "emailError"
                                                +"Ingrese un email válido"
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
                                                attributes["placeholder"] = "1234567890"
                                                attributes["maxlength"] = "10"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "telefonoError"
                                                +"Ingrese 10 dígitos numéricos"
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
                                                attributes["placeholder"] = "Ingrese su mensaje (máximo 20 palabras, solo letras)"
                                                attributes["rows"] = "4"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "mensajeError"
                                                +"Máximo 20 palabras, solo letras"
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
                                                    attributes["required"] = "true"
                                                }
                                                label {
                                                    attributes["for"] = "terminos"
                                                    span("form-label required") {
                                                        +"Acepto los términos y condiciones"
                                                    }
                                                }
                                            }
                                            div("error-message") {
                                                attributes["id"] = "terminosError"
                                                +"Debe aceptar los términos y condiciones"
                                            }
                                        }

                                        // Botones del formulario
                                        div("form-buttons") {
                                            button(type = ButtonType.submit, classes = "submit-btn") {
                                                +"Enviar Formulario"
                                            }
                                            button(type = ButtonType.button, classes = "reset-btn") {
                                                attributes["onclick"] = "return resetForm();"
                                                +"Limpiar Formulario"
                                            }
                                        }

                                        // Resultados del formulario
                                        div("form-result") {
                                            attributes["id"] = "formResultado"
                                            h3("result-title") { +"Datos Enviados" }
                                            div("result-content") {
                                                attributes["id"] = "resultadoContent"
                                                +"Los datos del formulario aparecerán aquí después de enviarlos."
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
                        }
                    }
                }
            }

            // ========== NIVEL 1 CON DISEÑO MINIMALISTA ==========
            get("/breadcrumbs") {
                println("Sirviendo Nivel 1")
                call.respondHtml {
                    head {
                        title { +"Nivel 1 - Introducción" }
                        style { unsafe { +getMinimalistCSS() } }
                    }
                    body {
                        div("page-wrapper") {
                            // Header minimalista
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Breadcrumbs" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "#", classes = "nav-item active") { +"Nivel 1" }
                                        a(href = "/breadcrumbs/detalle", classes = "nav-item") { +"Nivel 2" }
                                        a(href = "/breadcrumbs/detalle/configuracion", classes = "nav-item") { +"Nivel 3" }
                                    }
                                }
                            }

                            // Breadcrumbs minimalistas
                            nav("breadcrumb-nav") {
                                ol("breadcrumb-list") {
                                    li("breadcrumb-item") {
                                        a(href = "/", classes = "breadcrumb-link") { +"Inicio" }
                                    }
                                    li("breadcrumb-separator") { +"/" }
                                    li("breadcrumb-item active") {
                                        span("breadcrumb-text") { +"Nivel 1" }
                                    }
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
                                    p("footer-text") { +"© 2024 Sistema de Breadcrumbs - Ejemplo Educativo" }
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

            // ========== NIVEL 2 CON DISEÑO MINIMALISTA ==========
            get("/breadcrumbs/detalle") {
                println("Sirviendo Nivel 2")
                call.respondHtml {
                    head {
                        title { +"Nivel 2 - Detalles" }
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
                                        a(href = "/breadcrumbs", classes = "nav-item") { +"Nivel 1" }
                                        a(href = "#", classes = "nav-item active") { +"Nivel 2" }
                                        a(href = "/breadcrumbs/detalle/configuracion", classes = "nav-item") { +"Nivel 3" }
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
                                            div("type-card") {
                                                h3("type-title") { +"Breadcrumbs Estáticos" }
                                                ul("type-features") {
                                                    li { +"Definidos manualmente en cada página" }
                                                    li { +"Ideales para sitios con estructura fija" }
                                                    li { +"Fáciles de implementar y mantener" }
                                                    li { +"Rendimiento óptimo sin lógica compleja" }
                                                }
                                                div("type-tag static") { +"Recomendado para sitios pequeños" }
                                            }

                                            div("type-card") {
                                                h3("type-title") { +"Breadcrumbs Dinámicos" }
                                                ul("type-features") {
                                                    li { +"Generados automáticamente según la estructura" }
                                                    li { +"Escalables para sitios de gran tamaño" }
                                                    li { +"Se adaptan a cambios en la jerarquía" }
                                                    li { +"Requieren lógica de programación avanzada" }
                                                }
                                                div("type-tag dynamic") { +"Ideal para aplicaciones complejas" }
                                            }

                                            div("type-card") {
                                                h3("type-title") { +"Breadcrumbs Basados en Rutas" }
                                                ul("type-features") {
                                                    li { +"Derivados directamente de la URL actual" }
                                                    li { +"Muy utilizados en aplicaciones SPA" }
                                                    li { +"Flexibles y adaptables" }
                                                    li { +"Dependen de la estructura de enrutamiento" }
                                                }
                                                div("type-tag path") { +"Común en frameworks modernos" }
                                            }
                                        }

                                        div("code-example") {
                                            h3("code-title") { +"Ejemplo de Implementación" }
                                            div("code-block") {
                                                pre {
                                                    code {
                                                        +"""<!-- Estructura HTML de breadcrumbs -->
<nav class="breadcrumb-nav">
  <ol class="breadcrumb-list">
    <li class="breadcrumb-item">
      <a href="/" class="breadcrumb-link">Inicio</a>
    </li>
    <li class="breadcrumb-separator">/</li>
    <li class="breadcrumb-item">
      <a href="/productos" class="breadcrumb-link">Productos</a>
    </li>
    <li class="breadcrumb-separator">/</li>
    <li class="breadcrumb-item active">
      <span class="breadcrumb-text">Detalle del Producto</span>
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
                                    p("footer-text") { +"© 2024 Sistema de Breadcrumbs - Ejemplo Educativo" }
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

            // ========== NIVEL 3 CON DISEÑO MINIMALISTA ==========
            get("/breadcrumbs/detalle/configuracion") {
                println("Intentando servir Nivel 3")

                try {
                    call.respondHtml {
                        head {
                            title { +"Nivel 3 - Configuración" }
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
                                            a(href = "/breadcrumbs", classes = "nav-item") { +"Nivel 1" }
                                            a(href = "/breadcrumbs/detalle", classes = "nav-item") { +"Nivel 2" }
                                            a(href = "#", classes = "nav-item active") { +"Nivel 3" }
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
                                                div("config-section") {
                                                    h3("config-title") { +"Personalización Visual" }
                                                    div("config-options") {
                                                        div("config-group") {
                                                            label("config-label") {
                                                                attributes["for"] = "separator"
                                                                +"Separador de Niveles:"
                                                            }
                                                            select {
                                                                attributes["id"] = "separator"
                                                                classes = setOf("config-select")
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
                                                            label("config-label") {
                                                                attributes["for"] = "primaryColor"
                                                                +"Color Primario:"
                                                            }
                                                            div("color-picker") {
                                                                input(type = InputType.color) {
                                                                    attributes["id"] = "primaryColor"
                                                                    attributes["value"] = "#212121"
                                                                }
                                                                span("color-value") { +"#212121" }
                                                            }
                                                        }

                                                        div("config-group") {
                                                            label("config-label") {
                                                                attributes["for"] = "fontSize"
                                                                +"Tamaño de Fuente:"
                                                            }
                                                            div("slider-container") {
                                                                input(type = InputType.range) {
                                                                    attributes["id"] = "fontSize"
                                                                    attributes["min"] = "12"
                                                                    attributes["max"] = "20"
                                                                    attributes["value"] = "14"
                                                                }
                                                                span("slider-value") { +"14px" }
                                                            }
                                                        }
                                                    }
                                                }

                                                div("config-section") {
                                                    h3("config-title") { +"Opciones de Comportamiento" }
                                                    div("config-switches") {
                                                        div("switch-group") {
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "animations"
                                                                attributes["checked"] = "checked"
                                                            }
                                                            label {
                                                                attributes["for"] = "animations"
                                                                span("switch-label") { +"Habilitar animaciones" }
                                                            }
                                                        }

                                                        div("switch-group") {
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "responsive"
                                                                attributes["checked"] = "checked"
                                                            }
                                                            label {
                                                                attributes["for"] = "responsive"
                                                                span("switch-label") { +"Diseño responsive" }
                                                            }
                                                        }

                                                        div("switch-group") {
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "icons"
                                                                attributes["checked"] = "checked"
                                                            }
                                                            label {
                                                                attributes["for"] = "icons"
                                                                span("switch-label") { +"Mostrar indicadores visuales" }
                                                            }
                                                        }

                                                        div("switch-group") {
                                                            input(type = InputType.checkBox) {
                                                                attributes["id"] = "truncate"
                                                                attributes["checked"] = "checked"
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
                                                div("banner-content") {
                                                    h3("banner-title") { +"Navegación Completada Exitosamente" }
                                                    p("banner-text") {
                                                        +"Has demostrado el funcionamiento completo de un sistema de breadcrumbs implementado con las mejores prácticas de desarrollo web moderno."
                                                    }
                                                    div("completion-stats") {
                                                        div("stat-item") {
                                                            span("stat-number") { +"3" }
                                                            span("stat-label") { +"Niveles Navegados" }
                                                        }
                                                        div("stat-item") {
                                                            span("stat-number") { +"100%" }
                                                            span("stat-label") { +"Funcionalidad Comprobada" }
                                                        }
                                                        div("stat-item") {
                                                            span("stat-number") { +"✓" }
                                                            span("stat-label") { +"Sistema Validado" }
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
                                        p("footer-text") { +"© 2024 Sistema de Breadcrumbs - Ejemplo Educativo" }
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

            // Rutas de error de prueba (se mantienen igual)
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

// Función CSS minimalista para las páginas de contenido (se mantiene igual)
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
    
    /* ===== TIPOS DE IMPLEMENTACIÓN ===== */
    .implementation-types {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 20px;
        margin: 24px 0;
    }
    
    .type-card {
        background: white;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 20px;
        transition: border-color 0.2s;
    }
    
    .type-card:hover {
        border-color: #212121;
    }
    
    .type-title {
        font-size: 1.125rem;
        font-weight: 500;
        color: #000000;
        margin-bottom: 12px;
    }
    
    .type-features {
        list-style: none;
        margin-bottom: 12px;
    }
    
    .type-features li {
        padding: 6px 0;
        border-bottom: 1px solid #eeeeee;
        color: #616161;
        font-size: 0.9rem;
    }
    
    .type-features li:last-child {
        border-bottom: none;
    }
    
    .type-tag {
        display: inline-block;
        padding: 4px 10px;
        border-radius: 4px;
        font-size: 0.7rem;
        font-weight: 400;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }
    
    .type-tag.static {
        background: #f5f5f5;
        color: #424242;
    }
    
    .type-tag.dynamic {
        background: #eeeeee;
        color: #212121;
    }
    
    .type-tag.path {
        background: #fafafa;
        color: #616161;
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
    
    /* ===== PANEL DE CONFIGURACIÓN ===== */
    .configuration-panel {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 24px;
        margin: 24px 0;
    }
    
    .config-section {
        background: #fafafa;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 20px;
    }
    
    .config-title {
        font-size: 1.05rem;
        font-weight: 500;
        color: #000000;
        margin-bottom: 16px;
        padding-bottom: 10px;
        border-bottom: 1px solid #e0e0e0;
    }
    
    .config-options {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }
    
    .config-group {
        display: flex;
        flex-direction: column;
        gap: 6px;
    }
    
    .config-label {
        font-weight: 400;
        color: #616161;
        font-size: 0.9rem;
    }
    
    .config-select {
        padding: 8px 10px;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        background: white;
        font-size: 0.9rem;
        color: #212121;
        transition: border-color 0.2s;
    }
    
    .config-select:focus {
        outline: none;
        border-color: #212121;
    }
    
    .color-picker {
        display: flex;
        align-items: center;
        gap: 10px;
    }
    
    .color-picker input[type="color"] {
        width: 50px;
        height: 35px;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        cursor: pointer;
    }
    
    .color-value {
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.85rem;
        color: #616161;
        background: white;
        padding: 6px 10px;
        border: 1px solid #bdbdbd;
        border-radius: 4px;
        min-width: 80px;
    }
    
    .slider-container {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    
    .slider-container input[type="range"] {
        flex: 1;
        height: 4px;
        border-radius: 2px;
        background: #e0e0e0;
        outline: none;
    }
    
    .slider-value {
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.85rem;
        color: #616161;
        min-width: 40px;
    }
    
    .config-switches {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }
    
    .switch-group {
        display: flex;
        align-items: center;
        gap: 10px;
    }
    
    .switch-group input[type="checkbox"] {
        width: 18px;
        height: 18px;
        border: 1px solid #bdbdbd;
        border-radius: 3px;
        cursor: pointer;
    }
    
    .switch-label {
        font-weight: 400;
        color: #616161;
        font-size: 0.9rem;
        cursor: pointer;
    }
    
    /* ===== BANNER DE COMPLETACIÓN ===== */
    .completion-banner {
        background: #fafafa;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        padding: 24px;
        margin: 24px 0;
        text-align: center;
    }
    
    .banner-title {
        font-size: 1.25rem;
        font-weight: 400;
        color: #000000;
        margin-bottom: 12px;
    }
    
    .banner-text {
        font-size: 1.05rem;
        color: #616161;
        max-width: 600px;
        margin: 0 auto 20px;
        line-height: 1.6;
    }
    
    .completion-stats {
        display: flex;
        justify-content: center;
        gap: 24px;
        flex-wrap: wrap;
    }
    
    .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    
    .stat-number {
        font-size: 1.5rem;
        font-weight: 300;
        color: #000000;
        margin-bottom: 4px;
    }
    
    .stat-label {
        font-size: 0.85rem;
        color: #616161;
        font-weight: 400;
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

// Configuración del manejo de errores (se mantiene igual)
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
                                    +"Ir al Nivel 1"
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

// Función CSS minimalista para páginas de error (se mantiene igual)
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