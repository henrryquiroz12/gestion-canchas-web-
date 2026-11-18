package com.canchas.web.controller;

import com.canchas.web.dto.ReservationHoldView;
import com.canchas.web.security.VisitorHoldSession;
import com.canchas.web.service.ReservationHoldService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes = {AuthController.class, PublicCourtController.class})
@RequiredArgsConstructor
public class VisitorHoldAdvice {
    private final ReservationHoldService holds;
    @ModelAttribute("visitorHold")
    public ReservationHoldView hold(HttpServletRequest request) {
        return holds.current(VisitorHoldSession.existingOwner(request.getSession(false)));
    }
}
