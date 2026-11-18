CREATE TABLE IF NOT EXISTS retenciones_horario (
    owner_key VARCHAR(36) NOT NULL PRIMARY KEY,
    token VARCHAR(36),
    court_id BIGINT,
    slot_date DATE,
    start_time TIME(6),
    expires_at DATETIME(6),
    status VARCHAR(16) NOT NULL,
    reservation_id BIGINT,
    INDEX idx_retencion_slot (court_id, slot_date, start_time, expires_at),
    CONSTRAINT fk_retencion_cancha FOREIGN KEY (court_id) REFERENCES canchas(id),
    CONSTRAINT fk_retencion_reserva FOREIGN KEY (reservation_id) REFERENCES alquileres(id)
) ENGINE=InnoDB;
