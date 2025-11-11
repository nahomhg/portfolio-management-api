package io.github.nahomgh.portfolio.exceptions;

public class InvalidVerificationCodeException extends RuntimeException{
    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
