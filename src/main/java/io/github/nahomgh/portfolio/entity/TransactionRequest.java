package io.github.nahomgh.portfolio.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest (
        @Valid
        @NotBlank(message = "Asset Name Mandatory") @NotNull(message = "Asset Name Mandatory") String asset,
        @NotNull(message = "Transaction Type Mandatory. Type must be BUY or SELL") TransactionType transactionType,
        @Positive @NotNull(message = "Units Amount Mandatory") BigDecimal units,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate transactionDate) {

}
