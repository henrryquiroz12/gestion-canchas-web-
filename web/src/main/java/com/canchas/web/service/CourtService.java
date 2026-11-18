package com.canchas.web.service;

import com.canchas.web.model.Court;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CourtService {

    Court create(Court court);
    Court update(Long id, Court court);
    Court changeAvailability(Long id, boolean available);
    void delete(Long id);
    Court findById(Long id);
    Court lockById(Long id);
    List<Court> findAvailable();
    List<Court> findAll();
    Page<Court> findAllPaginated(int page, int size);
}
