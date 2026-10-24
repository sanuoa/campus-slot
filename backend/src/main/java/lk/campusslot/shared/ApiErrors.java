package lk.campusslot.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> domain(ApiException ex) { return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage())); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        var error = ex.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(Map.of("message", error.getField() + ": " + error.getDefaultMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception ex) { return ResponseEntity.badRequest().body(Map.of("message", "Check the request fields and date format.")); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicate(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("message", "This conflicts with an existing record. Refresh availability and try again."));
    }
}
