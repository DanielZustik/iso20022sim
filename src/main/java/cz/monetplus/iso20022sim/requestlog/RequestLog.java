package cz.monetplus.iso20022sim.requestlog;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RequestLog {

    private static final int MAX_ENTRIES = 50;

    private final Deque<RequestLogEntry> entries = new ArrayDeque<>(); // umoznuje add/remove z obou stran fronty tj. stack / queue v jednom
                                                                        // neni thread safe, ale extremne rychly
    public synchronized void recordAuthorisation( //bez sycnhro by mohly soubzne http req modifikovat zaroven Deque. Ten neni thread safe mineno ordering / lost entries jsou mozne.
            String requestIdentifier,             // i kdyby Deque byl thread safe, stejne je by bylo treba atomizovat vicero operaci pr. add(), jinak by stat mohl byt incosistent
            String exchangeId,
            String totalAmount,
            String cardIdentifier,
            String merchantIdentifier,
            String acceptorIdentifier,
            String authorisationDecision,
            String matchedRule,
            String responseSummary) {
        add(new RequestLogEntry(
                currentTimestamp(),
                requestIdentifier,
                exchangeId,
                totalAmount,
                maskCardIdentifier(cardIdentifier),
                merchantIdentifier,
                acceptorIdentifier,
                authorisationDecision,
                matchedRule,
                responseSummary
        ));
    }

    public synchronized void recordInvalidRequest(String responseSummary) {
        add(new RequestLogEntry(
                currentTimestamp(),
                null,
                null,
                null,
                null,
                null,
                null,
                "INVALID_REQUEST",
                null,
                responseSummary
        ));
    }

    public synchronized List<RequestLogEntry> recentEntries() {
        return List.copyOf(entries);
    }

    public synchronized void clear() {
        entries.clear();
    }

    private void add(RequestLogEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    private static OffsetDateTime currentTimestamp() {
        return OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
    }

    private static String maskCardIdentifier(String cardIdentifier) {
        if (cardIdentifier == null || cardIdentifier.length() <= 4) {
            return cardIdentifier;
        }
        return "*".repeat(cardIdentifier.length() - 4) + cardIdentifier.substring(cardIdentifier.length() - 4);
    }

    public record RequestLogEntry(
            OffsetDateTime timestamp,
            String requestIdentifier,
            String exchangeId,
            String totalAmount,
            String cardIdentifier,
            String merchantIdentifier,
            String acceptorIdentifier,
            String authorisationDecision,
            String matchedRule,
            String responseSummary) {
    }
}
