package cz.monetplus.iso20022sim.api;

import cz.monetplus.iso20022sim.validation.AuthorisationRequestValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/authorisations")
public class AuthorisationApiController {

    private final AuthorisationRequestValidator requestValidator;

    public AuthorisationApiController(AuthorisationRequestValidator requestValidator) {
        this.requestValidator = requestValidator;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<String> submitAuthorisation(@RequestBody String xmlRequestBody) {
        requestValidator.validate(xmlRequestBody);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Authorisation request accepted for processing.");
    }
}
