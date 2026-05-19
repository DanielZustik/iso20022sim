package cz.monetplus.iso20022sim.authorisation;

public class SchemaValidationException extends RuntimeException {

    public SchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}