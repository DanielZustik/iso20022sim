package cz.monetplus.iso20022sim.manualtesting;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cz.monetplus.iso20022sim.requestlog.RequestLog;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest //Scans project for config and start Spring app. No browser, no TCP socket, no Tomcat listening on a port
@AutoConfigureMockMvc
class ManualTestingUiTest {

    @Autowired
    private MockMvc mockMvc; //web layer using fake HTTP via MockMvc

    @Autowired
    private RequestLog requestLog;

    @BeforeEach
    void clearRequestLog() {
        requestLog.clear();
    }

    @Test
    void manualTestingUiRendersAndTargetsAuthorisationApi() throws Exception {
        mockMvc.perform(get("/manual-testing")) //ala dotaz z browseru
                .andExpect(status().isOk()) // odpoved ma..
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Manual Testing UI"))) // proveruje cele http body
                .andExpect(content().string(containsString("approved")))
                .andExpect(content().string(containsString("amount-decline")))
                .andExpect(content().string(containsString("denied-card")))
                .andExpect(content().string(containsString("denied-acceptor")))
                .andExpect(content().string(containsString("invalid-request")))
                .andExpect(content().string(containsString("Recent Request Log")))
                .andExpect(content().string(containsString("kept in memory only")))
                .andExpect(content().string(containsString("fetch(\"/api/authorisations\"")));
    }

    @Test
    void sampleCatalogueEndpointsAreAccessible() throws Exception {
        assertSampleResponseContains("approved", "4111111111111111");
        assertSampleResponseContains("amount-decline", "<TtlAmt>1500.00</TtlAmt>");
        assertSampleResponseContains("denied-card", "4000000000000002");
        assertSampleResponseContains("denied-acceptor", "POI-BLOCKED");
        assertSampleResponseContains("invalid-request", "<AccptrAuthstnReq/>");
    }

    @Test
    void unknownSampleReturnsNotFound() throws Exception {
        mockMvc.perform(get("/manual-testing/samples/not-defined"))
                .andExpect(status().isNotFound());
    }

    @Test
    void curatedSamplesHitAuthorisationApiWithExpectedStatuses() throws Exception {
        Map<String, Integer> expectedStatusesBySample = new LinkedHashMap<>();
        expectedStatusesBySample.put("approved", 200);
        expectedStatusesBySample.put("amount-decline", 200);
        expectedStatusesBySample.put("denied-card", 200);
        expectedStatusesBySample.put("denied-acceptor", 200);
        expectedStatusesBySample.put("invalid-request", 422);

        for (Map.Entry<String, Integer> expected : expectedStatusesBySample.entrySet()) {
            String requestXml = loadSampleXml(expected.getKey());
            mockMvc.perform(post("/api/authorisations")
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.ALL)
                            .content(requestXml))
                    .andExpect(status().is(expected.getValue()));
        }
    }

    @Test
    void requestLogShowsRecentDeclinedSubmissionWithSelectedFields() throws Exception {
        mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML)
                        .content(loadSampleXml("denied-card")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/manual-testing/request-log")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].timestamp").isNotEmpty()) //proveruje jen konkretni pole z json odpovedi, ne cele http body. Vezme prvni array element [0]
                .andExpect(jsonPath("$[0].requestIdentifier").value("UI-DENIED-CARD-001")) // stejna idea jako XPath expressions
                .andExpect(jsonPath("$[0].exchangeId").value("3"))// $ root element, v mem pripade array, pak prvni prvek [0], pak nazev pole
                .andExpect(jsonPath("$[0].totalAmount").value("120.50"))
                .andExpect(jsonPath("$[0].cardIdentifier").value("************0002"))
                .andExpect(jsonPath("$[0].merchantIdentifier").value("MERCHANT-001"))
                .andExpect(jsonPath("$[0].acceptorIdentifier").value("POI-001"))
                .andExpect(jsonPath("$[0].authorisationDecision").value("DECL"))
                .andExpect(jsonPath("$[0].matchedRule").value("DENIED_CARD_IDENTIFIER"))
                .andExpect(jsonPath("$[0].responseSummary").value("DECL matchedRule=DENIED_CARD_IDENTIFIER"));

        mockMvc.perform(get("/manual-testing"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UI-DENIED-CARD-001")))
                .andExpect(content().string(containsString("************0002")));
    }

    @Test
    void requestLogRepresentsInvalidSubmissionsConsistently() throws Exception {
        mockMvc.perform(post("/api/authorisations")
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.ALL)
                        .content(loadSampleXml("invalid-request")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/manual-testing/request-log")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorisationDecision").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$[0].responseSummary").value(containsString("Rejected: Authorisation request is not schema-valid")));
    }

    private void assertSampleResponseContains(String sampleId, String expectedText) throws Exception {
        mockMvc.perform(get("/manual-testing/samples/" + sampleId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString(expectedText)));
    }

    private String loadSampleXml(String sampleId) throws Exception {
        MvcResult sampleResult = mockMvc.perform(get("/manual-testing/samples/" + sampleId))
                .andExpect(status().isOk())
                .andReturn();
        return sampleResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
