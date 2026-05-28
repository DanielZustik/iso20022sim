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

    private static final String REQUEST_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.001.001.15";
    private static final String RESPONSE_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.002.001.15";
    private static final String RULE_AMOUNT_THRESHOLD = "AMOUNT_THRESHOLD";
    private static final String RULE_DENIED_CARD_IDENTIFIER = "DENIED_CARD_IDENTIFIER";
    private static final String RULE_DENIED_ACCEPTOR_OR_MERCHANT = "DENIED_ACCEPTOR_OR_MERCHANT_ID";

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
        String requestXml = loadApprovedRequestXml();
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

    @Test
    void amountThresholdRuleDeclinesWithSchemaValidResponseAndRuleReason() throws Exception {
        String requestXml = loadApprovedRequestXml()
                .replace("<TtlAmt>120.50</TtlAmt>", "<TtlAmt>1500.00</TtlAmt>");

        MvcResult mvcResult = submitAuthorisationExpectingOk(requestXml);
        String responseXml = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertDeclinedResponse(responseXml, RULE_AMOUNT_THRESHOLD);
    }

    @Test
    void deniedCardRuleDeclinesWithSchemaValidResponseAndRuleReason() throws Exception {
        String requestXml = loadApprovedRequestXml()
                .replace("4111111111111111", "4000000000000002");

        MvcResult mvcResult = submitAuthorisationExpectingOk(requestXml);
        String responseXml = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertDeclinedResponse(responseXml, RULE_DENIED_CARD_IDENTIFIER);
    }

    @Test
    void deniedMerchantRuleDeclinesWithSchemaValidResponseAndRuleReason() throws Exception {
        String requestXml = loadApprovedRequestXml()
                .replace("MERCHANT-001", "MERCHANT-BLOCKED");

        MvcResult mvcResult = submitAuthorisationExpectingOk(requestXml);
        String responseXml = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertDeclinedResponse(responseXml, RULE_DENIED_ACCEPTOR_OR_MERCHANT);
    }

    @Test
    void deniedAcceptorRuleDeclinesWithSchemaValidResponseAndRuleReason() throws Exception {
        String requestXml = loadApprovedRequestXml()
                .replace("POI-001", "POI-BLOCKED");

        MvcResult mvcResult = submitAuthorisationExpectingOk(requestXml);
        String responseXml = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertDeclinedResponse(responseXml, RULE_DENIED_ACCEPTOR_OR_MERCHANT);
    }

    @Test
    void ruleOrderingPrefersAmountThresholdOverOtherMatchingRules() throws Exception {
        String requestXml = loadApprovedRequestXml()
                .replace("<TtlAmt>120.50</TtlAmt>", "<TtlAmt>1500.00</TtlAmt>")
                .replace("4111111111111111", "4000000000000002")
                .replace("MERCHANT-001", "MERCHANT-BLOCKED")
                .replace("POI-001", "POI-BLOCKED");

        MvcResult mvcResult = submitAuthorisationExpectingOk(requestXml);
        String responseXml = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertDeclinedResponse(responseXml, RULE_AMOUNT_THRESHOLD);
    }

    @Test
    void malformedXmlReturnsBadRequestWithoutAuthorisationResponse() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.ALL)
                        .content("<Document"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn();
        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("not well-formed XML");
        assertThat(body).doesNotContain(RESPONSE_NS);
    }

    @Test
    void schemaInvalidRequestReturnsUnprocessableEntityWithoutAuthorisationResponse() throws Exception {
        String schemaInvalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:caaa.001.001.15">
                    <AccptrAuthstnReq/>
                </Document>
                """;

        MvcResult mvcResult = mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.ALL)
                        .content(schemaInvalidXml))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("not schema-valid caaa.001.001.15 XML");
        assertThat(body).doesNotContain(RESPONSE_NS);
    }

    @Test
    void unsupportedIsoMessageVersionReturnsUnprocessableEntityWithoutAuthorisationResponse() throws Exception {
        String unsupportedVersionXml = loadApprovedRequestXml()
                .replace(REQUEST_NS, "urn:iso:std:iso:20022:tech:xsd:caaa.001.001.14");

        MvcResult mvcResult = mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.ALL)
                        .content(unsupportedVersionXml))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("Unsupported ISO 20022 message/version namespace");
        assertThat(body).doesNotContain(RESPONSE_NS);
    }

    private MvcResult submitAuthorisationExpectingOk(String requestXml) throws Exception {
        return mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML)
                        .content(requestXml))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn();
    }

    private void assertDeclinedResponse(String responseXml, String expectedRuleReason) throws Exception {
        validateAgainstSchema(responseXml, responseSchema);

        Document responseDocument = parseXml(responseXml);
        String responseCode = firstElementText(responseDocument, RESPONSE_NS, "Rspn");
        String ruleReason = firstElementText(responseDocument, RESPONSE_NS, "RspnRsn");
        String addtlResponseInfo = firstElementText(responseDocument, RESPONSE_NS, "AddtlRspnInf");
        String approvalCode = optionalElementText(responseDocument, RESPONSE_NS, "AuthstnCd");

        assertThat(responseCode).isEqualTo("DECL");
        assertThat(ruleReason).isEqualTo(expectedRuleReason);
        assertThat(addtlResponseInfo).isNotBlank();
        assertThat(approvalCode).isNull();
    }

    private static String loadApprovedRequestXml() throws IOException {
        return new String(new ClassPathResource("samples/approved-authorisation-request.xml")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
        String value = nodeList.item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private static String optionalElementText(Document document, String namespace, String localName) {
        NodeList nodeList = document.getElementsByTagNameNS(namespace, localName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        String value = nodeList.item(0).getTextContent();
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
