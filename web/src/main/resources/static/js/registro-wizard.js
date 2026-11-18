/**
 * registro-wizard.js - Wizard de Registro Accesible en 3 Pasos (WCAG 2.2 AA)
 * - Control de pasos con teclado y lector de pantalla.
 * - Validación cliente previa con aria-describedby y foco controlado.
 * - Submit final único al backend.
 * - Auto-detección de errores de validación backend para posicionar el paso correcto.
 */
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('registroForm');
    if (!form) return;

    const step1 = document.getElementById('stepFieldset1');
    const step2 = document.getElementById('stepFieldset2');
    const step3 = document.getElementById('stepFieldset3');

    const btnNext1 = document.getElementById('btnNext1');
    const btnNext2 = document.getElementById('btnNext2');
    const btnPrev2 = document.getElementById('btnPrev2');
    const btnPrev3 = document.getElementById('btnPrev3');
    const btnSubmit = document.getElementById('btnSubmit');

    const progBar1 = document.getElementById('progBar1');
    const progBar2 = document.getElementById('progBar2');
    const progBar3 = document.getElementById('progBar3');
    const stepLabel = document.getElementById('wizardStepLabel');

    let currentStep = 1;

    function setStep(stepNumber, focusFirst = true) {
        currentStep = stepNumber;

        // Ocultar todos los fieldsets
        [step1, step2, step3].forEach((fs, idx) => {
            if (idx + 1 === stepNumber) {
                fs.hidden = false;
                fs.classList.add('active');
            } else {
                fs.hidden = true;
                fs.classList.remove('active');
            }
        });

        // Actualizar barras de progreso y atributos de accesibilidad
        if (stepNumber === 1) {
            progBar1.className = 'wizard-progress-bar active';
            progBar1.setAttribute('aria-current', 'step');
            progBar2.className = 'wizard-progress-bar';
            progBar2.removeAttribute('aria-current');
            progBar3.className = 'wizard-progress-bar';
            progBar3.removeAttribute('aria-current');
            if (stepLabel) stepLabel.textContent = 'Paso 1 de 3: Datos personales';
            if (focusFirst) {
                const el = document.getElementById('fullName');
                if (el) el.focus();
            }
        } else if (stepNumber === 2) {
            progBar1.className = 'wizard-progress-bar completed';
            progBar1.removeAttribute('aria-current');
            progBar2.className = 'wizard-progress-bar active';
            progBar2.setAttribute('aria-current', 'step');
            progBar3.className = 'wizard-progress-bar';
            progBar3.removeAttribute('aria-current');
            if (stepLabel) stepLabel.textContent = 'Paso 2 de 3: Contacto y ubicación';
            if (focusFirst) {
                const el = document.getElementById('phoneNumber');
                if (el) el.focus();
            }
        } else if (stepNumber === 3) {
            progBar1.className = 'wizard-progress-bar completed';
            progBar1.removeAttribute('aria-current');
            progBar2.className = 'wizard-progress-bar completed';
            progBar2.removeAttribute('aria-current');
            progBar3.className = 'wizard-progress-bar active';
            progBar3.setAttribute('aria-current', 'step');
            if (stepLabel) stepLabel.textContent = 'Paso 3 de 3: Acceso';
            if (focusFirst) {
                const el = document.getElementById('email');
                if (el) el.focus();
            }
        }
    }

    function clearFieldError(inputEl) {
        if (!inputEl) return;
        inputEl.classList.remove('is-invalid');
        const errSpan = document.getElementById(inputEl.id + 'Error');
        if (errSpan) {
            errSpan.textContent = '';
        }
    }

    function showFieldError(inputEl, message) {
        if (!inputEl) return;
        inputEl.classList.add('is-invalid');
        let errSpan = document.getElementById(inputEl.id + 'Error');
        if (!errSpan) {
            errSpan = document.createElement('span');
            errSpan.id = inputEl.id + 'Error';
            errSpan.className = 'error-text text-danger small mt-1 d-block';
            inputEl.parentNode.appendChild(errSpan);
        }
        errSpan.textContent = message;
    }

    function validateStep1() {
        return window.AccountValidation.validate(step1);
    }

    function validateStep2() {
        let valid = window.AccountValidation.validate(step2);
        let firstInvalid = null;

        const dept = document.getElementById('department');
        clearFieldError(dept);
        if (!dept || !dept.value) {
            showFieldError(dept, 'Selecciona tu departamento.');
            valid = false;
            if (!firstInvalid) firstInvalid = dept;
        }

        const prov = document.getElementById('province');
        clearFieldError(prov);
        if (!prov || !prov.value || !prov.value.trim()) {
            showFieldError(prov, 'Selecciona tu provincia.');
            valid = false;
            if (!firstInvalid) firstInvalid = prov;
        }

        if (!valid && firstInvalid) {
            firstInvalid.focus();
        }
        return valid;
    }

    function validateStep3() {
        let valid = window.AccountValidation.validate(step3);
        let firstInvalid = null;

        const username = document.getElementById('username');
        clearFieldError(username);
        if (!username || !username.value.trim()) {
            showFieldError(username, 'El usuario es obligatorio.');
            valid = false;
            if (!firstInvalid) firstInvalid = username;
        } else if (username.value.trim().length < 4 || username.value.trim().length > 30) {
            showFieldError(username, 'El usuario debe tener entre 4 y 30 caracteres.');
            valid = false;
            if (!firstInvalid) firstInvalid = username;
        } else if (!/^[a-zA-Z0-9._]+$/.test(username.value.trim())) {
            showFieldError(username, 'Solo letras, números, puntos y guiones bajos.');
            valid = false;
            if (!firstInvalid) firstInvalid = username;
        }

        const password = document.getElementById('password');
        clearFieldError(password);
        if (!password || !password.value) {
            showFieldError(password, 'La contraseña es obligatoria.');
            valid = false;
            if (!firstInvalid) firstInvalid = password;
        } else if (password.value.length < 6) {
            showFieldError(password, 'La contraseña debe tener al menos 6 caracteres.');
            valid = false;
            if (!firstInvalid) firstInvalid = password;
        }

        const confirmPassword = document.getElementById('confirmPassword');
        clearFieldError(confirmPassword);
        if (!confirmPassword || !confirmPassword.value) {
            showFieldError(confirmPassword, 'Debe confirmar la contraseña.');
            valid = false;
            if (!firstInvalid) firstInvalid = confirmPassword;
        } else if (password && password.value !== confirmPassword.value) {
            showFieldError(confirmPassword, 'Las contraseñas no coinciden.');
            valid = false;
            if (!firstInvalid) firstInvalid = confirmPassword;
        }

        if (!valid && firstInvalid) {
            firstInvalid.focus();
        }
        return valid;
    }

    let dniPending = false;
    let submitting = false;

    async function checkDni() {
        if (dniPending) return false;
        const dni = document.getElementById('dni');
        const checkedValue = dni.value;
        dniPending = true;
        btnNext1.disabled = true;
        btnSubmit.disabled = true;
        form.setAttribute('aria-busy', 'true');
        try {
            const url = new URL(form.dataset.dniUrl, window.location.href);
            url.searchParams.set('dni', checkedValue);
            const response = await fetch(url, {cache: 'no-store', headers: {'Accept': 'application/json'}});
            const data = await response.json();
            if (dni.value !== checkedValue) return false;
            if (!response.ok || data.available !== true) {
                window.AccountValidation.error(dni, data.dni || 'El DNI ya se encuentra registrado.');
                setStep(1, false);
                dni.focus();
                return false;
            }
            return true;
        } catch (error) {
            window.AccountValidation.error(dni, 'No se pudo comprobar el DNI. Intenta nuevamente.');
            setStep(1, false);
            dni.focus();
            return false;
        } finally {
            dniPending = false;
            btnNext1.disabled = false;
            btnSubmit.disabled = false;
            form.removeAttribute('aria-busy');
        }
    }

    async function nextStep1() {
        if (validateStep1() && await checkDni() && validateStep1()) setStep(2, true);
    }

    btnNext1.addEventListener('click', nextStep1);
    btnPrev2.addEventListener('click', () => setStep(1, true));
    btnNext2.addEventListener('click', () => {
        if (validateStep2()) setStep(3, true);
    });
    btnPrev3.addEventListener('click', () => setStep(2, true));

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (dniPending || submitting) return;
        if (currentStep === 1) {
            await nextStep1();
            return;
        }
        if (currentStep === 2) {
            if (validateStep2()) setStep(3, true);
            return;
        }
        const validators = [validateStep1, validateStep2, validateStep3];
        for (let index = 0; index < validators.length; index++) {
            setStep(index + 1, false);
            if (!validators[index]()) return;
        }
        if (!await checkDni()) return;
        // Recheck fields changed during the request; Spring also validates on submit.
        for (let index = 0; index < validators.length; index++) {
            setStep(index + 1, false);
            if (!validators[index]()) return;
        }
        submitting = true;
        btnSubmit.disabled = true;
        HTMLFormElement.prototype.submit.call(form);
    });

    // Toggle de visibilidad de contraseña
    const toggleButtons = document.querySelectorAll('.toggle-pass-btn');
    toggleButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const input = document.getElementById(targetId);
            if (input) {
                const isPassword = input.getAttribute('type') === 'password';
                input.setAttribute('type', isPassword ? 'text' : 'password');
                this.textContent = isPassword ? 'Ocultar' : 'Mostrar';
            }
        });
    });

    // Auto-detección de errores de validación backend al recargar la página
    const hasStep3Errors = step3.querySelectorAll('.invalid-feedback:not(:empty), .error-text:not(:empty), .is-invalid').length > 0;
    const hasStep2Errors = step2.querySelectorAll('.invalid-feedback:not(:empty), .error-text:not(:empty), .is-invalid').length > 0;
    const hasStep1Errors = step1.querySelectorAll('.invalid-feedback:not(:empty), .error-text:not(:empty), .is-invalid').length > 0;

    if (hasStep1Errors) {
        setStep(1, false);
    } else if (hasStep2Errors) {
        setStep(2, false);
    } else if (hasStep3Errors) {
        setStep(3, false);
    } else {
        setStep(1, false);
    }

    // Cascading Ubicacion: Departamento -> Provincias (FASE 16.4.2)
    const deptSelect = document.getElementById('department');
    const provSelect = document.getElementById('province');

    function populateProvinces(deptKey, selectedProvince) {
        if (!provSelect) return;
        provSelect.innerHTML = '';

        if (!deptKey || !window.PROVINCIAS_MAP || !window.PROVINCIAS_MAP[deptKey]) {
            provSelect.disabled = true;
            const defOpt = document.createElement('option');
            defOpt.value = '';
            defOpt.textContent = 'Selecciona primero un departamento';
            provSelect.appendChild(defOpt);
            return;
        }

        provSelect.disabled = false;
        const placeholderOpt = document.createElement('option');
        placeholderOpt.value = '';
        placeholderOpt.textContent = 'Selecciona tu provincia';
        provSelect.appendChild(placeholderOpt);

        const provinces = window.PROVINCIAS_MAP[deptKey] || [];
        provinces.forEach(prov => {
            const opt = document.createElement('option');
            opt.value = prov;
            opt.textContent = prov;
            if (selectedProvince && selectedProvince.trim().toLowerCase() === prov.toLowerCase()) {
                opt.selected = true;
            }
            provSelect.appendChild(opt);
        });
    }

    if (deptSelect && provSelect) {
        if (deptSelect.value) {
            const currentSelected = provSelect.value || window.INITIAL_PROVINCE || '';
            populateProvinces(deptSelect.value, currentSelected);
        } else {
            provSelect.disabled = true;
        }

        deptSelect.addEventListener('change', function () {
            populateProvinces(this.value, '');
            clearFieldError(deptSelect);
            clearFieldError(provSelect);
        });

        provSelect.addEventListener('change', function () {
            clearFieldError(provSelect);
        });
    }
});
