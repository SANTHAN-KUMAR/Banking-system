package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // NEW: Import EntityGraph
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccount_IdOrDestinationAccount_Id(Long sourceAccountId, Long destinationAccountId);

    @Query("SELECT t FROM Transaction t ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findTopByOrderByTransactionDateDescIdDesc(Pageable pageable);

    default Optional<Transaction> findLatestTransaction() {
        return findTopByOrderByTransactionDateDescIdDesc(org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    // NEW/MODIFIED: Eagerly fetch source and destination accounts, and their users, for ledger verification
    // This query ensures that when verifyLedgerIntegrity fetches all transactions,
    // all linked Account and User objects are also loaded, preventing LazyInitializationException.
    @EntityGraph(attributePaths = {"sourceAccount", "destinationAccount", "sourceAccount.user", "destinationAccount.user"})
    List<Transaction> findAllByOrderByTransactionDateAscIdAsc();

    // The optional findTopByAccountIdOrderByTransactionDateDesc is commented out as it's not currently used.
    /*
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findTopByAccountIdOrderByTransactionDateDesc(
            @Param("accountId") Long accountId, Pageable pageable);

    default Optional<Transaction> findLatestTransactionForAccount(Long accountId) {
        return findTopByAccountIdOrderByTransactionDateDesc(accountId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }
    */
}
