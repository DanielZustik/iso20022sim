package cz.monetplus.iso20022sim.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ResponseTemplateService {

    private static final String APPROVED_RESPONSE_PATH = "classpath:samples/caaa.002.001.15-approved.xml";
    private final String approvedResponse;

    public ResponseTemplateService(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(APPROVED_RESPONSE_PATH);
        try (var inputStream = resource.getInputStream()) {
            this.approvedResponse = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String approvedAuthorisationResponse() {
        return approvedResponse;
    }
}
