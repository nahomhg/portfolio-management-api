package io.github.nahomgh.portfolio.entity;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.exceptions.InsufficientFundsException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Entity
@Table(name="holdings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"asset", "user_id"}),
        indexes = {
            @Index(name="idx_holding_user_id", columnList = "user_id")
        })
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name="asset", nullable = false)
    private String asset;

    @NotNull
    @Positive
    @Column(name="units", nullable = false)
    private BigDecimal units;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @NotNull
    @Positive
    @Column(name="total_cost_basis", nullable = false)
    private BigDecimal totalCostBasis;

    @NotNull
    @Positive
    @Column(name="avg_cost_basis", nullable = false)
    private BigDecimal avgCostBasis;

    @Version
    private Long version;

    public Holding() {
    }

    public Holding(User user, String asset, BigDecimal units, BigDecimal pricePerUnit) {
        this.asset = asset;
        this.units = units;
        this.user = user;
        this.avgCostBasis = pricePerUnit;
        this.totalCostBasis = units.multiply(avgCostBasis);
    }

    public String getAsset() {
        return asset;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setAvgCostBasis(BigDecimal avgCostBasis) {
        this.avgCostBasis = avgCostBasis;
    }

    public BigDecimal getTotalCostBasis() {
        return this.totalCostBasis;
    }

    public void setTotalCostBasis(BigDecimal totalCostBasis) {
        this.totalCostBasis = totalCostBasis;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public BigDecimal getAvgCostBasis() {
        return avgCostBasis;
    }


    @Override
    public String toString() {
        return "Holding{" +
                "id=" + id +
                ", asset='" + asset + '\'' +
                ", units=" + units +
                ", user=" + user +
                ", avgCostBasis=" + avgCostBasis +
                '}';
    }
}
