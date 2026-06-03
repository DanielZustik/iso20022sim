package cz.monetplus.iso20022sim.manualtesting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) //loading a WebServerApplicationContext, starting embedded servers, and listening on a random port
class ManualTestingUiRunSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void manualTestingUiIsServedByRunningApplication() { //“Can the whole app actually start as a running web application, listen on a port, and serve /manual-testing through a real HTTP client?”
        ResponseEntity<String> response = restTemplate.getForEntity("/manual-testing", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(response.getBody())
                .contains("Manual Testing UI")
                .contains("Authorisation API")
                .contains("Recent Request Log");
    }
}
