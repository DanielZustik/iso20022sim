package cz.monetplus.iso20022sim.validation;

public class UnsupportedMessageVersionException extends RuntimeException {

    public UnsupportedMessageVersionException(String message) {
        super(message);
    }
}
