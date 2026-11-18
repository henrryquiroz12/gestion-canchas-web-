document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('clientSearchForm');
    if (!form) return;
    const criterion = document.getElementById('clientSearchCriterion');
    const input = document.getElementById('clientSearchValue');
    const results = document.getElementById('clientSearchResults');
    const error = document.getElementById('clientSearchError');
    const status = document.getElementById('clientSearchStatus');
    const reservationSearch = form.dataset.resultType === 'reservations';
    const emptyMessage = reservationSearch ? 'No se encontraron reservas con ese criterio.' : 'No se encontraron clientes con ese criterio.';
    let timer;
    let request;
    let version = 0;

    function cell(row, text, className = '') {
        const td = document.createElement('td');
        td.textContent = text || '';
        td.className = className;
        row.appendChild(td);
        return td;
    }

    function render(clients) {
        const rows = document.createDocumentFragment();
        clients.forEach(client => {
            const row = document.createElement('tr');
            const name = cell(row, client.fullName);
            if (client.dni) {
                const dni = document.createElement('small');
                dni.className = 'd-block text-muted';
                dni.textContent = `DNI: ${client.dni}`;
                name.appendChild(dni);
            }
            cell(row, client.username);
            cell(row, client.email);
            cell(row, [client.phoneNumber, client.department, client.province].filter(Boolean).join(' · '));
            const state = document.createElement('span');
            state.className = `badge-estado ${client.active ? 'badge-activo' : 'badge-inactivo'}`;
            state.textContent = client.active ? 'Activo' : 'Inactivo';
            cell(row, '').appendChild(state);
            const action = document.createElement('form');
            action.method = 'post';
            action.action = `${form.dataset.clientsUrl}/${client.id}/estado`;
            action.className = 'd-inline';
            action.appendChild(form.querySelector('input[type="hidden"]').cloneNode(true));
            const active = document.createElement('input');
            active.type = 'hidden';
            active.name = 'activo';
            active.value = String(!client.active);
            action.appendChild(active);
            const button = document.createElement('button');
            button.type = 'submit';
            button.className = `btn-icon ${client.active ? 'btn-icon--danger' : 'btn-icon--success'}`;
            button.title = client.active ? 'Desactivar cliente' : 'Activar cliente';
            button.setAttribute('aria-label', button.title);
            const icon = document.createElement('i');
            icon.className = client.active ? 'bi bi-person-x' : 'bi bi-person-check';
            icon.setAttribute('aria-hidden', 'true');
            button.appendChild(icon);
            action.appendChild(button);
            cell(row, '').appendChild(action);
            rows.appendChild(row);
        });
        results.replaceChildren(rows);
    }

    function renderReservations(reservations) {
        const rows = document.createDocumentFragment();
        reservations.forEach(reservation => {
            const row = document.createElement('tr');
            cell(row, `RES-${reservation.id}`);
            const name = cell(row, reservation.fullName);
            if (reservation.dni) {
                const dni = document.createElement('small');
                dni.className = 'd-block text-muted';
                dni.textContent = `DNI: ${reservation.dni}`;
                name.appendChild(dni);
            }
            cell(row, reservation.courtName);
            cell(row, reservation.date.split('-').reverse().join('/'));
            cell(row, `${reservation.startTime.slice(0, 5)} - ${reservation.endTime.slice(0, 5)}`);
            cell(row, `S/ ${Number(reservation.amount).toFixed(2)}`);
            const badge = document.createElement('span');
            badge.className = `badge-estado badge-${reservation.status.toLowerCase()}`;
            badge.textContent = reservation.status;
            cell(row, '').appendChild(badge);
            const detail = document.createElement('a');
            detail.className = 'btn btn-outline btn-sm';
            detail.href = `${form.dataset.detailsUrl}/${reservation.id}`;
            detail.textContent = 'Ver Detalle';
            cell(row, '').appendChild(detail);
            rows.appendChild(row);
        });
        results.replaceChildren(rows);
    }

    function search() {
        const current = ++version;
        clearTimeout(timer);
        request?.abort();
        results.replaceChildren();
        results.removeAttribute('aria-busy');
        error.textContent = '';
        input.setAttribute('aria-invalid', 'false');
        const value = input.value;
        if (!value.trim()) {
            status.textContent = reservationSearch ? 'Escribe un DNI o nombre del cliente para buscar sus reservas.' : 'Escribe un DNI o nombre para buscar.';
            return;
        }
        const valid = criterion.value === 'DNI' ? /^[0-9]{1,8}$/.test(value)
            : value.length <= 100 && /^[\p{L}\p{M} ]+$/u.test(value);
        if (!valid) {
            error.textContent = criterion.value === 'DNI' ? 'Escribe de 1 a 8 dígitos, sin espacios ni signos.'
                : 'Usa solo letras y espacios para buscar por nombre o apellidos.';
            input.setAttribute('aria-invalid', 'true');
            status.textContent = '';
            return;
        }
        status.textContent = 'Buscando…';
        timer = setTimeout(async () => {
            request = new AbortController();
            results.setAttribute('aria-busy', 'true');
            const url = new URL(form.dataset.searchUrl, window.location.href);
            url.searchParams.set('criterio', criterion.value);
            url.searchParams.set('valor', value);
            try {
                const response = await fetch(url, {signal: request.signal, cache: 'no-store', headers: {Accept: 'application/json'}});
                const data = await response.json();
                if (current !== version) return;
                if (!response.ok) throw new Error(data.valor || 'No se pudo completar la búsqueda.');
                if (!Array.isArray(data)) throw new Error('Respuesta de búsqueda inválida.');
                if (reservationSearch) renderReservations(data);
                else render(data);
                status.textContent = data.length ? `${data.length} coincidencias (máximo 10).`
                    : emptyMessage;
            } catch (failure) {
                if (current !== version || failure.name === 'AbortError') return;
                error.textContent = failure.message || 'No se pudo completar la búsqueda.';
                status.textContent = '';
            } finally {
                if (current === version) results.removeAttribute('aria-busy');
            }
        }, 300);
    }

    form.addEventListener('submit', event => event.preventDefault());
    input.addEventListener('input', search);
    criterion.addEventListener('change', () => {
        input.maxLength = criterion.value === 'DNI' ? 8 : 100;
        input.inputMode = criterion.value === 'DNI' ? 'numeric' : 'text';
        input.placeholder = criterion.value === 'DNI' ? 'Escribe de 1 a 8 dígitos' : 'Escribe nombres o apellidos';
        input.value = '';
        search();
        input.focus();
    });
    document.getElementById('clearClientSearch').addEventListener('click', () => {
        input.value = '';
        search();
        input.focus();
    });
});
