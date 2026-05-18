package cz.monetplus.iso20022sim.api;

import cz.monetplus.iso20022sim.service.AuthorisationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authorisations")
public class AuthorisationController {

    private final AuthorisationService authorisationService;

    public AuthorisationController(AuthorisationService authorisationService) {
        this.authorisationService = authorisationService;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> authorise(@RequestBody String requestXml) {
        String responseXml = authorisationService.authorise(requestXml);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(responseXml);
    }
}
