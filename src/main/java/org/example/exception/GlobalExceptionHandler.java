package org.example.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException exception, Model model) {
        return error(model, 404, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(AccessDeniedException exception, Model model) {
        return error(model, 403, exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(BusinessException exception, Model model) {
        return error(model, 409, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String integrity(Model model) {
        return error(
                model,
                409,
                "Operacja koliduje z istniejącymi danymi. Odśwież stronę i spróbuj ponownie.");
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(Model model) {
        return error(model, 400, "Przesłano niepoprawne dane formularza.");
    }

    private String error(Model model, int status, String message) {
        model.addAttribute("status", status);
        model.addAttribute("message", message);
        return "error";
    }
}
