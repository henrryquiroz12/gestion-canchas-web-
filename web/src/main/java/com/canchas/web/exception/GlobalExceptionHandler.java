package com.canchas.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public void handleResponseStatus(ResponseStatusException ex) {
        throw ex;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public RedirectView handleResourceNotFound(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        return redirigirAtras(request);
    }

    @ExceptionHandler(InvalidDateException.class)
    public RedirectView handleInvalidDate(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        return redirigirAtras(request);
    }

    @ExceptionHandler(TimeSlotUnavailableException.class)
    public RedirectView handleTimeSlotUnavailable(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        return redirigirAtras(request);
    }

    @ExceptionHandler(DuplicateDataException.class)
    public RedirectView handleDuplicateData(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        return redirigirAtras(request);
    }

    @ExceptionHandler({OperationNotAllowedException.class, InvalidReservationStatusException.class})
    public RedirectView handleOperationNotAllowed(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        return redirigirAtras(request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public RedirectView manejarAccesoDenegado(AccessDeniedException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", "No tienes permisos para realizar esta acción.");
        return new RedirectView("/error/access-denied");
    }

    @ExceptionHandler(Exception.class)
    public RedirectView manejarErrorGeneral(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensajeError", "Ocurrió un error inesperado. Intenta nuevamente.");
        return redirigirAtras(request);
    }

    private RedirectView redirigirAtras(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return new RedirectView(referer != null && !referer.isBlank() ? referer : "/");
    }
}