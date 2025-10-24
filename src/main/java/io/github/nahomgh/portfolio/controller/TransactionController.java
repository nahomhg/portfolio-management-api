package io.github.nahomgh.portfolio.controller;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.dto.TransactionDTO;
import io.github.nahomgh.portfolio.entity.TransactionRequest;
import io.github.nahomgh.portfolio.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getUserTransactions(Authentication authentication){
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok().body(transactionService.getTransactions(user.getId()));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(HttpServletRequest request, @Valid @RequestBody TransactionRequest transactionRequest, Authentication authentication){
        String idempotencyKey = request.getHeader("IDEMPOTENCY_KEY");
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(transactionRequest, idempotencyKey, user.getId()));

    }
}

