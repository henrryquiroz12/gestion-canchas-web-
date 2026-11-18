package com.canchas.web.service;

import com.canchas.web.dto.CourtBlockGroupDTO;
import com.canchas.web.model.CourtBlock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CourtBlockService {
    List<CourtBlock> findActiveByCourtAndDate(Long courtId, LocalDate date);
    List<CourtBlock> findAllActive();
    List<CourtBlockGroupDTO> findGroupedBlocks();
    CourtBlock blockSlot(Long courtId, LocalDate date, LocalTime startTime, String reason);
    List<CourtBlock> blockRange(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime, String reason);
    List<CourtBlock> blockFullDay(Long courtId, LocalDate date, String reason);
    void unblockSlot(Long courtId, LocalDate date, LocalTime startTime);
    void unblockById(Long id);
    void unblockByIds(List<Long> ids);
}
