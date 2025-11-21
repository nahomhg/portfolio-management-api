package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.dto.HoldingDTO;
import io.github.nahomgh.portfolio.dto.PortfolioDTO;
import io.github.nahomgh.portfolio.entity.Holding;
import io.github.nahomgh.portfolio.exceptions.PriceUnavailableException;
import io.github.nahomgh.portfolio.repository.HoldingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


@Service
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final PriceDataService priceDataService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);

    public PortfolioService(HoldingRepository holdingRepository, PriceDataService priceDataService) {
        this.holdingRepository = holdingRepository;
        this.priceDataService = priceDataService;
    }

    public PortfolioDTO getPortfolio(Long userId) {
        List<Holding> holdings = holdingRepository.findAllByUser_id(userId);
        if (holdings.isEmpty()) {
            return new PortfolioDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map<String, BigDecimal> priceCache = new HashMap<>();
        for(Holding holding : holdings){
            priceCache.putIfAbsent(holding.getAsset(), priceDataService.getAssetPrice(holding.getAsset()));
        }

        // Return holding details:
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalValuation = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            BigDecimal currentAssetPrice = priceCache.get(holding.getAsset());
            totalInvested = totalInvested.add(holding.getTotalCostBasis());
            totalValuation = totalValuation.add(holding.getUnits().multiply(currentAssetPrice).setScale(2, RoundingMode.HALF_UP));

        }

        BigDecimal totalPnL = totalValuation.subtract(totalInvested);

        List<HoldingDTO> holdingsListDTO = new ArrayList<>();
        for (Holding holding : holdings) {
            BigDecimal currentAssetPrice = priceCache.get(holding.getAsset());
            if(currentAssetPrice == null){
                throw new PriceUnavailableException("Prices unavailable for asset "+holding.getAsset());
            }
            BigDecimal holdingInvestment = holding.getTotalCostBasis().setScale(2, RoundingMode.HALF_UP);
            BigDecimal holdingValuation = currentAssetPrice.multiply(holding.getUnits()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unrealisedPnl = holdingValuation.subtract(holdingInvestment).setScale(2, RoundingMode.HALF_UP);
            BigDecimal portfolioWeight = totalValuation.compareTo(BigDecimal.ZERO) > 0 ? holdingValuation.divide(totalValuation, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            holdingsListDTO.add(
                    new HoldingDTO(
                            holding.getAsset(),
                            holding.getUnits(),
                            holdingValuation,
                            holdingInvestment,
                            unrealisedPnl,
                            portfolioWeight));
        }
        return new PortfolioDTO(totalInvested, totalValuation, totalPnL, holdingsListDTO);
    }
}