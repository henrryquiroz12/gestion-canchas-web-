/* Account patterns come from the same constants as Spring Bean Validation. */
window.AccountValidation = (() => {
    const messages = {
        fullName: 'Usa solo letras y espacios entre palabras.',
        dni: 'El DNI debe contener exactamente 8 dígitos.',
        phoneNumber: 'El celular debe contener 9 dígitos y comenzar con 9.',
        email: 'Ingresa un correo válido de gmail.com, hotmail.com o utp.edu.pe, sin espacios.'
    };

    const inputFilters = {
        fullName: /[^\p{L}\p{M} ]/gu,
        dni: /[^0-9]/g,
        phoneNumber: /[^0-9]/g
    };

    function filterInput(input, pattern) {
        const clean = value => value.replace(pattern, '');
        function sanitize(event) {
            if (event.isComposing) return;
            const value = clean(input.value).slice(0, input.maxLength);
            if (value === input.value) return;
            const caret = Math.min(clean(input.value.slice(0, input.selectionStart)).length, value.length);
            input.value = value;
            input.setSelectionRange(caret, caret);
        }

        input.addEventListener('input', sanitize);
        input.addEventListener('compositionend', sanitize);
        input.addEventListener('paste', event => {
            if (!event.clipboardData) return;
            const pasted = event.clipboardData.getData('text');
            const start = input.selectionStart;
            const end = input.selectionEnd;
            const available = Math.max(0, input.maxLength - (input.value.length - (end - start)));
            const value = clean(pasted).slice(0, available);
            if (value === pasted) return;
            event.preventDefault();
            if (!value) return;
            input.setRangeText(value, start, end, 'end');
            input.dispatchEvent(new Event('input', {bubbles: true}));
        });
    }

    function error(input, message) {
        const form = input.form;
        input.closest('.form-group')?.querySelectorAll('.invalid-feedback').forEach(output => {
            output.textContent = '';
        });
        let output = form.querySelector(`[data-error-for="${input.name}"]`) ||
            document.getElementById(input.id + 'Error');
        if (!output) {
            output = document.createElement('span');
            output.className = 'error-text text-danger small d-block';
            output.dataset.errorFor = input.name;
            input.insertAdjacentElement('afterend', output);
        }
        if (!output.id) output.id = `${input.id || input.name}-validation-error`;
        output.setAttribute('role', 'alert');
        const described = new Set((input.getAttribute('aria-describedby') || '').split(' ').filter(Boolean));
        described.add(output.id);
        input.setAttribute('aria-describedby', [...described].join(' '));
        input.setAttribute('aria-invalid', message ? 'true' : 'false');
        input.classList.toggle('is-invalid', !!message);
        output.textContent = message;
    }

    function validate(scope, focus = true) {
        let first;
        const genderRadio = scope.querySelector('input[type="radio"][name="gender"]');
        scope.querySelectorAll('input, select, textarea').forEach(input => {
            if (input.disabled || input.readOnly || input.type === 'hidden') return;
            if (input.type === 'radio' && input.name === 'gender') {
                if (input !== genderRadio) return;
                const missing = input.validity.valueMissing;
                error(input, missing ? 'Elige una opción' : '');
                if (missing && !first) first = input;
                return;
            }
            let message = '';
            const pattern = window.ACCOUNT_RULES[input.name];
            if (input.required && !input.value.trim()) message = input.name === 'gender' ? 'Elige una opción' : 'Este campo es obligatorio.';
            else if (input.value && pattern) {
                const match = new RegExp(pattern, input.name === 'email' ? 'iu' : 'u').exec(input.value);
                if (!match || match[0] !== input.value) message = messages[input.name];
            }
            if (!message && input.maxLength > 0 && input.value.length > input.maxLength) {
                message = `Usa como máximo ${input.maxLength} caracteres.`;
            }
            if (!message && !input.validity.valid) message = input.validationMessage;
            error(input, message);
            if (message && !first) first = input;
        });
        if (first && focus) first.focus();
        return !first;
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('form').forEach(form => {
            if (!form.querySelector('input[name="fullName"], input[name="email"]')) return;
            form.querySelectorAll('input, select[name="gender"]').forEach(input => {
                const backendError = input.closest('.form-group')?.querySelector('.invalid-feedback');
                if (backendError?.textContent.trim()) error(input, backendError.textContent.trim());
                if (input.hasAttribute('oninput') && ['dni', 'phoneNumber'].includes(input.name)) {
                    input.removeAttribute('oninput');
                }
                const filter = inputFilters[input.name];
                if (filter) filterInput(input, filter);
                input.addEventListener('input', () => error(input, ''));
                if (input.name === 'gender') {
                    input.addEventListener('change', () => {
                        const target = input.type === 'radio'
                            ? form.querySelector('input[type="radio"][name="gender"]')
                            : input;
                        error(target, '');
                    });
                }
                if (input.name !== 'password') return;
                const tooltip = document.createElement('span');
                tooltip.id = `${input.id}-password-help`;
                tooltip.setAttribute('role', 'tooltip');
                tooltip.hidden = true;
                tooltip.textContent = 'Requisito: mínimo 6 caracteres. Consejos: usa al menos 15 caracteres, elige una frase larga y evita claves comunes o reutilizadas.';
                Object.assign(tooltip.style, {position: 'absolute', zIndex: '20', bottom: '100%', left: '0', maxWidth: 'min(320px, 85vw)', padding: '10px 12px', borderRadius: '8px', background: '#18283c', color: '#fff', fontSize: '0.8rem', boxShadow: '0 3px 12px #0003'});
                input.parentElement.style.position = 'relative';
                input.parentElement.appendChild(tooltip);
                input.setAttribute('aria-describedby', `${input.getAttribute('aria-describedby') || ''} ${tooltip.id}`.trim());
                input.addEventListener('focus', () => { tooltip.hidden = false; });
                input.addEventListener('click', () => { tooltip.hidden = false; });
                input.addEventListener('blur', () => { tooltip.hidden = true; });
                input.addEventListener('keydown', event => { if (event.key === 'Escape') tooltip.hidden = true; });
            });
            if (form.id === 'registroForm') return;
            form.noValidate = true;
            form.addEventListener('submit', event => {
                if (!validate(form)) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                }
            }, true);
        });
    });
    return {validate, error};
})();
