package cz.monetplus.iso20022sim.authorisation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "simulator")
@Getter
public class SimulatorConfigurationProperties {

    private DefaultDecision defaultDecision = DefaultDecision.APPROVE; // z configu skrz setter bude overriden
    private final Rules rules = new Rules(); // v configu je uvedena nested struktura, Spring vi, ze tedy nenastavuje pro rules primo hodnotu, ale vola getter a nastavuje hodnoty skrzz settery pote
                                             // mental. gymanistika, mohlo to by normalni non nested non static classou.. jen proto, ze Rules enjsou domenovou objekt defakto, jen vnitrni organizaci pro props, ktery domenou je

    public void setDefaultDecision(DefaultDecision defaultDecision) {
        this.defaultDecision = defaultDecision;
    }

    public enum DefaultDecision {
        APPROVE,
        DECLINE
    }

    @Setter
    @Getter
    public static class Rules { // nested bo je to jen interni struktura SimulatorConfigurationProperties
                                // static nested se chova jako normlani classa jen zanorena v nasmespacu, nepotrebuje mit isntanci outter classy (ac tady ji ma) a sla by vytvorit naprimo new SimulatorConfigurationProperties.Rules();
        private BigDecimal amountThreshold = new BigDecimal("1000.00");
        private List<String> deniedCardIdentifiers = new ArrayList<>();
        private List<String> deniedMerchantIdentifiers = new ArrayList<>();
        private List<String> deniedAcceptorIdentifiers = new ArrayList<>();
    }
}
