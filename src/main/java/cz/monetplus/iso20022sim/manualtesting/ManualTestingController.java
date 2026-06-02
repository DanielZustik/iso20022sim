package cz.monetplus.iso20022sim.manualtesting;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller // handles web requests, but method return values may be interpreted as view names
@RequestMapping("/manual-testing")
public class ManualTestingController {

    private final ManualTestingSampleCatalogue sampleCatalogue;

    public ManualTestingController(ManualTestingSampleCatalogue sampleCatalogue) {
        this.sampleCatalogue = sampleCatalogue;
    }

    @GetMapping
    public String manualTestingPage(Model model) {
        model.addAttribute("samples", sampleCatalogue.listSamples());
        model.addAttribute("defaultSampleId", "approved");
        return "manual-testing";
    }

    @GetMapping(path = "/samples/{sampleId}", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody // return value is string into http body, not a view name
    public String sampleRequest(@PathVariable String sampleId) {
        return sampleCatalogue.requireSample(sampleId).requestXml();
    }
}

