package io.github.nahomgh.portfolio.exceptions;

public class EmailDeliveryException extends RuntimeException{

    public EmailDeliveryException(String message, Throwable clause) {
        super(message, clause);
    }
}
