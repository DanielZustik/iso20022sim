package cz.monetplus.iso20022sim.service;

import cz.monetplus.iso20022sim.error.InvalidAuthorisationRequestException;
import cz.monetplus.iso20022sim.error.InvalidAuthorisationResponseException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;

@Service
public class SchemaValidationService {

    private static final String REQUEST_XSD = "classpath:xsd/iso20022/caaa.001.001.15.xsd";
    private static final String RESPONSE_XSD = "classpath:xsd/iso20022/caaa.002.001.15.xsd";

    private final Schema requestSchema;
    private final Schema responseSchema;

    public SchemaValidationService(ResourceLoader resourceLoader) throws SAXException, IOException {
        this.requestSchema = loadSchema(resourceLoader.getResource(REQUEST_XSD));
        this.responseSchema = loadSchema(resourceLoader.getResource(RESPONSE_XSD));
    }

    public void validateRequest(String xml) {
        validate(xml, requestSchema, true);
    }

    public void validateResponse(String xml) {
        validate(xml, responseSchema, false);
    }

    private static Schema loadSchema(Resource resource) throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (var inputStream = resource.getInputStream()) {
            return schemaFactory.newSchema(new StreamSource(inputStream));
        }
    }

    private static void validate(String xml, Schema schema, boolean request) {
        Validator validator = schema.newValidator();
        try {
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (Exception exception) {
            if (request) {
                String message = "Request is not a schema-valid caaa.001.001.15 document. " + exception.getMessage();
                throw new InvalidAuthorisationRequestException(message, exception);
            }
            String message = "Generated response is not a schema-valid caaa.002.001.15 document. " + exception.getMessage();
            throw new InvalidAuthorisationResponseException(message, exception);
        }
    }
}
