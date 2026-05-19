package cz.monetplus.iso20022sim.authorisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorisationApiTest {

    private static final String RESPONSE_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.002.001.15";

    @Autowired
    private MockMvc mockMvc; //bez toho by se dal controller testovat jen vlasntim serverem nebo ten necontrolovat a omezit se jen na test servisky

    private Schema requestSchema;
    private Schema responseSchema;

    @BeforeEach //pred kazdym testem
    void loadSchemas() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.requestSchema = schemaFactory.newSchema(new StreamSource(new ClassPathResource("xsd/caaa.001.001.15.xsd").getInputStream()));
        this.responseSchema = schemaFactory.newSchema(new StreamSource(new ClassPathResource("xsd/caaa.002.001.15.xsd").getInputStream()));
    }

    @Test
    void approvedAuthorisationReturnsSchemaValidResponseWithApprovalCode() throws Exception {
        String requestXml = new String(new ClassPathResource("samples/approved-authorisation-request.xml")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        validateAgainstSchema(requestXml, requestSchema);

        MvcResult mvcResult = mockMvc.perform(post("/api/authorisations") //fake HTTP POST request to endpoint /api/authorisations
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML) // req contentType
                        .content(requestXml) //http body
                )
                .andExpect(status().isOk()) // kontrola ze AuthorisationController vratil 200
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML)) // resp contentType
                .andReturn();

        String responseXml = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8); // http body

        validateAgainstSchema(responseXml, responseSchema);

        Document responseDocument = parseXml(responseXml);
        String responseCode = firstElementText(responseDocument, RESPONSE_NS, "Rspn");
        String approvalCode = firstElementText(responseDocument, RESPONSE_NS, "AuthstnCd");

        assertThat(responseCode).isEqualTo("APPR");
        assertThat(approvalCode).isNotBlank();
        assertThat(approvalCode.length()).isLessThanOrEqualTo(8);
    }

    private static void validateAgainstSchema(String xml, Schema schema) throws SAXException, IOException {
        schema.newValidator().validate(new StreamSource(new StringReader(xml)));
    }

    private static Document parseXml(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(xml)));
    }

    private static String firstElementText(Document document, String namespace, String localName) {
        NodeList nodeList = document.getElementsByTagNameNS(namespace, localName);
        assertThat(nodeList.getLength()).isGreaterThan(0);
        return nodeList.item(0).getTextContent();
    }
}
