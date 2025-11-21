package io.github.nahomgh.portfolio.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest (
        @Valid
        @NotBlank(message = "Asset Name Mandatory") @NotNull(message = "Asset Name Mandatory")
        @Size(min = 2, max = 30, message = "Asset name has to be minimum 2 characters")
        String asset,
        @NotNull(message = "Transaction Type Mandatory. Type must be BUY or SELL") TransactionType transactionType,
        @Positive @NotNull(message = "Units Amount Mandatory") @DecimalMin(value = "0.00001") @Max(value = 1_000) BigDecimal units,
        @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") @PastOrPresent(message = "Date MUST be past or present") LocalDate transactionDate) {

}
