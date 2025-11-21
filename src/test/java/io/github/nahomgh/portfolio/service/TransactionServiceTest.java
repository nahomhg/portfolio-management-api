package io.github.nahomgh.portfolio.service;

import io.github.nahomgh.portfolio.auth.domain.User;

import io.github.nahomgh.portfolio.dto.AssetDTO;
import io.github.nahomgh.portfolio.dto.TransactionDTO;
import io.github.nahomgh.portfolio.entity.Holding;
import io.github.nahomgh.portfolio.entity.Transaction;
import io.github.nahomgh.portfolio.entity.TransactionRequest;
import io.github.nahomgh.portfolio.entity.TransactionType;
import io.github.nahomgh.portfolio.exceptions.DuplicateTransactionException;
import io.github.nahomgh.portfolio.exceptions.InsufficientFundsException;
import io.github.nahomgh.portfolio.repository.HoldingRepository;
import io.github.nahomgh.portfolio.repository.TransactionRepository;
import io.github.nahomgh.portfolio.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
    private ProcessTransactionsService processTransactions;

    private TransactionService transactionService;

    private String idempotencyKey;
    private User testUser;

    @BeforeEach
    public void setup(){
        idempotencyKey = "123";
        //
        testUser = new User("nahom_gh@outlook.com","bob","bob123");
        testUser.setEnabled(true);
        testUser.setId(1L);

        //
        transactionService = new TransactionService(
                transactionRepository,
                priceDataService,
                userRepository,
                processTransactions
        );

        //
        Mockito.lenient().when(transactionRepository.findTransactionByUserIdAndClientIdempotencyKey(1L,idempotencyKey)).thenReturn(Optional.empty());
        Mockito.lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    }

    private AssetDTO createAsset(){
        return new AssetDTO("bitcoin",
                "BTC",
                BigDecimal.valueOf(100_000));
    }

    private Transaction createTransactionObject(String assetName, String idempotencyKey, User testUser, TransactionType transactionType, BigDecimal unitAmount){
        BigDecimal totalCost = createAsset().price().multiply(unitAmount);
        BigDecimal pricePerUnit = createAsset().price();
        return new Transaction(
                assetName,
                transactionType,
                unitAmount,
                totalCost,
                pricePerUnit,
                idempotencyKey,
                testUser,
                Instant.now()
        );
    }

    private TransactionRequest createNewTransactionRequest(String assetName, TransactionType transactionType, BigDecimal unitAmount){
        return new TransactionRequest(
                assetName,
                transactionType,
                unitAmount,
                LocalDate.now()
        );
    }

    @Test
    void createNewTransaction() {

        TransactionRequest request = createNewTransactionRequest("BTC", TransactionType.BUY, BigDecimal.valueOf(0.1));
        Transaction transaction = createTransactionObject(request.asset(), idempotencyKey, testUser, TransactionType.BUY, BigDecimal.valueOf(0.1));

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

        InOrder inOrder = Mockito.inOrder(transactionRepository,holdingRepository);

        inOrder.verify(transactionRepository).findTransactionByUserIdAndClientIdempotencyKey(1L, idempotencyKey);
        inOrder.verify(holdingRepository, Mockito.atLeastOnce()).findByAssetAndUser_Id("BTC", 1L);
        inOrder.verify(holdingRepository).save(Mockito.any(Holding.class));
        inOrder.verify(transactionRepository).save(Mockito.any(Transaction.class));

        Mockito.verify(holdingRepository,Mockito.times(1)).findByAssetAndUser_Id("BTC",1L);
        Mockito.verify(transactionRepository, Mockito.times(1)).save(Mockito.any(Transaction.class));

    }


    @Test
    void createSellTransaction(){

        Holding btcHolding = new Holding(testUser,"BTC", BigDecimal.valueOf(2.3),BigDecimal.valueOf(55_000));

        TransactionRequest sellTransactionRequest = createNewTransactionRequest("BTC", TransactionType.SELL, BigDecimal.valueOf(0.5));
        Transaction transaction = createTransactionObject("BTC",idempotencyKey, testUser, TransactionType.SELL,  BigDecimal.valueOf(0.5));

        Mockito.when(priceDataService.resolveAssetSymbol(sellTransactionRequest.asset())).thenReturn("BTC");
        Mockito.when(priceDataService.getAssetPrice(sellTransactionRequest.asset())).thenReturn(BigDecimal.valueOf(100_000));
        Mockito.when(holdingRepository.findByAssetAndUser_Id("BTC",testUser.getId())).thenReturn(Optional.of(btcHolding));
        Mockito.when(transactionRepository.save(Mockito.any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDTO sellTransactionDTO = transactionService.createTransaction(sellTransactionRequest,idempotencyKey,testUser.getId());

        Assertions.assertNotNull(sellTransactionDTO);
        Assertions.assertEquals(transaction.getAsset(), sellTransactionDTO.asset());
        Assertions.assertEquals(transaction.getTransactionType(), sellTransactionDTO.transactionType());
        Assertions.assertTrue(transaction.getUnits().compareTo(sellTransactionDTO.units())==0);

        Mockito.verify(holdingRepository).save(Mockito.argThat(holding ->
                holding.getAsset().equals("BTC") && holding.getUnits().compareTo(BigDecimal.valueOf(1.8)) == 0)
        );

        InOrder inOrder = Mockito.inOrder(transactionRepository,holdingRepository);

        inOrder.verify(transactionRepository).findTransactionByUserIdAndClientIdempotencyKey(1L, idempotencyKey);
        inOrder.verify(holdingRepository, Mockito.atLeastOnce()).findByAssetAndUser_Id("BTC", 1L);
        inOrder.verify(holdingRepository).save(Mockito.any(Holding.class));
        inOrder.verify(transactionRepository).save(Mockito.any(Transaction.class));

        Mockito.verify(userRepository, Mockito.times(1)).findById(testUser.getId());
        Mockito.verify(priceDataService, Mockito.times(1)).resolveAssetSymbol(sellTransactionRequest.asset());
        Mockito.verify(priceDataService, Mockito.times(1)).getAssetPrice(sellTransactionRequest.asset());
        Mockito.verify(holdingRepository, Mockito.atLeastOnce()).findByAssetAndUser_Id(sellTransactionRequest.asset(),testUser.getId());
    }

    @Test
    void createNewTransactionDuplicateDetailsShouldThrowException(){

        TransactionRequest request = createNewTransactionRequest("BTC", TransactionType.BUY, BigDecimal.valueOf(0.1));

        Transaction existingTrxn = createTransactionObject(request.asset(), idempotencyKey, testUser, TransactionType.BUY, BigDecimal.valueOf(0.1));

        Mockito.when(transactionRepository.findTransactionByUserIdAndClientIdempotencyKey(1L,idempotencyKey)).thenReturn(Optional.of(existingTrxn));

        Assertions.assertThrows(DuplicateTransactionException.class, () -> transactionService.createTransaction(request,idempotencyKey,testUser.getId()));

        Mockito.verify(transactionRepository, Mockito.never()).save(Mockito.any());

        Mockito.verify(transactionRepository, Mockito.times(1)).findTransactionByUserIdAndClientIdempotencyKey(1L, idempotencyKey);

    }



    @Test
    @DisplayName("Testing - Sell Transaction Fails - Insufficient Funds Exception Thrown")
    void sellTransactionShouldFailWhenInsufficientHoldings(){

        // Attempt to sell more assets than held.
        TransactionRequest sellTransactionRequest = createNewTransactionRequest("BTC", TransactionType.SELL, BigDecimal.valueOf(3.1));

        Holding btcHolding = new Holding(testUser,"BTC", BigDecimal.valueOf(2.3),BigDecimal.valueOf(55_000));
        Mockito.when(priceDataService.resolveAssetSymbol(sellTransactionRequest.asset())).thenReturn(btcHolding.getAsset());
        Mockito.when(holdingRepository.findByAssetAndUser_Id(btcHolding.getAsset(),testUser.getId())).thenReturn(Optional.of(btcHolding));
        Mockito.when(priceDataService.getAssetPrice(sellTransactionRequest.asset())).thenReturn(BigDecimal.valueOf(100_000));

        Assertions.assertThrows(InsufficientFundsException.class, () -> transactionService.createTransaction(sellTransactionRequest,idempotencyKey,testUser.getId()));

        Mockito.verify(holdingRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(transactionRepository, Mockito.never()).save(Mockito.any());


        Mockito.verify(transactionRepository, Mockito.times(1)).findTransactionByUserIdAndClientIdempotencyKey(1L, idempotencyKey);
        Mockito.verify(priceDataService, Mockito.times(1)).resolveAssetSymbol(sellTransactionRequest.asset());
        Mockito.verify(holdingRepository, Mockito.atLeastOnce()).findByAssetAndUser_Id(sellTransactionRequest.asset(),testUser.getId());
    }



}
