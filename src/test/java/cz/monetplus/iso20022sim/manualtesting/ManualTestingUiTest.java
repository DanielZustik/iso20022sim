package cz.monetplus.iso20022sim.manualtesting;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ManualTestingUiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void manualTestingUiRendersAndTargetsAuthorisationApi() throws Exception {
        mockMvc.perform(get("/manual-testing")) //ala dotaz z browseru
                .andExpect(status().isOk()) // odpoved ma..
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Manual Testing UI")))
                .andExpect(content().string(containsString("approved")))
                .andExpect(content().string(containsString("amount-decline")))
                .andExpect(content().string(containsString("denied-card")))
                .andExpect(content().string(containsString("denied-acceptor")))
                .andExpect(content().string(containsString("invalid-request")))
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

