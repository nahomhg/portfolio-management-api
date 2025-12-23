package io.github.nahomgh.portfolio.entity;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.InputValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;


@Entity
@Table(name="transactions",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_id_idempotency_id", columnNames = {"user_id", "client_idempotency_key"}),
        indexes = {
            @Index(name="idx_user_id", columnList = "user_id"),
                @Index(name="idx_txn_client_id", columnList = "client_idempotency_key")
        })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name ="client_idempotency_key", nullable = true, updatable = false, columnDefinition = "varchar(75)")
    private String clientIdempotencyKey;

    @Column(name = "asset_name", nullable = false, length = 128)
    private String asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 16)
    private TransactionType transactionType;

    @Column(name="total_cost", nullable = false, updatable = false, precision = 38, scale = 8)
    @PositiveOrZero
    private BigDecimal totalCost;

    @Column(name="price_per_unit", nullable = false, updatable = false, precision = 38, scale = 8)
    private BigDecimal pricePerUnit;

    @Positive
    @NotNull
    @Column(name="units", nullable = false, precision = 38, scale = 8)
    private BigDecimal units;

    @Column(name="txn_timestamp", nullable = false, updatable = false)
    private Instant transactionTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_user_id"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="holding_id", nullable = true, updatable = false, foreignKey = @ForeignKey(name="fk_holding_id"))
    private Holding holding;


    public Transaction(){

    }

    public Transaction(String asset, TransactionType transactionType, BigDecimal units, BigDecimal totalCost, BigDecimal pricePerUnit,
                       String clientIdempotencyKey, User user) {
        if (clientIdempotencyKey == null || clientIdempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction Idempotency Key ID cannot be null or empty");
        }
        if (units == null || units.compareTo(BigDecimal.ZERO) == 0) {
            throw new InputValidationException("Units cannot be empty or 0");
        }
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) < 0
                || (totalCost.compareTo(BigDecimal.ZERO) != 0 && transactionType == TransactionType.AIRDROP)
                || (totalCost.compareTo(BigDecimal.ZERO) == 0 && transactionType != TransactionType.AIRDROP)){
            throw new InputValidationException("Prices must be greater than 0 for non-airdrop Transactions");
            }

        this.asset = asset;
        this.units = units;
        this.totalCost = totalCost;
        this.transactionType = transactionType;
        this.pricePerUnit = pricePerUnit;
        this.user = user;
        this.clientIdempotencyKey = clientIdempotencyKey;
        this.transactionTimestamp = Instant.now();
    }

    public Transaction(String asset, TransactionType transactionType, BigDecimal units, BigDecimal totalCost, BigDecimal pricePerUnit,
                       String clientIdempotencyKey, User user, Instant transactionDate) {
       this(asset, transactionType, units, totalCost, pricePerUnit, clientIdempotencyKey, user);
       this.transactionTimestamp = transactionDate;
    }

    public Long getId() {
        return id;
    }

    public String getAsset() {
        return asset;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Instant getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public BigDecimal getTotalPrice() {
        return totalCost;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "asset='" + asset + '\'' +
                ", transactionType=" + transactionType +
                ", totalCost=" + totalCost +
                ", pricePerUnit=" + pricePerUnit +
                ", units=" + units +
                ", transaction_timestamp=" + transactionTimestamp +
                '}';
    }
}
