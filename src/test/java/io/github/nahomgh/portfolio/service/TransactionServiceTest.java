package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.domain.User;

import io.github.nahomgh.portfolio.dto.AssetDTO;
import io.github.nahomgh.portfolio.dto.TransactionDTO;
import io.github.nahomgh.portfolio.entity.Transaction;
import io.github.nahomgh.portfolio.entity.TransactionRequest;
import io.github.nahomgh.portfolio.entity.TransactionType;
import io.github.nahomgh.portfolio.exceptions.DuplicateTransactionException;
import io.github.nahomgh.portfolio.repository.HoldingRepository;
import io.github.nahomgh.portfolio.repository.TransactionRepository;
import io.github.nahomgh.portfolio.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PriceDataService priceDataService;

    @Mock
    private HoldingRepository holdingRepository;

    @InjectMocks
    private TransactionService transactionService;

    private AssetDTO createAsset(){
        return new AssetDTO("bitcoin",
                "BTC",
                BigDecimal.valueOf(100_000));
    }

    private User createTestUser(){
        User user_bob = new User("nahom_gh@outlook.com","bob","bob123");
        user_bob.setEnabled(true);
        user_bob.setId(1L);
        return user_bob;
    }

    private Transaction createTransactionObject(String assetName, String idempotencyKey, User testUser){
        BigDecimal units = BigDecimal.valueOf(0.1);
        BigDecimal totalCost = createAsset().price().multiply(units);
        return new Transaction(
                assetName,
                TransactionType.BUY,
                units,
                totalCost,
                idempotencyKey,
                testUser,
                Instant.now()
        );
    }

    private TransactionRequest createNewTransactionRequest(String assetName){

        return new TransactionRequest(
                assetName,
                TransactionType.BUY,
                BigDecimal.valueOf(0.1),
                LocalDate.now()
        );
    }

    @Test
    void createNewTransaction() {

        User testUser = createTestUser();

        String idempotencyKey = "abc123";

        TransactionRequest request = createNewTransactionRequest("BTC");
        Transaction transaction = createTransactionObject(request.asset(), idempotencyKey, testUser);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        Mockito.when(holdingRepository.findByAssetAndUser_Id(request.asset(),testUser.getId())).thenReturn(Optional.empty());


        Mockito.when(priceDataService.resolveAssetSymbol(request.asset())).thenReturn("BTC");
        Mockito.when(priceDataService.getAssetPrice(request.asset())).thenReturn(BigDecimal.valueOf(100_000));

        Mockito.when(transactionRepository.save(Mockito.any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDTO responseTrxn = transactionService.createTransaction(request,idempotencyKey,testUser.getId());
        Assertions.assertNotNull(responseTrxn);
        Assertions.assertEquals(transaction.getAsset(),responseTrxn.asset());
        Assertions.assertEquals(transaction.getTransactionType(), responseTrxn.transactionType());
        Assertions.assertTrue(transaction.getUnits().compareTo(responseTrxn.units()) == 0);
        Assertions.assertTrue(transaction.getPricePerUnit().compareTo(responseTrxn.pricePerUnit())==0);

        Mockito.verify(holdingRepository,Mockito.times(2)).findByAssetAndUser_Id("BTC",1L);
        Mockito.verify(transactionRepository, Mockito.times(1)).save(Mockito.any(Transaction.class));

    }


    @Test
    void createNewTransactionDuplicateDetailsShouldThrowException(){
        String idempotencyKey = "idemKey123";

        User testUser = createTestUser();
        TransactionRequest request = createNewTransactionRequest("BTC");

        Transaction existingTrxn = createTransactionObject(request.asset(), idempotencyKey, testUser);

        Mockito.when(transactionRepository.findTransactionByUserIdAndClientIdempotencyKey(1L,idempotencyKey)).thenReturn(Optional.of(existingTrxn));

        Assertions.assertThrows(DuplicateTransactionException.class, () -> {
            transactionService.createTransaction(request,idempotencyKey,testUser.getId());
        });
        Mockito.verify(transactionRepository, Mockito.times(1)).findTransactionByUserIdAndClientIdempotencyKey(1L, idempotencyKey);

    }


}