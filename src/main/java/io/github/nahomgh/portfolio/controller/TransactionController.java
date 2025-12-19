package io.github.nahomgh.portfolio.controller;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.dto.TransactionDTO;
import io.github.nahomgh.portfolio.entity.TransactionRequest;
import io.github.nahomgh.portfolio.exceptions.DuplicateTransactionException;
import io.github.nahomgh.portfolio.exceptions.IdempotencyKeyConflictException;
import io.github.nahomgh.portfolio.exceptions.MissingKeyException;
import io.github.nahomgh.portfolio.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDTO>> getUserTransactions(Authentication authentication,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(defaultValue = "desc") String sort){
        User user = (User) authentication.getPrincipal();
        Sort sortOrder = sort.equalsIgnoreCase("desc") || sort.equalsIgnoreCase("descending") ?
                Sort.by("transactionTimestamp").descending()
                : Sort.by("transactionTimestamp").ascending();
        
        Pageable pageable = PageRequest.of(page,size, sortOrder);
        return ResponseEntity.ok().body(transactionService.getTransactions(user.getId(), pageable));
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(HttpServletRequest request, @Valid @RequestBody TransactionRequest transactionRequest, Authentication authentication){
        try {
            if(request.getHeader("IDEMPOTENCY_KEY") == null)
                throw new MissingKeyException("Idempotency Key missing from headers");
            String idempotencyKey = StringEscapeUtils.escapeHtml4(request.getHeader("IDEMPOTENCY_KEY"));
            User user = (User) authentication.getPrincipal();
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(transactionRequest, idempotencyKey, user.getId()));
        }catch(DuplicateTransactionException dupe){
            Map<String, Object> response = Map.of(
                    "message", dupe.getMessage(),
                    "transaction", dupe.getTransaction()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        catch(IdempotencyKeyConflictException dupeKey){
            Map<String, Object> response = Map.of(
                    "message", dupeKey.getMessage()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
}

