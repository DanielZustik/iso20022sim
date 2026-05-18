package cz.monetplus.iso20022sim.error;

public class InvalidAuthorisationRequestException extends RuntimeException {

    public InvalidAuthorisationRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
