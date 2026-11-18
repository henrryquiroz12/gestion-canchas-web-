document.addEventListener('DOMContentLoaded', () => {
    const profileForm = document.getElementById('formPerfilCliente');
    if (!profileForm) return;

    // Cascading Ubicacion: Departamento -> Provincias (FASE 16.4.2)
    const deptSelect = document.getElementById('perfilDepartamento');
    const provSelect = document.getElementById('perfilProvincia');

    function populateProfileProvinces(deptKey, selectedProvince) {
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
            populateProfileProvinces(deptSelect.value, window.CLIENT_PROVINCE || '');
        } else {
            provSelect.disabled = true;
        }

        deptSelect.addEventListener('change', function () {
            // Requisito 23: Si cliente cambia Departamento, limpiar la Provincia anterior y obligar a seleccionar una provincia valida del nuevo departamento
            populateProfileProvinces(this.value, '');
        });
    }

    // Refactorizacion de mostrarExitoFormulario para Toast Global FASE 16.4.6
    function mostrarExitoFormulario(form, mensaje) {
        if (form) {
            const feedback = form.querySelector('.modal-feedback, .form-feedback');
            if (feedback) feedback.innerHTML = '';
        }
        if (window.FqsToast) {
            window.FqsToast.show(mensaje || 'Cambios guardados correctamente.', 'success');
        }
    }

    profileForm.addEventListener('submit', (e) => {
        e.preventDefault();
        limpiarErroresFormulario(profileForm);
        const submitBtn = profileForm.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        fetch(profileForm.action, { method: 'POST', body: new FormData(profileForm), headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(async (response) => {
            const data = await response.json();
            if (!response.ok) {
                mostrarErroresFormulario(profileForm, data);
                submitBtn.disabled = false;
                return;
            }
            mostrarExitoFormulario(profileForm, data.mensaje || 'Cambios guardados correctamente.');
            if (provSelect) {
                window.CLIENT_PROVINCE = provSelect.value;
            }
            setTimeout(() => submitBtn.disabled = false, 1600);
        })
        .catch(() => {
            submitBtn.disabled = false;
            mostrarErroresFormulario(profileForm, { global: 'Error de conexión. Intenta nuevamente.' });
        });
    });
});