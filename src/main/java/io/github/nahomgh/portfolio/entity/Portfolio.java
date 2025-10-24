package io.github.nahomgh.portfolio.entity;

import io.github.nahomgh.portfolio.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "portfolios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolioType", "user_id"}),
        indexes = {
                @Index(name="idx_portfolio_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name="created_at", nullable = false,updatable = false)
    private Instant createdAt;

    @Column(name="total_invested", nullable = true, updatable = true)
    private BigDecimal totalInvested;

    @Column(name="total_valuation", nullable = true, updatable = true)
    private BigDecimal totalValuation;

    @Column(name="total_pnl", nullable = true, updatable = true)
    private BigDecimal totalPnL;

    @Column(name="last_updated", nullable = false, updatable = true)
    private Instant lastUpdated;


    @Override
    public String toString() {
        return "Portfolio{" +
                "id=" + id +
                ", user=" + user +
                ", createdAt=" + createdAt +
                ", totalInvested=" + totalInvested +
                ", totalValuation=" + totalValuation +
                ", totalPnL=" + totalPnL +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
