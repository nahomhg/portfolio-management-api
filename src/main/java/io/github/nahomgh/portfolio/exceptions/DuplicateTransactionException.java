package io.github.nahomgh.portfolio.exceptions;

import io.github.nahomgh.portfolio.dto.TransactionDTO;

public class DuplicateTransactionException extends RuntimeException{

    private final TransactionDTO transaction;

    public DuplicateTransactionException(String message, TransactionDTO transaction) {
        super(message);
        this.transaction = transaction;
    }


    public TransactionDTO getTransaction() {
        return transaction;
    }
}
