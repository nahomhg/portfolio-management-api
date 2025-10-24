package io.github.nahomgh.portfolio.dto;

import java.math.BigDecimal;

public record HoldingDTO (String assetName,
                          BigDecimal units,
                          BigDecimal totalValuation,
                          BigDecimal totalCostBasis,
                          BigDecimal unrealisedPnl,
                          BigDecimal portfolioWeight
                          ) {

}
