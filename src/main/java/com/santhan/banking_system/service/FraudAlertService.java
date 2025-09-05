package com.santhan.banking_system.service;

import com.santhan.banking_system.model.FraudAlert;
import com.santhan.banking_system.model.FraudAlert.AlertType;
import com.santhan.banking_system.model.FraudAlert.AlertStatus;
import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.TransactionType; // Import TransactionType
import com.santhan.banking_system.repository.FraudAlertRepository;
import com.santhan.banking_system.repository.TransactionRepository;
import com.santhan.banking_system.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset; // Import ZoneOffset for conversion
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    // Define thresholds for fraud rules
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("10000.00");
    private static final int MULTIPLE_LARGE_TRANSACTIONS_COUNT = 3;
    private static final long MULTIPLE_LARGE_TRANSACTIONS_PERIOD_MINUTES = 10;
    private static final long NEW_ACCOUNT_THRESHOLD_HOURS = 24;

    @Autowired
    public FraudAlertService(FraudAlertRepository fraudAlertRepository,
                             TransactionRepository transactionRepository,
                             AccountRepository accountRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public FraudAlert createAlert(Transaction transaction, AlertType alertType, String description, AlertStatus status) {
        // Prevent duplicate alerts for the exact same transaction and alert type
        Optional<FraudAlert> existingAlert = fraudAlertRepository.findByTransactionAndAlertType(transaction, alertType);
        if (existingAlert.isPresent()) {
            System.out.println("DEBUG: Alert of type " + alertType + " already exists for transaction ID " + transaction.getId() + ". Skipping new alert creation.");
            return existingAlert.get();
        }

        FraudAlert alert = new FraudAlert();
        alert.setTransaction(transaction);
        alert.setAlertType(alertType);
        alert.setDescription(description);
        alert.setStatus(status);
        alert.setCreatedAt(Instant.now());
        alert.setLastUpdatedAt(Instant.now());

        System.out.println("DEBUG: Creating new fraud alert for Transaction ID: " + transaction.getId() +
                ", Type: " + alertType.name() + ", Description: " + description);
        return fraudAlertRepository.save(alert);
    }

    @Transactional
    public void evaluateTransactionForFraud(Transaction transaction) {
        // Rule 1: Large Single Transaction Detection
        if (transaction.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            String description = "Transaction amount (" + transaction.getAmount() + ") exceeds large transaction threshold (" + LARGE_TRANSACTION_THRESHOLD + ").";
            createAlert(transaction, AlertType.LARGE_TRANSACTION, description, AlertStatus.PENDING);
            System.out.println("INFO: Detected potential fraud: LARGE_TRANSACTION for Txn ID " + transaction.getId());
        }

        // Rule 2: Multiple Large Transactions in Short Period
        Account relevantAccount = null;
        if (transaction.getSourceAccount() != null) {
            relevantAccount = transaction.getSourceAccount();
        } else if (transaction.getDestinationAccount() != null) {
            relevantAccount = transaction.getDestinationAccount();
        }

        if (relevantAccount != null) {
            Instant timeWindowStart = Instant.now().minus(MULTIPLE_LARGE_TRANSACTIONS_PERIOD_MINUTES, ChronoUnit.MINUTES);

            // Fetch transactions related to this account within the time window
            List<Transaction> recentTransactions = transactionRepository.findBySourceAccountAndTransactionDateAfterOrDestinationAccountAndTransactionDateAfter(
                    relevantAccount, timeWindowStart, relevantAccount, timeWindowStart
            );

            // Filter for only 'large' transactions based on the same threshold
            long largeRecentTransactionsCount = recentTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0)
                    .count();

            if (largeRecentTransactionsCount >= MULTIPLE_LARGE_TRANSACTIONS_COUNT) {
                String description = "Account ID " + relevantAccount.getId() + " has " + largeRecentTransactionsCount +
                        " large transactions (over " + LARGE_TRANSACTION_THRESHOLD + ") within the last " + MULTIPLE_LARGE_TRANSACTIONS_PERIOD_MINUTES + " minutes.";
                createAlert(transaction, AlertType.MULTIPLE_LARGE_TRANSACTIONS, description, AlertStatus.PENDING);
                System.out.println("INFO: Detected potential fraud: MULTIPLE_LARGE_TRANSACTIONS for Account ID " + relevantAccount.getId());
            }
        }

        // Rule 3: Transaction to a Newly Created Account (if it's a transfer or deposit)
        if (transaction.getDestinationAccount() != null &&
                (transaction.getTransactionType() == TransactionType.TRANSFER || transaction.getTransactionType() == TransactionType.DEPOSIT)) { // Fixed comparison

            Account destinationAccount = transaction.getDestinationAccount();

            if (destinationAccount.getCreatedAt() != null) {
                Instant newAccountCutoff = Instant.now().minus(NEW_ACCOUNT_THRESHOLD_HOURS, ChronoUnit.HOURS);

                // Convert LocalDateTime to Instant for comparison, assuming createdAt is LocalDateTime
                Instant destinationAccountCreatedAtInstant = destinationAccount.getCreatedAt().toInstant(ZoneOffset.UTC); // Assuming UTC for conversion

                if (destinationAccountCreatedAtInstant.isAfter(newAccountCutoff)) { // Fixed comparison
                    String description = "Transfer/Deposit to a newly created account (ID: " + destinationAccount.getId() + "). Account created on: " + destinationAccount.getCreatedAt();
                    createAlert(transaction, AlertType.NEWLY_CREATED_ACCOUNT_TRANSFER, description, AlertStatus.PENDING);
                    System.out.println("INFO: Detected potential fraud: NEWLY_CREATED_ACCOUNT_TRANSFER for Txn ID " + transaction.getId());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByStatus(AlertStatus status) {
        return fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public FraudAlert updateAlertStatus(Long alertId, AlertStatus newStatus) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud Alert not found with ID: " + alertId));
        alert.setStatus(newStatus);
        alert.setLastUpdatedAt(Instant.now()); // Update timestamp on status change
        return fraudAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        if (!fraudAlertRepository.existsById(alertId)) {
            throw new IllegalArgumentException("Fraud Alert not found with ID: " + alertId);
        }
        fraudAlertRepository.deleteById(alertId);
        System.out.println("INFO: Deleted Fraud Alert ID: " + alertId);
    }
}
