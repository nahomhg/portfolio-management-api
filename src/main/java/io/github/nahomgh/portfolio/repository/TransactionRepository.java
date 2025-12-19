package io.github.nahomgh.portfolio.repository;

import io.github.nahomgh.portfolio.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAsset(String assetName);

    Page<Transaction> findTransactionsByUserId(Long userId, Pageable pageable);

    Optional<Transaction> findTransactionByUserIdAndClientIdempotencyKey(Long userId, String externalTxnId);

}
