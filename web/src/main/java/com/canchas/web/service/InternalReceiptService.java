package com.canchas.web.service;

import com.canchas.web.model.InternalReceipt;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface InternalReceiptService {

    InternalReceipt generate(Long reservationId);
    InternalReceipt findByReservationId(Long reservationId);
    List<InternalReceipt> findByDateRange(LocalDate start, LocalDate end);
    List<InternalReceipt> findAll();
    Page<InternalReceipt> findAllPaginated(int page, int size);
    InternalReceipt findById(Long id);
    void cancel(Long id);
    byte[] generatePdf(Long id);
}
