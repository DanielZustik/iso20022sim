package cz.monetplus.iso20022sim.api;

public record ApiErrorResponse(
        int status,
        String error,
        String message
) {
}
