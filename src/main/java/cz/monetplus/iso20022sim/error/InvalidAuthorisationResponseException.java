package cz.monetplus.iso20022sim.error;

public class InvalidAuthorisationResponseException extends RuntimeException {

    public InvalidAuthorisationResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
