package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph; // Import EntityGraph
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Method to find all transactions for a given account (either as source or destination)
    List<Transaction> findBySourceAccountOrDestinationAccount(Account sourceAccount, Account destinationAccount);

    // From Phase 1, the corrected method:
    List<Transaction> findBySourceAccount_IdOrDestinationAccount_Id(Long sourceAccountId, Long destinationAccountId);

    // Method to find the latest transaction for previous hash chaining
    // REPLACED with standard Spring Data JPA method + Locking for concurrency control
    // @Query(value = "SELECT t FROM Transaction t ORDER BY t.transactionDate DESC, t.id DESC LIMIT 1")
    // Optional<Transaction> findLatestTransaction();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Transaction> findTopByOrderByTransactionDateDescIdDesc();

    // Method to find all transactions ordered by date and ID (for ledger verification)
    List<Transaction> findAllByOrderByTransactionDateAscIdAsc();

    // Corrected method for Fraud Detection:
    List<Transaction> findBySourceAccountAndTransactionDateAfterOrDestinationAccountAndTransactionDateAfter(
            Account sourceAccount, Instant sourceTransactionDateAfter,
            Account destinationAccount, Instant destinationTransactionDateAfter);

    // Methods to find transactions by status for ledger verification/fraud (if needed for specific filtering)
    List<Transaction> findBySourceAccountAndStatus(Account sourceAccount, String status);
    List<Transaction> findByDestinationAccountAndStatus(Account destinationAccount, String status);

    // NEW: Find transactions for a specific account within a date range with eager fetching
    @EntityGraph(attributePaths = {"sourceAccount", "destinationAccount"}) // Eagerly fetch source and destination accounts
    List<Transaction> findBySourceAccountAndTransactionDateBetweenOrDestinationAccountAndTransactionDateBetween(
            Account sourceAccount, Instant startDate1, Instant endDate1,
            Account destinationAccount, Instant startDate2, Instant endDate2
    );

    // NEW: Find all transactions BEFORE a specific date for opening balance calculation
    // This finds transactions where the account was either source OR destination before the given date
    @EntityGraph(attributePaths = {"sourceAccount", "destinationAccount"}) // Eagerly fetch for this method too
    List<Transaction> findBySourceAccountAndTransactionDateBeforeOrDestinationAccountAndTransactionDateBefore(
            Account sourceAccount, Instant dateBefore1,
            Account destinationAccount, Instant dateBefore2
    );
}
