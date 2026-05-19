package cz.monetplus.iso20022sim.authorisation;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/authorisations")
public class AuthorisationController {

    private final AuthorisationService authorisationService;

    public AuthorisationController(AuthorisationService authorisationService) {
        this.authorisationService = authorisationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE) //pouze scheckuje ze header v dosle http ma tez xml.application
    public ResponseEntity<String> authorise(@RequestBody String requestXml) {
        return ResponseEntity.ok(authorisationService.approveAuthorisation(requestXml));
    }

    @ExceptionHandler(MalformedXmlException.class) //vaze se pouze k tomuto controlleru
    public ResponseEntity<String> handleMalformedXml(MalformedXmlException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.TEXT_PLAIN)
                .body(exception.getMessage());
    }

    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<String> handleSchemaValidation(SchemaValidationException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(exception.getMessage());
    }
}