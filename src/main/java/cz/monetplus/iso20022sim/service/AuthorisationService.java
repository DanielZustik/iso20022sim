package cz.monetplus.iso20022sim.service;

import org.springframework.stereotype.Service;

@Service
public class AuthorisationService {

    private final SchemaValidationService schemaValidationService;
    private final ApprovalDecisionService approvalDecisionService;
    private final ResponseTemplateService responseTemplateService;

    public AuthorisationService(
            SchemaValidationService schemaValidationService,
            ApprovalDecisionService approvalDecisionService,
            ResponseTemplateService responseTemplateService
    ) {
        this.schemaValidationService = schemaValidationService;
        this.approvalDecisionService = approvalDecisionService;
        this.responseTemplateService = responseTemplateService;
    }

    public String authorise(String requestXml) {
        schemaValidationService.validateRequest(requestXml);
        if (!approvalDecisionService.isApprovedByDefault()) {
            throw new IllegalStateException("Default authorisation decision must be approval in V1 tracer.");
        }

        String responseXml = responseTemplateService.approvedAuthorisationResponse();
        schemaValidationService.validateResponse(responseXml);
        return responseXml;
    }
}
