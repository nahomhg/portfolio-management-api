package io.github.nahomgh.portfolio.dto;

import io.github.nahomgh.portfolio.entity.Transaction;
import io.github.nahomgh.portfolio.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDTO(String asset, TransactionType transactionType, BigDecimal units, BigDecimal totalPrice, BigDecimal pricePerUnit, Instant transactionDate){
    public TransactionDTO(Transaction transaction){
        this(transaction.getAsset(),transaction.getTransactionType(),
                transaction.getUnits(), transaction.getTotalPrice(),
                transaction.getPricePerUnit(), transaction.getTransactionTimestamp());
    }
}