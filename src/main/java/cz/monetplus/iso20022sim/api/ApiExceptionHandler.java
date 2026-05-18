package cz.monetplus.iso20022sim.api;

import cz.monetplus.iso20022sim.error.InvalidAuthorisationRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidAuthorisationRequestException.class)
    public ResponseEntity<String> handleInvalidAuthorisationRequest(InvalidAuthorisationRequestException exception) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(exception.getMessage());
    }
}
