package io.github.nahomgh.portfolio.dto;

import io.github.nahomgh.portfolio.entity.Holding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record PortfolioDTO(BigDecimal totalInvested,
                           BigDecimal totalValuation,
                           BigDecimal currentPnl,
                           List<HoldingDTO> holdings){

    public PortfolioDTO(BigDecimal totalInvested, BigDecimal totalValuation, BigDecimal currentPnl) {
       this(totalInvested, totalValuation, currentPnl, new ArrayList<>());
    }


}
