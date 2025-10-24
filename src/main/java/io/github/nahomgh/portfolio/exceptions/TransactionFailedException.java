package io.github.nahomgh.portfolio.exceptions;

public class TransactionFailedException extends RuntimeException{
    public TransactionFailedException() {
    }

    public TransactionFailedException(String message) {
        super(message);
    }
}
