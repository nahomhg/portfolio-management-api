package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.dto.TransactionDTO;
import io.github.nahomgh.portfolio.exceptions.DuplicateTransactionException;
import io.github.nahomgh.portfolio.exceptions.InsufficientFundsException;
import io.github.nahomgh.portfolio.exceptions.UserNotFoundException;
import io.github.nahomgh.portfolio.repository.HoldingRepository;
import io.github.nahomgh.portfolio.repository.TransactionRepository;
import io.github.nahomgh.portfolio.entity.Holding;
import io.github.nahomgh.portfolio.entity.Transaction;
import io.github.nahomgh.portfolio.entity.TransactionRequest;
import io.github.nahomgh.portfolio.entity.TransactionType;
import io.github.nahomgh.portfolio.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PriceDataService priceDataService;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);


    public TransactionService(TransactionRepository transactionRepository, PriceDataService priceDataService, HoldingRepository holdingRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.priceDataService = priceDataService;
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
    }

    public List<TransactionDTO> getTransactions(Long userId){
        Optional<User> userDetails = userRepository.findById(userId);
        if(userDetails.isPresent()){
            return transactionRepository.findTransactionsByUserId(userId).stream().map(TransactionDTO::new).toList();
        }
        logger.error("ERROR: User not found");
        throw new UserNotFoundException("User not Found");
    }

    @Transactional(rollbackOn = Exception.class)
    public TransactionDTO createTransaction(@Valid TransactionRequest request, final String idempotencyKey, Long userId) {
        Optional<Transaction> existingTransaction = transactionRepository.findTransactionByUserIdAndClientIdempotencyKey(userId,idempotencyKey);
        if(existingTransaction.isPresent()){
            if(doesPayloadMatchExistingTransaction(existingTransaction.get(),request)) {
                logger.error("Duplicate transaction detected.");
                throw new DuplicateTransactionException("Transaction Already Exists");
            }
            return new TransactionDTO(existingTransaction.get());
        }
        String assetName = priceDataService.resolveAssetSymbol(request.asset());
        BigDecimal userHoldings = getAssetHoldingsByUser(assetName, userId);
        if((request.transactionType() == TransactionType.SELL) && (request.units().compareTo(userHoldings)>0)) {
            throw new InsufficientFundsException("Insufficient Funds: You do NOT hold enough units to sell");
        }
        try {
            BigDecimal assetPrice = BigDecimal.ZERO;
            boolean isOldTransaction = false;
            if(request.transactionDate() != null && request.transactionDate().isBefore(LocalDate.now())){
                assetPrice = priceDataService.getHistoricalAssetPrice(request.asset(),request.transactionDate());
                isOldTransaction = true;
            }else
                assetPrice = priceDataService.getAssetPrice(assetName);

            BigDecimal total_cost = assetPrice.multiply(request.units());
            User userInfo = userRepository.findById(userId).get();
            Transaction trxn = new Transaction(
                    assetName,
                    request.transactionType(),
                    request.units(),
                    total_cost,
                    idempotencyKey,
                    userInfo,
                    isOldTransaction
                            ? request.transactionDate().atStartOfDay(ZoneId.of("UTC")).toInstant()
                            : Instant.now());

            Transaction transactionSaved = transactionRepository.save(trxn);
            logger.debug("NOTE: Beginning processing transaction");
            processTransaction(userInfo, assetName, request.units(), assetPrice, request.transactionType());
            logger.debug("SUCCESS: Transaction Added To User and Holdings updated");
            return new TransactionDTO(transactionSaved);
        }catch(DuplicateKeyException dupe){
            Transaction firstTransactionProcessed = transactionRepository.findTransactionByUserIdAndClientIdempotencyKey(
                    userId,
                    idempotencyKey)
                    .orElseThrow(() -> dupe);
            if(!doesPayloadMatchExistingTransaction(firstTransactionProcessed, request)){
                throw new DuplicateTransactionException("Idempotency key has been used with different payload");
            }
            return new TransactionDTO(firstTransactionProcessed);
        }
    }

    private boolean doesPayloadMatchExistingTransaction(Transaction existingTrxn, TransactionRequest trxnRequest){
        return trxnRequest.asset().equalsIgnoreCase(existingTrxn.getAsset())
                && trxnRequest.transactionType().toString().equalsIgnoreCase(existingTrxn.getTransactionType().toString())
                && trxnRequest.units().compareTo(existingTrxn.getUnits())==0;
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    private void processTransaction(User user, String assetName, BigDecimal units, BigDecimal pricePerUnit, TransactionType transactionType){
        Optional<Holding> existingHolding = holdingRepository.findByAssetAndUser_Id(assetName, user.getId());
        if(transactionType == TransactionType.BUY){
            if(existingHolding.isPresent()){
                Holding holding = existingHolding.get();
                BigDecimal currentTotalCost = holding.getTotalCostBasis();
                BigDecimal updatedTotalCost = currentTotalCost.add(units.multiply(pricePerUnit));
                BigDecimal updatedTotalUnits = holding.getUnits().add(units); // Appending units.
                BigDecimal updatedAvgCost = updatedTotalCost.divide(updatedTotalUnits, 8, RoundingMode.HALF_UP);

                holding.setUnits(updatedTotalUnits);
                holding.setAvgCostBasis(updatedAvgCost);
                holding.setTotalCostBasis(updatedTotalCost);
                holdingRepository.save(holding);
            }else{
                Holding addNewHolding = new Holding(user, assetName, units, pricePerUnit);
                holdingRepository.save(addNewHolding);
            }
        }else{ // Transaction is SELL
            if(existingHolding.isPresent()){
                Holding holding = existingHolding.get();
                BigDecimal updatedUnits = existingHolding.get().getUnits().subtract(units);

                if(updatedUnits.compareTo(BigDecimal.ZERO)==0){
                    holdingRepository.delete(holding);
                }else{
                    BigDecimal updatedTotalCostAfterSell = updatedUnits.multiply(holding.getAvgCostBasis());
                    holding.setUnits(updatedUnits);
                    holding.setTotalCostBasis(updatedTotalCostAfterSell);
                    holdingRepository.save(holding);
                }
            }
        }

    }

    private BigDecimal getAssetHoldingsByUser(String assetName, Long userId){
        return holdingRepository.findByAssetAndUser_Id(assetName, userId).map(Holding::getUnits).orElse(BigDecimal.ZERO);
    }

}


