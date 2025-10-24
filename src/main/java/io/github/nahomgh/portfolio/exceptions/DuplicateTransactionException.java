package io.github.nahomgh.portfolio.exceptions;

import io.github.nahomgh.portfolio.entity.Transaction;

public class DuplicateTransactionException extends RuntimeException{

    public DuplicateTransactionException(String message) {
        super(message);
    }

}
