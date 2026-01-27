// Main.kt - BREADCRUMBS con DISEÑO PROFESIONAL + FORMULARIO BÁSICO CON reCAPTCHA v2
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
        host = "0.0.0.0"  // ← CRÍTICO para Docker
    ) {
        // Configurar manejo de errores
        configureStatusPages()

        routing {
            // Interceptor para debug y manejo de errores
            intercept(ApplicationCallPipeline.Call) {
                try {
                    val requestUri = call.request.uri
                    val method = call.request.httpMethod.value
                    val startTime = System.currentTimeMillis()
                    val clientIp = call.request.origin.remoteHost

                    println("[$clientIp] $method $requestUri")

                    // Validación básica de rutas
                    if (requestUri.contains("..") || requestUri.contains("//")) {
                        call.respond(HttpStatusCode.BadRequest, "Ruta inválida")
                        return@intercept
                    }

                    // Procesar la llamada
                    proceed()

                    val duration = System.currentTimeMillis() - startTime
                    val status = call.response.status()?.value ?: 200

                    println("[$clientIp] $method $requestUri - $status (${duration}ms)")

                } catch (e: Exception) {
                    println("ERROR en interceptor: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error interno del servidor")
                }
            }

            // ========== PÁGINA PRINCIPAL CON DISEÑO Y FORMULARIO ==========
            get("/") {
                println("Sirviendo página principal")
                call.respondHtml {
                    head {
                        title { +"Sistema de Breadcrumbs" }
                        style {
                            unsafe {
                                +"""
                                * {
                                    margin: 0;
                                    padding: 0;
                                    box-sizing: border-box;
                                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                                }
                                
                                body {
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    min-height: 100vh;
                                    padding: 40px 20px;
                                }
                                
                                .main-container {
                                    max-width: 1200px;
                                    margin: 0 auto;
                                }
                                
                                /* ===== HERO SECTION ===== */
                                .hero-container {
                                    background: rgba(255, 255, 255, 0.98);
                                    backdrop-filter: blur(20px);
                                    border-radius: 24px;
                                    padding: 60px;
                                    margin-bottom: 40px;
                                    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
                                    border: 1px solid rgba(255, 255, 255, 0.3);
                                }
                                
                                .header-section {
                                    text-align: center;
                                    margin-bottom: 50px;
                                    padding-bottom: 30px;
                                    border-bottom: 2px solid rgba(102, 126, 234, 0.1);
                                }
                                
                                .main-title {
                                    color: #1a202c;
                                    font-size: 3rem;
                                    font-weight: 800;
                                    margin-bottom: 16px;
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    -webkit-background-clip: text;
                                    -webkit-text-fill-color: transparent;
                                    letter-spacing: -0.5px;
                                }
                                
                                .subtitle {
                                    color: #4a5568;
                                    font-size: 1.25rem;
                                    font-weight: 400;
                                    line-height: 1.6;
                                    max-width: 600px;
                                    margin: 0 auto;
                                }
                                
                                .levels-container {
                                    display: grid;
                                    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                                    gap: 30px;
                                    margin: 40px 0;
                                }
                                
                                .level-card {
                                    background: white;
                                    border-radius: 16px;
                                    padding: 35px;
                                    text-align: center;
                                    text-decoration: none;
                                    color: #2d3748;
                                    border: 2px solid transparent;
                                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                                    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
                                    position: relative;
                                    overflow: hidden;
                                }
                                
                                .level-card::before {
                                    content: '';
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    right: 0;
                                    height: 4px;
                                    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
                                }
                                
                                .level-card:hover {
                                    transform: translateY(-8px);
                                    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
                                    border-color: #667eea;
                                }
                                
                                .level-number {
                                    display: inline-block;
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    color: white;
                                    width: 50px;
                                    height: 50px;
                                    border-radius: 50%;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 1.5rem;
                                    font-weight: 700;
                                    margin: 0 auto 20px;
                                    box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
                                }
                                
                                .level-title {
                                    font-size: 1.5rem;
                                    font-weight: 700;
                                    margin-bottom: 12px;
                                    color: #1a202c;
                                }
                                
                                .level-description {
                                    color: #718096;
                                    font-size: 1rem;
                                    line-height: 1.6;
                                }
                                
                                .start-button {
                                    display: inline-flex;
                                    align-items: center;
                                    justify-content: center;
                                    padding: 16px 40px;
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    color: white;
                                    text-decoration: none;
                                    border-radius: 12px;
                                    font-size: 1.125rem;
                                    font-weight: 600;
                                    margin-top: 30px;
                                    transition: all 0.3s;
                                    box-shadow: 0 12px 24px rgba(102, 126, 234, 0.3);
                                    border: none;
                                    cursor: pointer;
                                }
                                
                                .start-button:hover {
                                    transform: translateY(-3px);
                                    box-shadow: 0 16px 32px rgba(102, 126, 234, 0.4);
                                }
                                
                                .features-list {
                                    background: rgba(102, 126, 234, 0.05);
                                    border-radius: 12px;
                                    padding: 25px;
                                    margin-top: 40px;
                                    border: 1px solid rgba(102, 126, 234, 0.1);
                                }
                                
                                .features-title {
                                    font-size: 1.25rem;
                                    font-weight: 600;
                                    color: #1a202c;
                                    margin-bottom: 16px;
                                }
                                
                                .features-grid {
                                    display: grid;
                                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                                    gap: 16px;
                                }
                                
                                .feature-item {
                                    color: #4a5568;
                                    font-size: 0.95rem;
                                    display: flex;
                                    align-items: center;
                                    gap: 10px;
                                }
                                
                                .feature-item::before {
                                    content: '✓';
                                    color: #667eea;
                                    font-weight: bold;
                                }
                                
                                /* ===== FORMULARIO BÁSICO SECTION ===== */
                                .form-container {
                                    background: rgba(255, 255, 255, 0.98);
                                    backdrop-filter: blur(20px);
                                    border-radius: 24px;
                                    padding: 60px;
                                    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
                                    border: 1px solid rgba(255, 255, 255, 0.3);
                                }
                                
                                .form-header {
                                    text-align: center;
                                    margin-bottom: 40px;
                                }
                                
                                .form-title {
                                    font-size: 2rem;
                                    font-weight: 700;
                                    color: #1a202c;
                                    margin-bottom: 12px;
                                    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
                                    -webkit-background-clip: text;
                                    -webkit-text-fill-color: transparent;
                                }
                                
                                .form-subtitle {
                                    color: #718096;
                                    font-size: 1.125rem;
                                    max-width: 600px;
                                    margin: 0 auto;
                                }
                                
                                .form-wrapper {
                                    max-width: 600px;
                                    margin: 0 auto;
                                }
                                
                                .form-group {
                                    margin-bottom: 24px;
                                }
                                
                                .form-label {
                                    display: block;
                                    margin-bottom: 8px;
                                    font-weight: 600;
                                    color: #2d3748;
                                    font-size: 0.95rem;
                                }
                                
                                .required::after {
                                    content: ' *';
                                    color: #e53e3e;
                                }
                                
                                .form-input {
                                    width: 100%;
                                    padding: 14px 16px;
                                    border: 2px solid #e2e8f0;
                                    border-radius: 8px;
                                    font-size: 1rem;
                                    color: #2d3748;
                                    background: white;
                                    transition: all 0.2s;
                                }
                                
                                .form-input:focus {
                                    outline: none;
                                    border-color: #667eea;
                                    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                                }
                                
                                .form-input.error {
                                    border-color: #e53e3e;
                                }
                                
                                .form-input.success {
                                    border-color: #48bb78;
                                }
                                
                                .form-hint {
                                    font-size: 0.875rem;
                                    color: #718096;
                                    margin-top: 6px;
                                    display: flex;
                                    align-items: center;
                                    gap: 6px;
                                }
                                
                                .error-message {
                                    color: #e53e3e;
                                    font-size: 0.875rem;
                                    margin-top: 6px;
                                    display: none;
                                }
                                
                                .textarea-field {
                                    width: 100%;
                                    padding: 14px 16px;
                                    border: 2px solid #e2e8f0;
                                    border-radius: 8px;
                                    font-size: 1rem;
                                    color: #2d3748;
                                    background: white;
                                    resize: vertical;
                                    min-height: 120px;
                                    font-family: inherit;
                                }
                                
                                .textarea-field:focus {
                                    outline: none;
                                    border-color: #667eea;
                                    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                                }
                                
                                /* ===== ESTILOS reCAPTCHA ===== */
                                .recaptcha-container {
                                    background: #f8fafc;
                                    border: 1px solid #e2e8f0;
                                    border-radius: 8px;
                                    padding: 20px;
                                    margin-bottom: 16px;
                                }
                                
                                .recaptcha-badge {
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    color: white;
                                    padding: 12px 16px;
                                    border-radius: 6px;
                                    margin-bottom: 16px;
                                    text-align: center;
                                    font-size: 0.9rem;
                                    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
                                }
                                
                                .recaptcha-badge a {
                                    color: #d6bcfa;
                                    text-decoration: underline;
                                }
                                
                                .recaptcha-badge a:hover {
                                    color: white;
                                }
                                
                                .g-recaptcha {
                                    margin: 16px 0;
                                    min-height: 78px;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                }
                                
                                .recaptcha-loading {
                                    display: flex;
                                    flex-direction: column;
                                    align-items: center;
                                    justify-content: center;
                                    padding: 20px;
                                    color: #4a5568;
                                }
                                
                                .recaptcha-spinner {
                                    width: 40px;
                                    height: 40px;
                                    border: 3px solid #e2e8f0;
                                    border-top: 3px solid #667eea;
                                    border-radius: 50%;
                                    animation: spin 1s linear infinite;
                                    margin-bottom: 12px;
                                }
                                
                                @keyframes spin {
                                    0% { transform: rotate(0deg); }
                                    100% { transform: rotate(360deg); }
                                }
                                
                                .grecaptcha-badge {
                                    visibility: visible !important;
                                }
                                
                                .g-recaptcha iframe {
                                    border-radius: 8px;
                                    border: 1px solid #cbd5e0 !important;
                                }
                                
                                .form-buttons {
                                    display: flex;
                                    gap: 16px;
                                    margin-top: 32px;
                                }
                                
                                .submit-btn {
                                    flex: 1;
                                    padding: 16px;
                                    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
                                    color: white;
                                    border: none;
                                    border-radius: 8px;
                                    font-size: 1.125rem;
                                    font-weight: 600;
                                    cursor: pointer;
                                    transition: all 0.3s;
                                    box-shadow: 0 8px 20px rgba(67, 233, 123, 0.3);
                                }
                                
                                .submit-btn:hover {
                                    transform: translateY(-2px);
                                    box-shadow: 0 12px 24px rgba(67, 233, 123, 0.4);
                                }
                                
                                .reset-btn {
                                    flex: 1;
                                    padding: 16px;
                                    background: #e2e8f0;
                                    color: #4a5568;
                                    border: none;
                                    border-radius: 8px;
                                    font-size: 1.125rem;
                                    font-weight: 600;
                                    cursor: pointer;
                                    transition: all 0.3s;
                                }
                                
                                .reset-btn:hover {
                                    background: #cbd5e0;
                                }
                                
                                .form-footer {
                                    text-align: center;
                                    margin-top: 32px;
                                    padding-top: 24px;
                                    border-top: 1px solid #e2e8f0;
                                    color: #718096;
                                    font-size: 0.875rem;
                                }
                                
                                .form-result {
                                    margin-top: 32px;
                                    padding: 24px;
                                    background: rgba(102, 126, 234, 0.05);
                                    border-radius: 12px;
                                    border: 1px solid rgba(102, 126, 234, 0.1);
                                    display: none;
                                }
                                
                                .result-title {
                                    font-size: 1.125rem;
                                    font-weight: 600;
                                    color: #2d3748;
                                    margin-bottom: 12px;
                                }
                                
                                .result-content {
                                    color: #4a5568;
                                    font-family: 'SF Mono', Monaco, monospace;
                                    background: white;
                                    padding: 16px;
                                    border-radius: 8px;
                                    border: 1px solid #e2e8f0;
                                    overflow-x: auto;
                                }
                                
                                @media (max-width: 768px) {
                                    .hero-container, .form-container {
                                        padding: 40px 25px;
                                    }
                                    
                                    .main-title {
                                        font-size: 2.25rem;
                                    }
                                    
                                    .levels-container {
                                        grid-template-columns: 1fr;
                                    }
                                    
                                    .form-title {
                                        font-size: 1.75rem;
                                    }
                                    
                                    .form-buttons {
                                        flex-direction: column;
                                    }
                                }
                                """
                            }
                        }
                        script {
                            unsafe {
                                +"""
                                // Variable para almacenar la respuesta del reCAPTCHA
                                let recaptchaToken = '';
                                let recaptchaVerified = false;
                                
                                // Función para cargar reCAPTCHA dinámicamente
                                function loadRecaptcha() {
                                    const scriptId = 'recaptcha-script';
                                    
                                    // Evitar cargar múltiples veces
                                    if (document.getElementById(scriptId)) {
                                        console.log('reCAPTCHA ya cargado');
                                        return;
                                    }
                                    
                                    // Mostrar estado de carga
                                    const recaptchaDiv = document.querySelector('.g-recaptcha');
                                    if (recaptchaDiv) {
                                        recaptchaDiv.innerHTML = \`
                                            <div class="recaptcha-loading">
                                                <div class="recaptcha-spinner"></div>
                                                <p>Cargando verificación de seguridad...</p>
                                            </div>
                                        \`;
                                    }
                                    
                                    // Cargar script de reCAPTCHA
                                    const script = document.createElement('script');
                                    script.id = scriptId;
                                    script.src = 'https://www.google.com/recaptcha/api.js';
                                    script.async = true;
                                    script.defer = true;
                                    document.head.appendChild(script);
                                }
                                
                                // Callback cuando reCAPTCHA se carga
                                function onRecaptchaLoaded() {
                                    console.log('reCAPTCHA cargado exitosamente');
                                    
                                    // Renderizar reCAPTCHA v2
                                    if (typeof grecaptcha !== 'undefined' && grecaptcha.render) {
                                        const recaptchaDiv = document.querySelector('.g-recaptcha');
                                        if (recaptchaDiv) {
                                            const widgetId = grecaptcha.render(recaptchaDiv, {
                                                'sitekey': '6LfVDVcsAAAAAErTmnJNGjMvB19ND5u5wN9NKGde',
                                                'callback': onRecaptchaSuccess,
                                                'expired-callback': onRecaptchaExpired,
                                                'error-callback': onRecaptchaError,
                                                'theme': 'light',
                                                'size': 'normal'
                                            });
                                            
                                            // Guardar widget ID para futuras referencias
                                            recaptchaDiv.dataset.widgetId = widgetId;
                                        }
                                    }
                                }
                                
                                // Callback cuando reCAPTCHA es exitoso
                                function onRecaptchaSuccess(response) {
                                    console.log('reCAPTCHA verificado exitosamente');
                                    recaptchaToken = response;
                                    recaptchaVerified = true;
                                    
                                    // Quitar error si existía
                                    document.getElementById('recaptchaError').style.display = 'none';
                                    
                                    // Mostrar feedback visual
                                    const recaptchaContainer = document.querySelector('.recaptcha-container');
                                    recaptchaContainer.style.borderColor = '#48bb78';
                                    recaptchaContainer.style.boxShadow = '0 0 0 3px rgba(72, 187, 120, 0.1)';
                                    
                                    // Actualizar badge
                                    const badge = document.querySelector('.recaptcha-badge');
                                    if (badge) {
                                        badge.innerHTML = \`
                                            <div style="display: flex; align-items: center; justify-content: center; gap: 8px;">
                                                <span style="color: #48bb78;">✓</span>
                                                <span>Verificación completada</span>
                                            </div>
                                            <small style="margin-top: 4px; display: block;">
                                                Este sitio está protegido por reCAPTCHA y se aplican la 
                                                <a href="https://policies.google.com/privacy" target="_blank" style="color: #d6bcfa;">Política de privacidad</a> y los 
                                                <a href="https://policies.google.com/terms" target="_blank" style="color: #d6bcfa;">Términos de servicio</a> de Google.
                                            </small>
                                        \`;
                                    }
                                }
                                
                                // Callback cuando reCAPTCHA expira
                                function onRecaptchaExpired() {
                                    console.log('reCAPTCHA expirado');
                                    recaptchaToken = '';
                                    recaptchaVerified = false;
                                    
                                    // Restaurar estilos
                                    const recaptchaContainer = document.querySelector('.recaptcha-container');
                                    recaptchaContainer.style.borderColor = '';
                                    recaptchaContainer.style.boxShadow = '';
                                    
                                    // Restaurar badge
                                    const badge = document.querySelector('.recaptcha-badge');
                                    if (badge) {
                                        badge.innerHTML = \`
                                            Este sitio está protegido por reCAPTCHA y se aplican la 
                                            <a href="https://policies.google.com/privacy" target="_blank">Política de privacidad</a> y los 
                                            <a href="https://policies.google.com/terms" target="_blank">Términos de servicio</a> de Google.
                                        \`;
                                    }
                                }
                                
                                // Callback cuando hay error en reCAPTCHA
                                function onRecaptchaError() {
                                    console.log('Error en reCAPTCHA');
                                    recaptchaToken = '';
                                    recaptchaVerified = false;
                                    
                                    document.getElementById('recaptchaError').textContent = 'Error en la verificación. Por favor, recargue la página.';
                                    document.getElementById('recaptchaError').style.display = 'block';
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
                                
                                // Para reCAPTCHA v2 (forzar verificación)
                                function resetRecaptcha() {
                                    if (typeof grecaptcha !== 'undefined') {
                                        const recaptchaDiv = document.querySelector('.g-recaptcha');
                                        if (recaptchaDiv && recaptchaDiv.dataset.widgetId) {
                                            grecaptcha.reset(recaptchaDiv.dataset.widgetId);
                                            onRecaptchaExpired();
                                        }
                                    }
                                }
                                
                                function validateForm() {
                                    let isValid = true;
                                    const formData = {};
                                    
                                    // Validar nombre completo (solo letras, max 40 caracteres)
                                    const nombreInput = document.getElementById('nombre');
                                    const nombreError = document.getElementById('nombreError');
                                    const nombreRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑ\\s]{1,40}$/;
                                    
                                    if (!nombreInput.value.trim()) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'Este campo es obligatorio';
                                        nombreError.style.display = 'block';
                                        isValid = false;
                                    } else if (!nombreRegex.test(nombreInput.value)) {
                                        nombreInput.classList.add('error');
                                        nombreInput.classList.remove('success');
                                        nombreError.textContent = 'Solo letras, máximo 40 caracteres';
                                        nombreError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        nombreInput.classList.remove('error');
                                        nombreInput.classList.add('success');
                                        nombreError.style.display = 'none';
                                        formData.nombre = nombreInput.value.trim();
                                    }
                                    
                                    // Validar email
                                    const emailInput = document.getElementById('email');
                                    const emailError = document.getElementById('emailError');
                                    const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
                                    
                                    if (!emailInput.value.trim()) {
                                        emailInput.classList.add('error');
                                        emailInput.classList.remove('success');
                                        emailError.textContent = 'Este campo es obligatorio';
                                        emailError.style.display = 'block';
                                        isValid = false;
                                    } else if (!emailRegex.test(emailInput.value)) {
                                        emailInput.classList.add('error');
                                        emailInput.classList.remove('success');
                                        emailError.textContent = 'Formato de email inválido';
                                        emailError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        emailInput.classList.remove('error');
                                        emailInput.classList.add('success');
                                        emailError.style.display = 'none';
                                        formData.email = emailInput.value.trim();
                                    }
                                    
                                    // Validar teléfono (exactamente 10 dígitos)
                                    const telefonoInput = document.getElementById('telefono');
                                    const telefonoError = document.getElementById('telefonoError');
                                    const telefonoRegex = /^\\d{10}$/;
                                    
                                    if (!telefonoInput.value.trim()) {
                                        telefonoInput.classList.add('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.textContent = 'Este campo es obligatorio';
                                        telefonoError.style.display = 'block';
                                        isValid = false;
                                    } else if (!telefonoRegex.test(telefonoInput.value)) {
                                        telefonoInput.classList.add('error');
                                        telefonoInput.classList.remove('success');
                                        telefonoError.textContent = 'Debe tener exactamente 10 dígitos numéricos';
                                        telefonoError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        telefonoInput.classList.remove('error');
                                        telefonoInput.classList.add('success');
                                        telefonoError.style.display = 'none';
                                        formData.telefono = telefonoInput.value.trim();
                                    }
                                    
                                    // Validar mensaje (solo palabras, máximo 20 palabras)
                                    const mensajeTextarea = document.getElementById('mensaje');
                                    const mensajeError = document.getElementById('mensajeError');
                                    
                                    if (mensajeTextarea.value.trim()) {
                                        const mensaje = mensajeTextarea.value.trim();
                                        
                                        // Contar palabras
                                        const palabras = mensaje.split(/\\s+/).filter(word => word.length > 0);
                                        
                                        // Validar que solo contenga letras y espacios
                                        const soloLetrasRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑ\\s]+$/;
                                        
                                        if (!soloLetrasRegex.test(mensaje)) {
                                            mensajeTextarea.classList.add('error');
                                            mensajeTextarea.classList.remove('success');
                                            mensajeError.textContent = 'Solo se permiten letras y espacios (sin números ni símbolos)';
                                            mensajeError.style.display = 'block';
                                            isValid = false;
                                        } else if (palabras.length > 20) {
                                            mensajeTextarea.classList.add('error');
                                            mensajeTextarea.classList.remove('success');
                                            mensajeError.textContent = 'Máximo 20 palabras. Actual: ' + palabras.length;
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
                                        // El mensaje es opcional, así que si está vacío no hay error
                                        mensajeTextarea.classList.remove('error', 'success');
                                        mensajeError.style.display = 'none';
                                        formData.mensaje = '';
                                    }
                                    
                                    // Validar reCAPTCHA
                                    if (!validateRecaptcha()) {
                                        isValid = false;
                                    }
                                    
                                    // Validar términos aceptados
                                    const terminosCheckbox = document.getElementById('terminos');
                                    const terminosError = document.getElementById('terminosError');
                                    
                                    if (!terminosCheckbox.checked) {
                                        terminosError.style.display = 'block';
                                        isValid = false;
                                    } else {
                                        terminosError.style.display = 'none';
                                        formData.terminosAceptados = true;
                                    }
                                    
                                    // Almacenar token de reCAPTCHA
                                    formData.recaptchaToken = recaptchaToken;
                                    
                                    // Mostrar resultados si es válido
                                    if (isValid) {
                                        mostrarResultados(formData);
                                        
                                        // Resetear reCAPTCHA después de enviar
                                        setTimeout(resetRecaptcha, 1000);
                                    }
                                    
                                    return false; // Prevenir envío real del formulario
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
                                    
                                    // Desplazar suavemente hacia los resultados
                                    resultadoDiv.scrollIntoView({ behavior: 'smooth' });
                                    
                                    // Animación de éxito
                                    resultadoDiv.style.animation = 'pulse 0.5s ease-in-out';
                                    setTimeout(() => {
                                        resultadoDiv.style.animation = '';
                                    }, 500);
                                }
                                
                                function resetForm() {
                                    const form = document.getElementById('formularioBasico');
                                    form.reset();
                                    
                                    // Limpiar clases de validación
                                    const inputs = form.querySelectorAll('.form-input, .textarea-field');
                                    for (let i = 0; i < inputs.length; i++) {
                                        inputs[i].classList.remove('error', 'success');
                                    }
                                    
                                    // Ocultar mensajes de error
                                    const errors = form.querySelectorAll('.error-message');
                                    for (let i = 0; i < errors.length; i++) {
                                        errors[i].style.display = 'none';
                                    }
                                    
                                    // Resetear reCAPTCHA
                                    resetRecaptcha();
                                    
                                    // Ocultar resultados
                                    const resultadoDiv = document.getElementById('formResultado');
                                    resultadoDiv.style.display = 'none';
                                    
                                    return false;
                                }
                                
                                // Validación en tiempo real
                                function setupRealTimeValidation() {
                                    const nombreInput = document.getElementById('nombre');
                                    const emailInput = document.getElementById('email');
                                    const telefonoInput = document.getElementById('telefono');
                                    const mensajeTextarea = document.getElementById('mensaje');
                                    
                                    // Validar nombre mientras escribe (solo letras)
                                    nombreInput.addEventListener('input', function(e) {
                                        // Filtrar caracteres no deseados
                                        this.value = this.value.replace(/[^A-Za-záéíóúÁÉÍÓÚñÑ\\s]/g, '');
                                        
                                        // Limitar a 40 caracteres
                                        if (this.value.length > 40) {
                                            this.value = this.value.substring(0, 40);
                                        }
                                        
                                        if (this.value.trim()) {
                                            const nombreRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑ\\s]{1,40}$/;
                                            if (nombreRegex.test(this.value)) {
                                                this.classList.remove('error');
                                                this.classList.add('success');
                                            } else {
                                                this.classList.add('error');
                                                this.classList.remove('success');
                                            }
                                        }
                                    });
                                    
                                    // Validar teléfono mientras escribe (solo números)
                                    telefonoInput.addEventListener('input', function(e) {
                                        // Solo permitir números
                                        this.value = this.value.replace(/[^\\d]/g, '');
                                        
                                        // Limitar a 10 dígitos
                                        if (this.value.length > 10) {
                                            this.value = this.value.substring(0, 10);
                                        }
                                        
                                        if (this.value.trim()) {
                                            const telefonoRegex = /^\\d{10}$/;
                                            if (telefonoRegex.test(this.value)) {
                                                this.classList.remove('error');
                                                this.classList.add('success');
                                            } else {
                                                this.classList.add('error');
                                                this.classList.remove('success');
                                            }
                                        }
                                    });
                                    
                                    // Validar mensaje mientras escribe
                                    mensajeTextarea.addEventListener('input', function(e) {
                                        const mensaje = this.value.trim();
                                        
                                        if (mensaje) {
                                            // Contar palabras
                                            const palabras = mensaje.split(/\\s+/).filter(word => word.length > 0);
                                            
                                            // Validar que solo contenga letras
                                            const soloLetrasRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑ\\s]+$/;
                                            
                                            if (!soloLetrasRegex.test(mensaje)) {
                                                this.classList.add('error');
                                                this.classList.remove('success');
                                            } else if (palabras.length > 20) {
                                                this.classList.add('error');
                                                this.classList.remove('success');
                                            } else {
                                                this.classList.remove('error');
                                                this.classList.add('success');
                                            }
                                            
                                            // Mostrar contador de palabras
                                            const contador = document.getElementById('contadorPalabras');
                                            if (!contador) {
                                                const contadorDiv = document.createElement('div');
                                                contadorDiv.id = 'contadorPalabras';
                                                contadorDiv.className = 'form-hint';
                                                contadorDiv.style.marginTop = '5px';
                                                contadorDiv.style.fontSize = '0.8rem';
                                                contadorDiv.style.color = palabras.length > 20 ? '#e53e3e' : '#718096';
                                                contadorDiv.textContent = 'Palabras: ' + palabras.length + '/20';
                                                this.parentNode.insertBefore(contadorDiv, this.nextSibling);
                                            } else {
                                                contador.textContent = 'Palabras: ' + palabras.length + '/20';
                                                contador.style.color = palabras.length > 20 ? '#e53e3e' : '#718096';
                                            }
                                        }
                                    });
                                    
                                    // Limpiar contador cuando se resetea el formulario
                                    document.getElementById('formularioBasico').addEventListener('reset', function() {
                                        const contador = document.getElementById('contadorPalabras');
                                        if (contador) {
                                            contador.remove();
                                        }
                                    });
                                }
                                
                                // Inicializar cuando se carga la página
                                document.addEventListener('DOMContentLoaded', function() {
                                    setupRealTimeValidation();
                                    
                                    // Cargar reCAPTCHA
                                    loadRecaptcha();
                                    
                                    // Agregar estilos para animaciones
                                    const style = document.createElement('style');
                                    style.textContent = \`
                                        @keyframes pulse {
                                            0% { transform: scale(1); }
                                            50% { transform: scale(1.02); }
                                            100% { transform: scale(1); }
                                        }
                                        
                                        .result-item {
                                            margin-bottom: 8px;
                                            padding-bottom: 8px;
                                            border-bottom: 1px solid #e2e8f0;
                                        }
                                        
                                        .result-item:last-child {
                                            border-bottom: none;
                                            margin-bottom: 0;
                                        }
                                    \`;
                                    document.head.appendChild(style);
                                });
                                """
                            }
                        }
                    }
                    body {
                        div("main-container") {
                            // ===== HERO SECTION (CONTENIDO EXISTENTE) =====
                            div("hero-container") {
                                div("header-section") {
                                    h1("main-title") { +"Sistema de Breadcrumbs" }
                                    p("subtitle") { +"Un ejemplo práctico de navegación jerárquica implementado con tecnología moderna" }
                                }

                                div("levels-container") {
                                    // Nivel 1
                                    a(classes = "level-card", href = "/breadcrumbs") {
                                        div("level-number") { +"1" }
                                        h3("level-title") { +"Nivel 1 - Introducción" }
                                        p("level-description") { +"Conceptos básicos y fundamentos de los sistemas de breadcrumbs" }
                                    }

                                    // Nivel 2
                                    a(classes = "level-card", href = "/breadcrumbs/detalle") {
                                        div("level-number") { +"2" }
                                        h3("level-title") { +"Nivel 2 - Detalles" }
                                        p("level-description") { +"Análisis técnico y casos de uso de navegación jerárquica" }
                                    }

                                    // Nivel 3
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

                            // ===== FORMULARIO BÁSICO SECTION CON reCAPTCHA =====
                            div("form-container") {
                                div("form-header") {
                                    h2("form-title") { +"Formulario de Contacto" }
                                    p("form-subtitle") { +"Complete el formulario con sus datos básicos" }
                                }

                                div("form-wrapper") {
                                    form {
                                        attributes["id"] = "formularioBasico"
                                        attributes["onsubmit"] = "return validateForm();"

                                        // Campo 1: Nombre completo (solo letras, max 40 caracteres)
                                        div("form-group") {
                                            label(classes = "form-label required") {
                                                attributes["for"] = "nombre"
                                                +"Nombre completo"
                                            }
                                            input(type = InputType.text, classes = "form-input") {
                                                attributes["id"] = "nombre"
                                                attributes["placeholder"] = "Ingrese su nombre completo (solo letras)"
                                                attributes["maxlength"] = "40"
                                                attributes["pattern"] = "[A-Za-záéíóúÁÉÍÓÚñÑ\\s]+"
                                                attributes["title"] = "Solo se permiten letras y espacios"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "nombreError"
                                                +"Solo letras, máximo 40 caracteres"
                                            }
                                            div("form-hint") {
                                                +"Máximo 40 caracteres, solo letras y espacios"
                                            }
                                        }

                                        // Campo 2: Email (validación de formato)
                                        div("form-group") {
                                            label(classes = "form-label required") {
                                                attributes["for"] = "email"
                                                +"Correo electrónico"
                                            }
                                            input(type = InputType.email, classes = "form-input") {
                                                attributes["id"] = "email"
                                                attributes["placeholder"] = "ejemplo@correo.com"
                                                attributes["pattern"] = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "emailError"
                                                +"Ingrese un email válido"
                                            }
                                            div("form-hint") {
                                                +"Ejemplo: usuario@dominio.com"
                                            }
                                        }

                                        // Campo 3: Teléfono (solo números, exactamente 10 dígitos)
                                        div("form-group") {
                                            label(classes = "form-label required") {
                                                attributes["for"] = "telefono"
                                                +"Teléfono"
                                            }
                                            input(type = InputType.tel, classes = "form-input") {
                                                attributes["id"] = "telefono"
                                                attributes["placeholder"] = "1234567890"
                                                attributes["maxlength"] = "10"
                                                attributes["pattern"] = "\\d{10}"
                                                attributes["title"] = "Debe tener exactamente 10 dígitos numéricos"
                                            }
                                            div("error-message") {
                                                attributes["id"] = "telefonoError"
                                                +"Ingrese 10 dígitos numéricos"
                                            }
                                            div("form-hint") {
                                                +"Formato: 10 dígitos sin espacios ni guiones"
                                            }
                                        }

                                        // Campo 4: Mensaje (solo palabras, máximo 20 palabras)
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
                                                +"Máximo 20 palabras, solo letras y espacios. No se permiten números ni símbolos."
                                            }
                                        }

                                        // Campo 5: reCAPTCHA v2
                                        div("form-group") {
                                            label(classes = "form-label required") {
                                                attributes["for"] = "recaptcha"
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
                                                    // SITE KEY: 6LfVDVcsAAAAAErTmnJNGjMvB19ND5u5wN9NKGde
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
                                                label(classes = "checkbox-label required") {
                                                    attributes["for"] = "terminos"
                                                    +"Acepto los términos y condiciones"
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

            // ========== NIVEL 1 CON DISEÑO ==========
            get("/breadcrumbs") {
                println("Sirviendo Nivel 1")
                call.respondHtml {
                    head {
                        title { +"Nivel 1 - Introducción" }
                        style { unsafe { +getProfessionalCSS() } }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Sistema Breadcrumbs" }
                                    }
                                    nav("primary-nav") {
                                        a(href = "/", classes = "nav-item") { +"Inicio" }
                                        a(href = "#", classes = "nav-item active") { +"Nivel 1" }
                                        a(href = "/breadcrumbs/detalle", classes = "nav-item") { +"Nivel 2" }
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

            // ========== NIVEL 2 CON DISEÑO ==========
            get("/breadcrumbs/detalle") {
                println("Sirviendo Nivel 2")
                call.respondHtml {
                    head {
                        title { +"Nivel 2 - Detalles" }
                        style { unsafe { +getProfessionalCSS() } }
                    }
                    body {
                        div("page-wrapper") {
                            // Header
                            header("main-header") {
                                div("header-content") {
                                    h1("site-logo") {
                                        a(href = "/") { +"Sistema Breadcrumbs" }
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

            // ========== NIVEL 3 CON DISEÑO ==========
            get("/breadcrumbs/detalle/configuracion") {
                println("Intentando servir Nivel 3")

                try {
                    call.respondHtml {
                        head {
                            title { +"Nivel 3 - Configuración" }
                            style { unsafe { +getProfessionalCSS() } }
                        }
                        body {
                            div("page-wrapper") {
                                // Header
                                header("main-header") {
                                    div("header-content") {
                                        h1("site-logo") {
                                            a(href = "/") { +"Sistema Breadcrumbs" }
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
                                                                    attributes["value"] = "#667eea"
                                                                }
                                                                span("color-value") { +"#667eea" }
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

// Configuración del manejo de errores
fun Application.configureStatusPages() {
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        // Manejo de error 404 - Ruta no encontrada
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondHtml(status) {
                head {
                    title { +"404 - Página No Encontrada" }
                    style { unsafe { +getErrorCSS() } }
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
                    style { unsafe { +getErrorCSS() } }
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
                    style { unsafe { +getErrorCSS() } }
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
                    style { unsafe { +getErrorCSS() } }
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
                    style { unsafe { +getErrorCSS() } }
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
                    style { unsafe { +getErrorCSS() } }
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

// Función para obtener CSS profesional (resto del código igual)
fun getProfessionalCSS(): String {
    return """
    /* ===== RESET Y CONFIGURACIÓN BASE ===== */
    * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    }
    
    body {
        background: #f7fafc;
        color: #2d3748;
        line-height: 1.6;
        min-height: 100vh;
    }
    
    /* ===== LAYOUT PRINCIPAL ===== */
    .page-wrapper {
        max-width: 1200px;
        margin: 0 auto;
        background: white;
        min-height: 100vh;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    }
    
    /* ===== HEADER PRINCIPAL ===== */
    .main-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 0 40px;
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
        color: white;
        text-decoration: none;
        font-size: 1.75rem;
        font-weight: 700;
        letter-spacing: -0.5px;
        transition: opacity 0.2s;
    }
    
    .site-logo a:hover {
        opacity: 0.9;
    }
    
    .primary-nav {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
    }
    
    .nav-item {
        color: rgba(255, 255, 255, 0.9);
        text-decoration: none;
        font-weight: 500;
        padding: 10px 20px;
        border-radius: 8px;
        transition: all 0.2s;
        font-size: 0.95rem;
    }
    
    .nav-item:hover {
        background: rgba(255, 255, 255, 0.15);
        color: white;
    }
    
    .nav-item.active {
        background: rgba(255, 255, 255, 0.25);
        color: white;
        font-weight: 600;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }
    
    /* ===== BREADCRUMBS NAV ===== */
    .breadcrumb-nav {
        background: #f8fafc;
        border-bottom: 1px solid #e2e8f0;
        padding: 16px 40px;
    }
    
    .breadcrumb-list {
        list-style: none;
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
    }
    
    .breadcrumb-item {
        display: flex;
        align-items: center;
    }
    
    .breadcrumb-link {
        color: #667eea;
        text-decoration: none;
        font-weight: 500;
        font-size: 0.9rem;
        padding: 4px 8px;
        border-radius: 4px;
        transition: all 0.2s;
    }
    
    .breadcrumb-link:hover {
        background: rgba(102, 126, 234, 0.1);
        text-decoration: underline;
    }
    
    .breadcrumb-separator {
        color: #a0aec0;
        font-weight: 400;
        padding: 0 4px;
    }
    
    .breadcrumb-text {
        color: #4a5568;
        font-weight: 600;
        font-size: 0.9rem;
        padding: 4px 8px;
    }
    
    .breadcrumb-item.active .breadcrumb-text {
        color: #2d3748;
    }
    
    /* ===== ÁREA DE CONTENIDO ===== */
    .content-area {
        padding: 40px;
    }
    
    .content-card {
        background: white;
        border-radius: 12px;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
        border: 1px solid #e2e8f0;
        overflow: hidden;
    }
    
    .card-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 32px;
    }
    
    .card-title {
        font-size: 2rem;
        font-weight: 700;
        margin-bottom: 8px;
    }
    
    .card-subtitle {
        font-size: 1rem;
        opacity: 0.9;
        font-weight: 400;
    }
    
    .card-body {
        padding: 32px;
    }
    
    .card-text {
        font-size: 1.125rem;
        line-height: 1.7;
        color: #4a5568;
        margin-bottom: 32px;
    }
    
    /* ===== COMPONENTES ESPECIALES ===== */
    .highlight-section {
        background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
        color: white;
        padding: 24px;
        border-radius: 8px;
        margin: 32px 0;
    }
    
    .highlight-title {
        font-size: 1.25rem;
        font-weight: 600;
        margin-bottom: 16px;
    }
    
    .benefit-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        margin-bottom: 12px;
    }
    
    .benefit-icon {
        font-weight: bold;
        font-size: 1.2rem;
        margin-top: 2px;
    }
    
    .benefit-text {
        flex: 1;
        font-size: 1rem;
    }
    
    .info-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 24px;
        margin: 32px 0;
    }
    
    .info-block {
        background: #f8fafc;
        padding: 24px;
        border-radius: 8px;
        border: 1px solid #e2e8f0;
        transition: transform 0.2s;
    }
    
    .info-block:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }
    
    .info-title {
        font-size: 1.125rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 12px;
    }
    
    .info-text {
        color: #4a5568;
        line-height: 1.6;
    }
    
    /* ===== TIPOS DE IMPLEMENTACIÓN ===== */
    .implementation-types {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 24px;
        margin: 32px 0;
    }
    
    .type-card {
        background: white;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 24px;
        transition: all 0.2s;
    }
    
    .type-card:hover {
        border-color: #667eea;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
    }
    
    .type-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 16px;
    }
    
    .type-features {
        list-style: none;
        margin-bottom: 16px;
    }
    
    .type-features li {
        padding: 8px 0;
        border-bottom: 1px solid #edf2f7;
        color: #4a5568;
    }
    
    .type-features li:last-child {
        border-bottom: none;
    }
    
    .type-tag {
        display: inline-block;
        padding: 6px 12px;
        border-radius: 20px;
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }
    
    .type-tag.static {
        background: rgba(102, 126, 234, 0.1);
        color: #667eea;
    }
    
    .type-tag.dynamic {
        background: rgba(56, 178, 172, 0.1);
        color: #38b2ac;
    }
    
    .type-tag.path {
        background: rgba(237, 100, 166, 0.1);
        color: #ed64a6;
    }
    
    /* ===== EJEMPLO DE CÓDIGO ===== */
    .code-example {
        margin: 32px 0;
    }
    
    .code-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 16px;
    }
    
    .code-block {
        background: #1a202c;
        border-radius: 8px;
        overflow: hidden;
    }
    
    .code-block pre {
        margin: 0;
        padding: 24px;
        overflow-x: auto;
    }
    
    .code-block code {
        font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', monospace;
        font-size: 0.875rem;
        color: #e2e8f0;
        line-height: 1.6;
    }
    
    /* ===== PANEL DE CONFIGURACIÓN ===== */
    .configuration-panel {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 32px;
        margin: 32px 0;
    }
    
    .config-section {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 24px;
    }
    
    .config-title {
        font-size: 1.125rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 20px;
        padding-bottom: 12px;
        border-bottom: 2px solid #667eea;
    }
    
    .config-options {
        display: flex;
        flex-direction: column;
        gap: 20px;
    }
    
    .config-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }
    
    .config-label {
        font-weight: 500;
        color: #4a5568;
        font-size: 0.95rem;
    }
    
    .config-select {
        padding: 10px 12px;
        border: 1px solid #cbd5e0;
        border-radius: 6px;
        background: white;
        font-size: 0.95rem;
        color: #2d3748;
        transition: border-color 0.2s;
    }
    
    .config-select:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
    
    .color-picker {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    
    .color-picker input[type="color"] {
        width: 60px;
        height: 40px;
        border: none;
        border-radius: 6px;
        cursor: pointer;
    }
    
    .color-value {
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.875rem;
        color: #4a5568;
        background: white;
        padding: 8px 12px;
        border: 1px solid #cbd5e0;
        border-radius: 6px;
        min-width: 100px;
    }
    
    .slider-container {
        display: flex;
        align-items: center;
        gap: 16px;
    }
    
    .slider-container input[type="range"] {
        flex: 1;
        height: 6px;
        border-radius: 3px;
        background: #e2e8f0;
        outline: none;
    }
    
    .slider-value {
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.875rem;
        color: #4a5568;
        min-width: 45px;
    }
    
    .config-switches {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }
    
    .switch-group {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    
    .switch-group input[type="checkbox"] {
        width: 20px;
        height: 20px;
        border: 2px solid #cbd5e0;
        border-radius: 4px;
        cursor: pointer;
    }
    
    .switch-label {
        font-weight: 500;
        color: #4a5568;
        font-size: 0.95rem;
        cursor: pointer;
    }
    
    /* ===== BANNER DE COMPLETACIÓN ===== */
    .completion-banner {
        background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);
        border-radius: 12px;
        padding: 32px;
        margin: 32px 0;
        text-align: center;
    }
    
    .banner-title {
        font-size: 1.5rem;
        font-weight: 700;
        color: #2d3748;
        margin-bottom: 16px;
    }
    
    .banner-text {
        font-size: 1.125rem;
        color: #4a5568;
        max-width: 600px;
        margin: 0 auto 24px;
        line-height: 1.7;
    }
    
    .completion-stats {
        display: flex;
        justify-content: center;
        gap: 32px;
        flex-wrap: wrap;
    }
    
    .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    
    .stat-number {
        font-size: 2rem;
        font-weight: 700;
        color: #2d3748;
        margin-bottom: 4px;
    }
    
    .stat-label {
        font-size: 0.875rem;
        color: #4a5568;
        font-weight: 500;
    }
    
    /* ===== BOTONES DE ACCIÓN ===== */
    .card-footer {
        background: #f8fafc;
        padding: 24px 32px;
        border-top: 1px solid #e2e8f0;
    }
    
    .action-buttons {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        flex-wrap: wrap;
    }
    
    .action-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 12px 28px;
        border-radius: 8px;
        text-decoration: none;
        font-weight: 600;
        font-size: 1rem;
        transition: all 0.2s;
        border: 2px solid transparent;
    }
    
    .action-button.secondary {
        background: white;
        color: #667eea;
        border-color: #667eea;
    }
    
    .action-button.secondary:hover {
        background: rgba(102, 126, 234, 0.1);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
    }
    
    .action-button.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
    }
    
    .action-button.primary:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
    }
    
    /* ===== FOOTER ===== */
    .main-footer {
        background: #2d3748;
        color: #cbd5e0;
        padding: 32px 40px;
        margin-top: 40px;
    }
    
    .footer-content {
        max-width: 1200px;
        margin: 0 auto;
        text-align: center;
    }
    
    .footer-text {
        font-size: 0.875rem;
        margin-bottom: 16px;
    }
    
    .footer-links {
        display: flex;
        justify-content: center;
        gap: 24px;
        flex-wrap: wrap;
    }
    
    .footer-links a {
        color: #cbd5e0;
        text-decoration: none;
        font-size: 0.875rem;
        transition: color 0.2s;
    }
    
    .footer-links a:hover {
        color: white;
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
            padding: 24px;
        }
        
        .action-buttons {
            flex-direction: column;
        }
        
        .action-button {
            width: 100%;
        }
        
        .breadcrumb-nav {
            padding: 16px 20px;
        }
        
        .main-header {
            padding: 0 20px;
        }
    }
    
    /* ===== ANIMACIONES ===== */
    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
    
    .content-card {
        animation: fadeIn 0.5s ease-out;
    }
    
    .type-card,
    .info-block,
    .config-section {
        animation: fadeIn 0.5s ease-out;
        animation-fill-mode: both;
    }
    
    .type-card:nth-child(1) { animation-delay: 0.1s; }
    .type-card:nth-child(2) { animation-delay: 0.2s; }
    .type-card:nth-child(3) { animation-delay: 0.3s; }
    
    .info-block:nth-child(1) { animation-delay: 0.1s; }
    .info-block:nth-child(2) { animation-delay: 0.2s; }
    .info-block:nth-child(3) { animation-delay: 0.3s; }
    """
}

// Función para obtener CSS de errores
fun getErrorCSS(): String {
    return """
    * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    }
    
    body {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
        background: rgba(255, 255, 255, 0.98);
        backdrop-filter: blur(20px);
        border-radius: 24px;
        padding: 60px;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.3);
        text-align: center;
    }
    
    .error-code {
        font-size: 8rem;
        font-weight: 800;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        line-height: 1;
        margin-bottom: 20px;
    }
    
    .error-title {
        font-size: 2.5rem;
        font-weight: 700;
        color: #1a202c;
        margin-bottom: 16px;
    }
    
    .error-message {
        font-size: 1.25rem;
        color: #4a5568;
        line-height: 1.6;
        max-width: 600px;
        margin: 0 auto 32px;
    }
    
    .error-details {
        background: #f8fafc;
        border-radius: 12px;
        padding: 24px;
        margin: 32px 0;
        text-align: left;
        border: 1px solid #e2e8f0;
    }
    
    .error-details p {
        margin-bottom: 12px;
        color: #4a5568;
        font-size: 1rem;
    }
    
    .error-details strong {
        color: #2d3748;
        font-weight: 600;
    }
    
    .error-stack {
        background: #1a202c;
        color: #e2e8f0;
        padding: 20px;
        border-radius: 8px;
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.875rem;
        overflow-x: auto;
        text-align: left;
        margin-top: 16px;
        white-space: pre-wrap;
        word-wrap: break-word;
    }
    
    .action-buttons {
        display: flex;
        gap: 16px;
        justify-content: center;
        margin-top: 32px;
        flex-wrap: wrap;
    }
    
    .error-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 16px 32px;
        border-radius: 12px;
        text-decoration: none;
        font-weight: 600;
        font-size: 1.125rem;
        transition: all 0.3s;
        border: 2px solid transparent;
        cursor: pointer;
        min-width: 180px;
    }
    
    .error-button.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
    }
    
    .error-button.primary:hover {
        transform: translateY(-3px);
        box-shadow: 0 16px 32px rgba(102, 126, 234, 0.4);
    }
    
    .error-button.secondary {
        background: white;
        color: #667eea;
        border-color: #667eea;
    }
    
    .error-button.secondary:hover {
        background: rgba(102, 126, 234, 0.1);
        transform: translateY(-3px);
    }
    
    .error-help {
        margin-top: 32px;
        padding-top: 24px;
        border-top: 1px solid #e2e8f0;
        color: #718096;
        font-size: 0.95rem;
    }
    
    @media (max-width: 768px) {
        .error-content {
            padding: 40px 25px;
        }
        
        .error-code {
            font-size: 6rem;
        }
        
        .error-title {
            font-size: 2rem;
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