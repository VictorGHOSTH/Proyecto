// Variables globales
let personasData = [];
let personaEnEdicion = null;

// Función para escapar HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Cargar personas al iniciar
document.addEventListener('DOMContentLoaded', function() {
    cargarPersonas();

    // Configurar eventos
    const btnAgregar = document.getElementById('btnAgregar');
    if (btnAgregar) btnAgregar.addEventListener('click', abrirModalAgregar);

    const btnCerrarModal = document.getElementById('btnCerrarModal');
    if (btnCerrarModal) btnCerrarModal.addEventListener('click', cerrarModal);

    const btnCancelar = document.getElementById('btnCancelar');
    if (btnCancelar) btnCancelar.addEventListener('click', cerrarModal);

    const btnFiltrar = document.getElementById('btnFiltrar');
    if (btnFiltrar) btnFiltrar.addEventListener('click', filtrarPersonas);

    const btnLimpiarFiltro = document.getElementById('btnLimpiarFiltro');
    if (btnLimpiarFiltro) btnLimpiarFiltro.addEventListener('click', limpiarFiltro);

    const personaForm = document.getElementById('personaForm');
    if (personaForm) personaForm.addEventListener('submit', guardarPersona);

    // Validación en tiempo real
    const inputs = document.querySelectorAll('#personaForm input, #personaForm select');
    inputs.forEach(input => {
        input.addEventListener('input', function() {
            validarCampoPersona(this);
        });
        input.addEventListener('blur', function() {
            validarCampoPersona(this);
        });
    });

    // Cerrar modal al hacer clic fuera
    window.addEventListener('click', function(event) {
        const modal = document.getElementById('personaModal');
        if (event.target === modal) {
            cerrarModal();
        }
    });
});

// Función para cargar personas
function cargarPersonas() {
    fetch('/unidad2/api/personas')
        .then(response => response.json())
        .then(data => {
            personasData = data;
            mostrarPersonas(personasData);
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarMensaje('error', 'Error al cargar las personas');
        });
}

// Función para mostrar personas en la tabla
function mostrarPersonas(lista) {
    const tbody = document.getElementById('tablaPersonasBody');

    if (!tbody) return;

    if (lista.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="padding: 40px; text-align: center; color: #9e9e9e;">No hay personas registradas</td></tr>';
        return;
    }

    let html = '';
    lista.forEach(persona => {
        const estadoStyle = persona.estado === 'Activo'
            ? 'background: #e8f5e8; color: #2e7d32; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem; display: inline-block;'
            : 'background: #ffebee; color: #c62828; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem; display: inline-block;';

        html += `<tr>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;">${persona.id}</td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;">${escapeHtml(persona.nombre)}</td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;">${escapeHtml(persona.apellido)}</td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;">${escapeHtml(persona.email)}</td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;">${escapeHtml(persona.telefono)}</td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;">${escapeHtml(persona.genero)}</td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0;"><span style="${estadoStyle}">${persona.estado}</span></td>
            <td style="padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: center;">
                <button onclick="editarPersona(${persona.id})" style="background: none; border: none; color: #2196f3; cursor: pointer; margin-right: 8px;" title="Editar">✏️</button>
                <button onclick="eliminarPersona(${persona.id})" style="background: none; border: none; color: #f44336; cursor: pointer;" title="Eliminar">🗑️</button>
            </td>
        </tr>`;
    });

    tbody.innerHTML = html;
}

// Función para filtrar personas
function filtrarPersonas() {
    const filtroInput = document.getElementById('filtroInput');
    if (!filtroInput) return;

    const filtro = filtroInput.value.toLowerCase().trim();

    if (!filtro) {
        mostrarPersonas(personasData);
        return;
    }

    const filtradas = personasData.filter(p =>
        p.nombre.toLowerCase().includes(filtro) ||
        p.apellido.toLowerCase().includes(filtro) ||
        p.email.toLowerCase().includes(filtro) ||
        p.telefono.includes(filtro) ||
        p.genero.toLowerCase().includes(filtro)
    );

    mostrarPersonas(filtradas);
}

// Función para limpiar filtro
function limpiarFiltro() {
    const filtroInput = document.getElementById('filtroInput');
    if (filtroInput) filtroInput.value = '';
    mostrarPersonas(personasData);
}

// Función para abrir modal de agregar
function abrirModalAgregar() {
    personaEnEdicion = null;
    const modalTitle = document.getElementById('modalTitle');
    if (modalTitle) modalTitle.textContent = 'Agregar Persona';

    const personaForm = document.getElementById('personaForm');
    if (personaForm) personaForm.reset();

    const personaId = document.getElementById('personaId');
    if (personaId) personaId.value = '0';

    const personaEstado = document.getElementById('personaEstado');
    if (personaEstado) personaEstado.value = 'Activo';

    // Limpiar errores
    document.querySelectorAll('#personaForm .real-time-error').forEach(el => {
        el.style.display = 'none';
    });

    // Limpiar clases de validación
    document.querySelectorAll('#personaForm .form-input').forEach(el => {
        el.classList.remove('error', 'valid');
    });

    const modal = document.getElementById('personaModal');
    if (modal) modal.style.display = 'flex';
}

// Función para editar persona
function editarPersona(id) {
    const persona = personasData.find(p => p.id === id);
    if (!persona) return;

    personaEnEdicion = persona;

    const modalTitle = document.getElementById('modalTitle');
    if (modalTitle) modalTitle.textContent = 'Editar Persona';

    const personaId = document.getElementById('personaId');
    if (personaId) personaId.value = persona.id;

    const personaNombre = document.getElementById('personaNombre');
    if (personaNombre) personaNombre.value = persona.nombre;

    const personaApellido = document.getElementById('personaApellido');
    if (personaApellido) personaApellido.value = persona.apellido;

    const personaEmail = document.getElementById('personaEmail');
    if (personaEmail) personaEmail.value = persona.email;

    const personaTelefono = document.getElementById('personaTelefono');
    if (personaTelefono) personaTelefono.value = persona.telefono;

    const personaGenero = document.getElementById('personaGenero');
    if (personaGenero) personaGenero.value = persona.genero;

    const personaEstado = document.getElementById('personaEstado');
    if (personaEstado) personaEstado.value = persona.estado;

    // Limpiar errores
    document.querySelectorAll('#personaForm .real-time-error').forEach(el => {
        el.style.display = 'none';
    });

    const modal = document.getElementById('personaModal');
    if (modal) modal.style.display = 'flex';
}

// Función para cerrar modal
function cerrarModal() {
    const modal = document.getElementById('personaModal');
    if (modal) modal.style.display = 'none';
    personaEnEdicion = null;
}

// Función para validar campos
function validarCampoPersona(campo) {
    if (!campo) return true;

    const valor = campo.value.trim();
    const id = campo.id;
    let esValido = true;
    let mensajeError = '';

    if (id === 'personaNombre' || id === 'personaApellido') {
        const nombreRegex = /^[A-Za-záéíóúÁÉÍÓÚñÑüÜ\s]+$/;
        if (valor.length === 0) {
            esValido = false;
            mensajeError = 'Este campo es obligatorio';
        } else if (valor.length > 50) {
            esValido = false;
            mensajeError = 'Máximo 50 caracteres';
        } else if (!nombreRegex.test(valor)) {
            esValido = false;
            mensajeError = 'Solo letras y espacios';
        }
    } else if (id === 'personaEmail') {
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (valor.length === 0) {
            esValido = false;
            mensajeError = 'El email es obligatorio';
        } else if (!emailRegex.test(valor)) {
            esValido = false;
            mensajeError = 'Formato de email inválido';
        }
    } else if (id === 'personaTelefono') {
        const soloDigitos = /^\d+$/;
        if (valor.length === 0) {
            esValido = false;
            mensajeError = 'El teléfono es obligatorio';
        } else if (!soloDigitos.test(valor)) {
            esValido = false;
            mensajeError = 'Solo dígitos numéricos';
        } else if (valor.length < 8 || valor.length > 15) {
            esValido = false;
            mensajeError = 'Entre 8 y 15 dígitos';
        }
    } else if (id === 'personaGenero') {
        if (valor === '') {
            esValido = false;
            mensajeError = 'Seleccione un género';
        }
    }

    const errorElement = document.getElementById(id + 'Error');
    if (errorElement) {
        if (!esValido) {
            campo.classList.add('error');
            campo.classList.remove('valid');
            errorElement.textContent = mensajeError;
            errorElement.style.display = 'block';
        } else {
            campo.classList.remove('error');
            campo.classList.add('valid');
            errorElement.style.display = 'none';
        }
    }

    return esValido;
}

// Función para validar todo el formulario
function validarFormularioPersona() {
    const campos = [
        document.getElementById('personaNombre'),
        document.getElementById('personaApellido'),
        document.getElementById('personaEmail'),
        document.getElementById('personaTelefono'),
        document.getElementById('personaGenero')
    ];

    let esValido = true;

    campos.forEach(campo => {
        if (campo && !validarCampoPersona(campo)) {
            esValido = false;
        }
    });

    return esValido;
}

// Función para guardar persona (Crear o Actualizar)
function guardarPersona(event) {
    event.preventDefault();

    if (!validarFormularioPersona()) {
        mostrarMensaje('error', 'Por favor, corrija los errores en el formulario');
        return;
    }

    const personaData = {
        id: parseInt(document.getElementById('personaId')?.value || '0'),
        nombre: document.getElementById('personaNombre')?.value.trim() || '',
        apellido: document.getElementById('personaApellido')?.value.trim() || '',
        email: document.getElementById('personaEmail')?.value.trim() || '',
        telefono: document.getElementById('personaTelefono')?.value.trim() || '',
        genero: document.getElementById('personaGenero')?.value || '',
        estado: document.getElementById('personaEstado')?.value || 'Activo'
    };

    const btnGuardar = document.getElementById('btnGuardar');
    if (btnGuardar) {
        btnGuardar.disabled = true;
        btnGuardar.textContent = 'Guardando...';
    }

    const url = personaEnEdicion
        ? `/unidad2/api/personas/${personaEnEdicion.id}`
        : '/unidad2/api/personas';

    const method = personaEnEdicion ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(personaData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            mostrarMensaje('error', data.error);
        } else {
            mostrarMensaje('success', personaEnEdicion ? 'Persona actualizada exitosamente' : 'Persona agregada exitosamente');
            cargarPersonas();
            cerrarModal();
        }
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarMensaje('error', 'Error de conexión: ' + error.message);
    })
    .finally(() => {
        if (btnGuardar) {
            btnGuardar.disabled = false;
            btnGuardar.textContent = 'Guardar';
        }
    });
}

// Función para eliminar persona
function eliminarPersona(id) {
    if (!confirm('¿Está seguro de eliminar esta persona?')) {
        return;
    }

    fetch(`/unidad2/api/personas/${id}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            mostrarMensaje('error', data.error);
        } else {
            mostrarMensaje('success', 'Persona eliminada exitosamente');
            cargarPersonas();
        }
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarMensaje('error', 'Error de conexión: ' + error.message);
    });
}

// Función para mostrar mensajes
function mostrarMensaje(tipo, mensaje) {
    const messageDiv = document.getElementById('crudMessage');
    if (!messageDiv) return;

    messageDiv.style.display = 'block';

    if (tipo === 'success') {
        messageDiv.className = 'success-message';
        messageDiv.style.background = '#d4edda';
        messageDiv.style.color = '#155724';
        messageDiv.style.border = '1px solid #c3e6cb';
    } else {
        messageDiv.className = 'error-message';
        messageDiv.style.background = '#ffebee';
        messageDiv.style.color = '#d32f2f';
        messageDiv.style.border = '1px solid #ffcdd2';
    }

    messageDiv.innerHTML = mensaje;

    // Scroll al mensaje
    messageDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });

    // Ocultar después de 3 segundos
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 3000);
}