package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant; // NEW: Import Instant
import java.util.List;
import org.springframework.data.jpa.repository.Query; // NEW: Import Query
import java.util.Optional; // NEW: Import Optional

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Method to find all transactions for a given account (either as source or destination)
    List<Transaction> findBySourceAccountOrDestinationAccount(Account sourceAccount, Account destinationAccount);

    // From Phase 1, the corrected method:
    List<Transaction> findBySourceAccount_IdOrDestinationAccount_Id(Long sourceAccountId, Long destinationAccountId);

    // Method to find the latest transaction for previous hash chaining
    @Query(value = "SELECT t FROM Transaction t ORDER BY t.transactionDate DESC, t.id DESC LIMIT 1")
    Optional<Transaction> findLatestTransaction();

    // Method to find all transactions ordered by date and ID (for ledger verification)
    List<Transaction> findAllByOrderByTransactionDateAscIdAsc();

    // NEW: Method for Frequent Transactions Detection
    // Finds transactions where the given account is either the source or destination
    // AND the transaction date is after the specified Instant (timeWindowStart).
    List<Transaction> findBySourceAccountIdAndTransactionDateAfterOrDestinationAccountIdAndTransactionDateAfter(
            Long sourceAccountId, Instant sourceTransactionDateAfter,
            Long destinationAccountId, Instant destinationTransactionDateAfter);
}
