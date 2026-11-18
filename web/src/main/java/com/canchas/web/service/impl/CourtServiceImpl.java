package com.canchas.web.service.impl;

import com.canchas.web.exception.OperationNotAllowedException;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.Court;
import com.canchas.web.repository.CourtRepository;
import com.canchas.web.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service("courtService")
@RequiredArgsConstructor
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;

    @Override
    @Transactional
    public Court create(Court court) {
        court.setAvailable(true);
        return courtRepository.save(court);
    }

    @Override
    @Transactional
    public Court update(Long id, Court courtData) {
        Court court = findById(id);
        if (courtData.getHourlyRate() == null || courtData.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio por hora debe ser mayor a 0.");
        }
        court.setHourlyRate(courtData.getHourlyRate());
        if (courtData.getDescription() != null) {
            court.setDescription(courtData.getDescription());
        }
        if (courtData.getImageUrl() != null) {
            court.setImageUrl(courtData.getImageUrl());
        }
        return courtRepository.save(court);
    }

    @Override
    @Transactional
    public Court changeAvailability(Long id, boolean available) {
        // Serialize with reservation creation, which locks this same court row.
        Court court = courtRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        court.setAvailable(available);
        return courtRepository.save(court);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        throw new OperationNotAllowedException("Las canchas físicas son fijas y no se pueden eliminar físicamente. Utilice la opción de deshabilitar.");
    }

    @Override
    public Court findById(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada con id: " + id));
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Court lockById(Long id) {
        return courtRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
    }

    @Override
    public List<Court> findAvailable() {
        return courtRepository.findByAvailableTrue();
    }

    @Override
    public List<Court> findAll() {
        return courtRepository.findAll();
    }

    @Override
    public Page<Court> findAllPaginated(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        return courtRepository.findAllPaginated(PageRequest.of(pageNumber, pageSize));
    }
}
