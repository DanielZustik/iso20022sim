package cz.monetplus.iso20022sim.api;

import cz.monetplus.iso20022sim.validation.MalformedXmlException;
import cz.monetplus.iso20022sim.validation.SchemaInvalidXmlException;
import cz.monetplus.iso20022sim.validation.UnsupportedMessageVersionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MalformedXmlException.class)
    public ResponseEntity<ApiErrorResponse> malformedXml(MalformedXmlException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed XML", ex.getMessage());
    }

    @ExceptionHandler(SchemaInvalidXmlException.class)
    public ResponseEntity<ApiErrorResponse> schemaInvalid(SchemaInvalidXmlException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Schema Invalid XML", ex.getMessage());
    }

    @ExceptionHandler(UnsupportedMessageVersionException.class)
    public ResponseEntity<ApiErrorResponse> unsupportedVersion(UnsupportedMessageVersionException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported ISO 20022 Message Version", ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message) {
        ApiErrorResponse body = new ApiErrorResponse(status.value(), error, message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
