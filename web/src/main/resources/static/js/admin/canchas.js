document.addEventListener('DOMContentLoaded', () => {
    const courtForm = document.getElementById('formCancha');
    const courtModalEl = document.getElementById('modalCancha');
    const disableModalEl = document.getElementById('modalConfirmarDeshabilitar');
    const confirmDisableForm = document.getElementById('formConfirmarDeshabilitar');

    let courtModal = null;
    let disableModal = null;

    if (courtModalEl && typeof bootstrap !== 'undefined') {
        courtModal = new bootstrap.Modal(courtModalEl);
    }
    if (disableModalEl && typeof bootstrap !== 'undefined') {
        disableModal = new bootstrap.Modal(disableModalEl);
    }

    if (courtForm && typeof enviarFormularioAjax === 'function') {
        enviarFormularioAjax(courtForm, 'Cancha actualizada correctamente.');
    }

    // Modal Edit Price
    document.querySelectorAll('.btn-editar-cancha').forEach(btn => {
        btn.addEventListener('click', () => {
            const data = btn.dataset;
            if (courtForm) {
                courtForm.action = `/admin/canchas/${data.id}/editar`;
                if (typeof limpiarErroresFormulario === 'function') {
                    limpiarErroresFormulario(courtForm);
                }
            }
            const nameInput = document.getElementById('canchaNombre');
            const typeInput = document.getElementById('canchaTipoTexto');
            const priceInput = document.getElementById('canchaPrecio');
            const descInput = document.getElementById('canchaDescripcion');

            if (nameInput) nameInput.value = data.nombre || '';
            if (typeInput) typeInput.value = data.tipo === 'GRANDE' ? 'GRANDE (Fútbol 7)' : 'NORMAL (Fútbol 6)';
            if (priceInput) priceInput.value = data.precio ? parseFloat(data.precio).toFixed(2) : '';
            if (descInput) descInput.value = data.descripcion || '';

            if (courtModal) {
                courtModal.show();
            }
        });
    });

    // Modal Confirmation Disable
    document.querySelectorAll('.btn-abrir-deshabilitar').forEach(btn => {
        btn.addEventListener('click', () => {
            const data = btn.dataset;
            if (confirmDisableForm) {
                confirmDisableForm.action = `/admin/canchas/${data.id}/estado`;
            }
            const titleEl = document.getElementById('textoTituloDeshabilitar');
            const messageEl = document.getElementById('mensajeDeshabilitar');

            if (titleEl) titleEl.textContent = `¿Deshabilitar ${data.nombre}?`;
            if (messageEl) {
                messageEl.textContent = `Los clientes dejarán de poder realizar nuevas reservas en ${data.nombre}. Las reservas históricas no se eliminarán.`;
            }

            if (disableModal) {
                disableModal.show();
            }
        });
    });
});
