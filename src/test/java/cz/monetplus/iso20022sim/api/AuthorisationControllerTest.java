package cz.monetplus.iso20022sim.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorisationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void approvedAuthorisationRequestReturnsSchemaValidResponse() throws Exception {
        String requestXml = readResource("samples/caaa.001.001.15-approved.xml");

        String responseXml = mockMvc.perform(
                        post("/api/authorisations")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(requestXml)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<Rspn>APPR</Rspn>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<AuthstnCd>")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThatCode(() -> validateResponseAgainstXsd(responseXml))
                .doesNotThrowAnyException();
    }

    @Test
    void schemaInvalidRequestReturnsUnprocessableEntity() throws Exception {
        String schemaInvalidRequest = readResource("samples/caaa.001.001.15-approved.xml")
                .replace("<AuthstnReq>", "");

        mockMvc.perform(
                        post("/api/authorisations")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(schemaInvalidRequest)
                )
                .andExpect(status().isUnprocessableEntity());
    }

    private static void validateResponseAgainstXsd(String responseXml) throws SAXException, IOException {
        var schemaResource = new ClassPathResource("xsd/iso20022/caaa.002.001.15.xsd");
        var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (var schemaInputStream = schemaResource.getInputStream()) {
            var schema = schemaFactory.newSchema(new StreamSource(schemaInputStream));
            var validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(responseXml)));
        }
    }

    private static String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
