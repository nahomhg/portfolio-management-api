package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.dto.TransactionDTO;
import io.github.nahomgh.portfolio.entity.TransactionType;
import io.github.nahomgh.portfolio.exceptions.DuplicateTransactionException;
import io.github.nahomgh.portfolio.exceptions.IdempotencyKeyConflictException;
import io.github.nahomgh.portfolio.exceptions.InputValidationException;
import io.github.nahomgh.portfolio.exceptions.UserNotFoundException;
import io.github.nahomgh.portfolio.repository.HoldingRepository;
import io.github.nahomgh.portfolio.repository.TransactionRepository;
import io.github.nahomgh.portfolio.entity.Transaction;
import io.github.nahomgh.portfolio.entity.TransactionRequest;
import io.github.nahomgh.portfolio.repository.UserRepository;
import jakarta.validation.Valid;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final ProcessTransactionsService processTransaction;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository, PriceDataService priceDataService,
            UserRepository userRepository, ProcessTransactionsService processTransaction) {
        this.transactionRepository = transactionRepository;
        this.priceDataService = priceDataService;
        this.userRepository = userRepository;
        this.processTransaction = processTransaction;
    }

    public Page<TransactionDTO> getTransactions(Long userId, Pageable pageable) {
        Optional<User> userDetails = userRepository.findById(userId);
        if (userDetails.isPresent()) {
          return transactionRepository.findTransactionsByUserId(userId, pageable).map(TransactionDTO::new);
        }
        logger.error("ERROR: User not found");
        throw new UserNotFoundException("User not Found");
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionDTO createTransaction(@Valid TransactionRequest request, final String idempotencyKey,
            Long userId) {
        User userInfo = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User with ID " + userId + " NOT Found. Transaction NOT processed."));
        Optional<Transaction> existingTransaction = transactionRepository
                .findTransactionByUserIdAndClientIdempotencyKey(userId, idempotencyKey);
        if (existingTransaction.isPresent()) {
            logger.info("INFO: Transaction already exists - returning transaction DTO to user");
            throw new DuplicateTransactionException("Transaction Already Exists",
                    new TransactionDTO(existingTransaction.get()));
        }
        String assetName = priceDataService.resolveAssetSymbol(StringEscapeUtils.escapeHtml4(request.asset()));
        BigDecimal assetPrice = BigDecimal.ZERO;
        boolean isOldTransaction = false;
        if (request.transactionDate() != null && request.transactionDate().isBefore(LocalDate.now())) {
            if(request.transactionDate().isBefore(LocalDate.now().minusYears(1))) {
                throw new InputValidationException("Transactions Older than 1 year are NOT allowed.");
            }
            assetPrice = priceDataService.getHistoricalAssetPrice(assetName, request.transactionDate());
            isOldTransaction = true;
        } else {
            assetPrice = priceDataService.getAssetPrice(assetName);
        }
        BigDecimal total_cost = request.transactionType() != TransactionType.AIRDROP
                ? assetPrice.multiply(request.units())
                : BigDecimal.ZERO;

        Transaction trxn = new Transaction(
                assetName,
                request.transactionType(),
                request.units(),
                total_cost,
                assetPrice,
                idempotencyKey,
                userInfo,
                isOldTransaction
                        ? request.transactionDate().atStartOfDay(ZoneId.of("UTC")).toInstant()
                        : Instant.now());

        processTransaction.process(userInfo, assetName, request.units(), assetPrice, total_cost,
                request.transactionType());
        Transaction transactionSaved = transactionRepository.save(trxn);
        logger.debug("NOTE: Beginning processing transaction");
        logger.debug("SUCCESS: Transaction Added To User and Holdings updated");
        return new TransactionDTO(transactionSaved);
    }

}
