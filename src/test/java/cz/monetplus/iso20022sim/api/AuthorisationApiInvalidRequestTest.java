package cz.monetplus.iso20022sim.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorisationApiInvalidRequestTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedXmlReturns400BadRequest() throws Exception {
        String malformedXml = "<Document><AccptrAuthstnReq></Document>";

        mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(malformedXml))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed XML"))
                .andExpect(jsonPath("$.message", containsString("XML parsing failed")))
                .andExpect(content().string(not(containsString("caaa.002.001.15"))));
    }

    @Test
    void schemaInvalidSupportedMessageReturns422UnprocessableEntity() throws Exception {
        String schemaInvalidXml = """
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:caaa.001.001.15">
                    <AccptrAuthstnReq>
                        <Hdr>
                            <MsgFctn>AUTQ</MsgFctn>
                            <CreDtTm>2026-05-18T11:55:00Z</CreDtTm>
                        </Hdr>
                        <Tx>
                            <TxId>TX-10001</TxId>
                            <TxAmt>99.95</TxAmt>
                            <CardId>411111******1111</CardId>
                        </Tx>
                    </AccptrAuthstnReq>
                </Document>
                """;

        mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(schemaInvalidXml))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Schema Invalid XML"))
                .andExpect(jsonPath("$.message", containsString("caaa.001.001.15 schema")))
                .andExpect(content().string(not(containsString("caaa.002.001.15"))));
    }

    @Test
    void unsupportedMessageVersionReturns422UnprocessableEntity() throws Exception {
        String unsupportedVersionXml = """
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:caaa.001.001.14">
                    <AccptrAuthstnReq>
                        <Hdr>
                            <MsgFctn>AUTQ</MsgFctn>
                            <CreDtTm>2026-05-18T11:55:00Z</CreDtTm>
                        </Hdr>
                        <Tx>
                            <TxId>TX-10002</TxId>
                            <TxAmt>99.95</TxAmt>
                            <CardId>411111******1111</CardId>
                            <AcceptorId>ACCEPTOR-1</AcceptorId>
                        </Tx>
                    </AccptrAuthstnReq>
                </Document>
                """;

        mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(unsupportedVersionXml))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unsupported ISO 20022 Message Version"))
                .andExpect(jsonPath("$.message", containsString("Unsupported ISO 20022 message/version namespace")))
                .andExpect(content().string(not(containsString("caaa.002.001.15"))));
    }
}
