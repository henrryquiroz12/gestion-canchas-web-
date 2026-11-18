document.addEventListener('DOMContentLoaded', () => {
    const banner = document.getElementById('reservationHoldBanner');
    if (!banner) return;
    const timer = banner.querySelector('[data-hold-timer]');
    const message = banner.querySelector('[data-hold-message]');
    const cancel = banner.querySelector('[data-hold-cancel]');
    let state = banner.dataset.state;
    let deadline = performance.now() + Number(banner.dataset.remaining) * 1000;
    let refreshing = false;
    let expiryChecked = false;
    let revision = 0;

    function tick() {
        const remaining = Math.max(0, Math.ceil((deadline - performance.now()) / 1000));
        timer.textContent = `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}`;
        const active = state === 'ACTIVE' && remaining > 0;
        cancel.disabled = !active;
        document.querySelectorAll('[data-hold-confirm]').forEach(button => { button.disabled = !active; });
        banner.querySelector('[data-hold-resume]').hidden = !active;
        if (!active) {
            message.textContent = state === 'CANCELLED' ? 'Cancelaste la retención. El horario vuelve a estar disponible.'
                : state === 'CONFIRMED' ? 'La retención ya se convirtió en una reserva.'
                : 'El plazo de la retención terminó. Consulta disponibilidad para elegir un horario de nuevo.';
            if (state === 'ACTIVE' && !expiryChecked) {
                expiryChecked = true;
                refresh();
            }
        }
    }

    async function refresh() {
        if (refreshing) return;
        refreshing = true;
        const requestedRevision = revision;
        try {
            const response = await fetch(banner.dataset.statusUrl, {cache: 'no-store', headers: {Accept: 'application/json'}});
            if (!response.ok) return;
            const current = await response.json();
            if (requestedRevision !== revision) return;
            // Never let a stale tab cancel or confirm a different selection.
            state = current.token === banner.dataset.token ? current.status : 'EXPIRED';
            deadline = performance.now() + Number(current.remainingSeconds || 0) * 1000;
            tick();
        } catch (_) {
            // The server's absolute deadline remains authoritative during a network failure.
        } finally { refreshing = false; }
    }

    cancel.addEventListener('click', async () => {
        revision++;
        cancel.disabled = true;
        const data = new URLSearchParams({token: banner.dataset.token});
        data.set(banner.dataset.csrfName, banner.dataset.csrfToken);
        try {
            const response = await fetch(banner.dataset.cancelUrl, {method: 'POST', body: data, headers: {Accept: 'application/json'}});
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || 'No se pudo cancelar la retención.');
            state = 'CANCELLED';
            deadline = performance.now();
            tick();
        } catch (error) {
            message.textContent = error.message;
            cancel.disabled = false;
        }
    });
    tick();
    setInterval(tick, 1000);
    setInterval(refresh, 15000);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
});
