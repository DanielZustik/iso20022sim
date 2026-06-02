package cz.monetplus.iso20022sim.manualtesting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ManualTestingSampleCatalogue {

    private final Map<String, ManualTestingSample> samplesById;
    private final List<ManualTestingSample> orderedSamples;

    public ManualTestingSampleCatalogue() {
        List<ManualTestingSample> samples = List.of( // immmutability, aby calleri nepozmenili kolekci a zachovali poradi
                sample("approved", "Approved sample request", "manual-testing-samples/approved-authorisation-request.xml"),
                sample("amount-decline", "Amount decline sample request", "manual-testing-samples/amount-decline-authorisation-request.xml"),
                sample("denied-card", "Denied-card sample request", "manual-testing-samples/denied-card-authorisation-request.xml"),
                sample("denied-acceptor", "Denied-acceptor sample request", "manual-testing-samples/denied-acceptor-authorisation-request.xml"),
                sample("invalid-request", "Invalid request sample", "manual-testing-samples/invalid-authorisation-request.xml")
        );

        Map<String, ManualTestingSample> mappedSamples = new LinkedHashMap<>();
        for (ManualTestingSample sample : samples) {
            mappedSamples.put(sample.id(), sample);
        }
        this.samplesById = Map.copyOf(mappedSamples);
        this.orderedSamples = samples;
    }

    public List<ManualTestingSample> listSamples() {
        return orderedSamples;
    }

    public ManualTestingSample requireSample(String sampleId) {
        ManualTestingSample sample = samplesById.get(sampleId);
        if (sample == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown sample id: " + sampleId);
        }
        return sample;
    }

    private static ManualTestingSample sample(String id, String displayName, String resourcePath) {
        return new ManualTestingSample(id, displayName, readClasspathResource(resourcePath));
    }

    private static String readClasspathResource(String resourcePath) {
        try (InputStream inputStream = ManualTestingSampleCatalogue.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing sample resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read sample resource: " + resourcePath, e);
        }
    }

    public record ManualTestingSample(String id, String displayName, String requestXml) {
    }
}
