/**
 * AJAX Form Utilities - Fredy Queen Soccer
 */

function clearFormErrors(form) {
    if (!form) return;
    form.querySelectorAll('.is-invalid').forEach(el => {
        el.classList.remove('is-invalid');
        el.setAttribute('aria-invalid', 'false');
    });
    form.querySelectorAll('.invalid-feedback, .error-text, [data-error-for]').forEach(el => el.textContent = '');
    const feedback = form.querySelector('.modal-feedback, .form-feedback');
    if (feedback) {
        feedback.innerHTML = '';
        feedback.className = feedback.classList.contains('modal-feedback') ? 'modal-feedback' : 'form-feedback';
    }
}

function showFormErrors(form, errors) {
    if (!form || !errors) return;
    const feedback = form.querySelector('.modal-feedback, .form-feedback');
    let globalMessages = [];

    for (const [field, message] of Object.entries(errors)) {
        if (field === 'global' || field === 'error') {
            globalMessages.push(message);
            continue;
        }
        const input = form.querySelector(`[name="${field}"]`);
        if (input) {
            if (window.AccountValidation) {
                window.AccountValidation.error(input, message);
                continue;
            }
            input.classList.add('is-invalid');
            let errorEl = form.querySelector(`[data-error-for="${field}"]`);
            if (!errorEl) {
                errorEl = input.parentElement.querySelector('.invalid-feedback, .error-text');
            }
            if (errorEl) {
                errorEl.textContent = message;
            } else {
                globalMessages.push(`${field}: ${message}`);
            }
        } else {
            globalMessages.push(message);
        }
    }

    if (feedback && globalMessages.length > 0) {
        feedback.innerHTML = `<div class="alert alert-danger mb-3">${globalMessages.join('<br>')}</div>`;
    }
}

function showFormSuccess(form, message) {
    if (!form) return;
    const feedback = form.querySelector('.modal-feedback, .form-feedback');
    if (feedback) {
        feedback.innerHTML = `<div class="alert alert-success mb-3">${message || 'Operación completada exitosamente.'}</div>`;
    }
}

function submitAjaxForm(form, successMessage, onSuccess) {
    if (!form) return;
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        clearFormErrors(form);
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn ? submitBtn.innerHTML : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Guardando...';
        }

        try {
            const response = await fetch(form.action, {
                method: form.method || 'POST',
                body: new FormData(form),
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                showFormErrors(form, data);
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalText;
                }
                return;
            }

            showFormSuccess(form, data.mensaje || successMessage);
            setTimeout(() => {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalText;
                }
                if (typeof onSuccess === 'function') {
                    onSuccess(data);
                } else {
                    window.location.reload();
                }
            }, 800);
        } catch (err) {
            console.error('Error AJAX:', err);
            showFormErrors(form, { global: 'Error de conexión. Intente nuevamente.' });
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            }
        }
    });
}

// Aliases for backwards compatibility
const limpiarErroresFormulario = clearFormErrors;
const mostrarErroresFormulario = showFormErrors;
const mostrarExitoFormulario = showFormSuccess;
const enviarFormularioAjax = submitAjaxForm;
