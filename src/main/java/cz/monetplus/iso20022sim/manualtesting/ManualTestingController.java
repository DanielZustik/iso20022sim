package cz.monetplus.iso20022sim.manualtesting;

import cz.monetplus.iso20022sim.requestlog.RequestLog;
import java.util.List;
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
    private final RequestLog requestLog;

    public ManualTestingController(ManualTestingSampleCatalogue sampleCatalogue, RequestLog requestLog) {
        this.sampleCatalogue = sampleCatalogue;
        this.requestLog = requestLog;
    }

    @GetMapping
    public String manualTestingPage(Model model) {
        model.addAttribute("samples", sampleCatalogue.listSamples());
        model.addAttribute("defaultSampleId", "approved");
        model.addAttribute("requestLogEntries", requestLog.recentEntries());
        return "manual-testing";
    }

    @GetMapping(path = "/samples/{sampleId}", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody // return value is eg. string into http body (via serilizator HttpMessageConverter = interface), not a view name
    public String sampleRequest(@PathVariable String sampleId) {
        return sampleCatalogue.requireSample(sampleId).requestXml();
    }

    @GetMapping(path = "/request-log", produces = MediaType.APPLICATION_JSON_VALUE) //via Jackson2 converter implemnetation
    @ResponseBody
    public List<RequestLog.RequestLogEntry> requestLog() {
        return requestLog.recentEntries();
    }
}
