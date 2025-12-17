package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.entity.Holding;
import io.github.nahomgh.portfolio.entity.TransactionType;
import io.github.nahomgh.portfolio.exceptions.InsufficientFundsException;
import io.github.nahomgh.portfolio.exceptions.ResourceNotFoundException;
import io.github.nahomgh.portfolio.repository.HoldingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ProcessTransactionsService {

    private final HoldingRepository holdingRepository;

    private final Logger logger = LoggerFactory.getLogger(ProcessTransactionsService.class);

    public ProcessTransactionsService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            backoff = @Backoff(delay = 500, multiplier = 3)
    )
    public void process(User user, String assetName, BigDecimal units, BigDecimal pricePerUnit, BigDecimal totalCost, TransactionType transactionType){
        String insufficient_holdings_msg = "Insufficient Funds: You do NOT hold enough units to sell.";
        String no_holdings_for_specified_asset_msg = "No Holdings: Holdings for "+assetName+" NOT found.";
        String unknown_transaction_msg = "Unknown Transaction Type passed. Transaction Type '\""+transactionType+"\"' NOT Valid. Transaction aborted.";

        Optional<Holding> existingHolding = holdingRepository.findByAssetAndUser_Id(assetName, user.getId());
        if(transactionType == TransactionType.BUY || transactionType == TransactionType.AIRDROP){
            if(existingHolding.isPresent()){
                Holding holding = existingHolding.get();
                BigDecimal currentTotalCost = holding.getTotalCostBasis();
                BigDecimal updatedTotalUnits = holding.getUnits().add(units); // Total units after transaction

                BigDecimal updatedTotalCost;
                BigDecimal updatedAvgCostBasis;

                if(transactionType == TransactionType.BUY){
                    updatedTotalCost = currentTotalCost.add(totalCost);
                    if(updatedTotalCost.compareTo(BigDecimal.ZERO) > 0){
                        updatedAvgCostBasis = updatedTotalCost.divide(updatedTotalUnits, 8, RoundingMode.HALF_UP);
                    }
                    else{
                        updatedAvgCostBasis = holding.getAvgCostBasis();
                    }
                }else{
                    updatedTotalCost = currentTotalCost;
                    updatedAvgCostBasis = updatedTotalCost.divide(updatedTotalUnits, 8, RoundingMode.HALF_UP);
                }
                holding.setUnits(updatedTotalUnits);
                holding.setTotalCostBasis(updatedTotalCost);
                holding.setAvgCostBasis(updatedAvgCostBasis);
                holdingRepository.save(holding);
            }else{
                Holding addNewHolding = new Holding(user, assetName, units, pricePerUnit);
                holdingRepository.save(addNewHolding);
            }
        }else if(transactionType == TransactionType.SELL){

            if(existingHolding.isPresent()){
                Holding holding = existingHolding.orElseThrow(
                        () -> {
                            logger.error(no_holdings_for_specified_asset_msg);
                            return new InsufficientFundsException(no_holdings_for_specified_asset_msg);
                        }
                );
                if(units.compareTo(holding.getUnits())>0) {
                    logger.error(insufficient_holdings_msg);
                    throw new InsufficientFundsException(insufficient_holdings_msg);
                }

                BigDecimal updatedUnits = existingHolding.get().getUnits().subtract(units);

                if(updatedUnits.compareTo(BigDecimal.ZERO)==0){
                    holdingRepository.delete(holding);
                }else{
                    BigDecimal updatedTotalCostAfterSell = updatedUnits.multiply(holding.getAvgCostBasis());
                    holding.setUnits(updatedUnits);
                    holding.setTotalCostBasis(updatedTotalCostAfterSell);
                    holdingRepository.save(holding);
                }
            }else {
                logger.error(no_holdings_for_specified_asset_msg);
                throw new ResourceNotFoundException(no_holdings_for_specified_asset_msg);
            }

        } else{
            logger.error(unknown_transaction_msg);
            throw new IllegalArgumentException(unknown_transaction_msg);
        }

    }
}
