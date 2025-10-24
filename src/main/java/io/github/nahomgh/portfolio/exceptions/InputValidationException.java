package io.github.nahomgh.portfolio.exceptions;

public class InputValidationException extends RuntimeException{
    public InputValidationException() {
    }

    public InputValidationException(String message) {
        super(message);
    }
}
