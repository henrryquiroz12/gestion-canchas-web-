document.addEventListener('DOMContentLoaded', function() {
    
    const dateInput = document.getElementById('fechaReserva');
    const slotsContainer = document.getElementById('slotsContainer');
    const reservationSummary = document.getElementById('resumenReserva');
    const btnContinue = document.getElementById('btnContinuar');
    
    if (!dateInput || !slotsContainer) return;

    let selectedSlot = null;

    // Initialize with today's date in America/Lima if empty
    if (!dateInput.value && typeof FECHA_HOY_LIMA !== 'undefined') {
        dateInput.value = FECHA_HOY_LIMA;
    }

    // Load slots on startup
    loadAvailability(dateInput.value);

    // Event listener for date change
    dateInput.addEventListener('change', function() {
        if (this.value) {
            loadAvailability(this.value);
        }
    });

    function loadAvailability(date) {
        slotsContainer.innerHTML = '<p class="text-center" style="padding: 20px 0;">Cargando horarios...</p>';
        if (btnContinue) btnContinue.disabled = true;
        if (reservationSummary) reservationSummary.style.display = 'none';
        selectedSlot = null;

        fetch(`/api/canchas/${CANCHA_ID}/disponibilidad?fecha=${date}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Error en la respuesta del servidor');
                }
                return response.json();
            })
            .then(slots => {
                renderSlots(slots);
            })
            .catch(error => {
                console.error('Error cargando horarios:', error);
                slotsContainer.innerHTML = '<p class="text-center text-danger" style="padding: 20px 0;">No pudimos consultar los horarios. Intenta nuevamente.</p>';
            });
    }

    function formatTime(timeArrayOrString) {
        if (Array.isArray(timeArrayOrString)) {
            const h = String(timeArrayOrString[0]).padStart(2, '0');
            const m = String(timeArrayOrString[1] || 0).padStart(2, '0');
            return `${h}:${m}`;
        } else if (typeof timeArrayOrString === 'string') {
            return timeArrayOrString.substring(0, 5);
        }
        return timeArrayOrString;
    }

    function formatEndTimeVisual(rawEnd) {
        let endStr = formatTime(rawEnd);
        if (endStr === '00:00') {
            return '24:00';
        }
        return endStr;
    }

    function formatSlotRange(rawStart, rawEnd) {
        const start = formatTime(rawStart);
        const end = formatEndTimeVisual(rawEnd);
        return `${start} - ${end}`;
    }

    function formatDate(isoDate) {
        if (!isoDate) return '';
        const parts = isoDate.split('-');
        if (parts.length !== 3) return isoDate;
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }

    function renderSlots(slots) {
        slotsContainer.innerHTML = '';

        if (!slots || slots.length === 0) {
            slotsContainer.innerHTML = '<p class="text-center text-muted" style="padding: 20px 0;">No hay horarios disponibles para esta fecha.</p>';
            return;
        }

        let allOccupied = true;

        slots.forEach(slot => {
            const status = (slot.status || slot.estado || '').toUpperCase();
            const rawStartTime = slot.startTime || slot.horaInicio;
            const rawEndTime = slot.endTime || slot.horaFin;
            const totalPrice = (slot.totalPrice !== undefined) ? slot.totalPrice : slot.precioTotal;
            const lightingSurcharge = (slot.lightingSurcharge !== undefined) ? slot.lightingSurcharge : slot.recargoIluminacion;

            const btn = document.createElement('button');
            btn.className = `slot-btn ${status.toLowerCase()}`;
            btn.type = 'button';
            
            const rangeText = formatSlotRange(rawStartTime, rawEndTime);
            
            let html = `<span class="slot-time">${rangeText}</span>`;
            let priceStr = `S/${totalPrice}`;
            if (lightingSurcharge > 0) {
                priceStr += ' <i class="bi bi-lightbulb-fill" style="color: #F59E0B;" title="Incluye iluminación nocturna"></i>';
            }
            
            html += `<span class="slot-price">${priceStr}</span>`;
            btn.innerHTML = html;

            if (status === 'DISPONIBLE') {
                allOccupied = false;
                btn.title = `Reservar de ${rangeText} por S/${totalPrice}`;
                btn.onclick = () => selectSlot(btn, slot, rawStartTime, rawEndTime);
            } else if (status === 'OCUPADO') {
                btn.disabled = true;
                btn.title = 'Horario no disponible';
            } else if (status === 'BLOQUEADO') {
                btn.disabled = true;
                btn.title = 'Horario bloqueado por administración';
            }

            slotsContainer.appendChild(btn);
        });

        if (allOccupied && slots.length > 0) {
            const msg = document.createElement('p');
            msg.className = 'text-center text-muted';
            msg.style.gridColumn = '1 / -1';
            msg.textContent = 'No quedan horarios disponibles para esta fecha.';
            slotsContainer.appendChild(msg);
        }
    }

    function selectSlot(btn, slot, rawStart, rawEnd) {
        document.querySelectorAll('.slot-btn.seleccionado').forEach(b => {
            b.classList.remove('seleccionado');
            b.setAttribute('aria-pressed', 'false');
        });

        btn.classList.add('seleccionado');
        btn.setAttribute('aria-pressed', 'true');
        selectedSlot = slot;

        const basePrice = Number((slot.basePrice !== undefined) ? slot.basePrice : slot.precioBase);
        const surcharge = Number((slot.lightingSurcharge !== undefined) ? slot.lightingSurcharge : slot.recargoIluminacion);
        const totalPrice = Number((slot.totalPrice !== undefined) ? slot.totalPrice : slot.precioTotal);

        const summaryDate = document.getElementById('resumenFecha');
        if (summaryDate) summaryDate.textContent = formatDate(dateInput.value);

        const summaryTime = document.getElementById('resumenHorario');
        if (summaryTime) summaryTime.textContent = formatSlotRange(rawStart, rawEnd);

        const summaryBase = document.getElementById('resumenBase');
        if (summaryBase) summaryBase.textContent = `S/${basePrice.toFixed(2)}`;
        
        const rowLighting = document.getElementById('rowIluminacion');
        if (rowLighting) {
            if (surcharge > 0) {
                rowLighting.style.display = 'flex';
                const strongEl = rowLighting.querySelector('strong');
                if (strongEl) strongEl.textContent = `S/${surcharge.toFixed(2)}`;
            } else {
                rowLighting.style.display = 'none';
            }
        }
        
        const summaryTotal = document.getElementById('resumenTotal');
        if (summaryTotal) summaryTotal.textContent = `S/${totalPrice.toFixed(2)}`;
        
        if (reservationSummary) reservationSummary.style.display = 'block';
        if (btnContinue) btnContinue.disabled = false;
    }

    if (btnContinue) {
        btnContinue.addEventListener('click', function(e) {
            e.preventDefault();
            if (!selectedSlot) return;
            
            const date = dateInput.value;
            const rawStartTime = selectedSlot.startTime || selectedSlot.horaInicio;
            const startTime = formatTime(rawStartTime);
            
            const holdForm = document.getElementById('holdStartForm');
            if (holdForm) {
                holdForm.querySelector('[name="date"]').value = date;
                holdForm.querySelector('[name="startTime"]').value = startTime;
                btnContinue.disabled = true;
                holdForm.requestSubmit();
                return;
            }
            window.location.href = `/cliente/reservas/nueva?canchaId=${CANCHA_ID}&fecha=${date}&horaInicio=${startTime}`;
        });
    }
});