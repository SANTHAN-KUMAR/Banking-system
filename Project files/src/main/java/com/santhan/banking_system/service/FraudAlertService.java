package com.santhan.banking_system.service;

import com.santhan.banking_system.model.FraudAlert;
import com.santhan.banking_system.model.FraudAlert.AlertStatus;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.repository.FraudAlertRepository;
import com.santhan.banking_system.repository.TransactionRepository; // NEW: Import TransactionRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant; // Ensure Instant is imported
import java.time.temporal.ChronoUnit; // Ensure ChronoUnit is imported
import java.util.List;
import java.util.Optional;

@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository; // NEW: Inject TransactionRepository

    // Define thresholds for fraud rules (can be moved to application.properties later)
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("5000.00");
    // NEW: Thresholds for frequent transactions
    private static final int FREQUENT_TRANSACTIONS_COUNT = 5; // e.g., 5 transactions
    private static final long FREQUENT_TRANSACTIONS_PERIOD_MINUTES = 60; // within 60 minutes

    @Autowired
    public FraudAlertService(FraudAlertRepository fraudAlertRepository,
                             TransactionRepository transactionRepository) { // NEW: Inject TransactionRepository
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository; // Initialize TransactionRepository
    }

    /**
     * Creates a new fraud alert in the database.
     * This method is called internally when suspicious activity is detected.
     *
     * @param transaction The transaction that triggered the alert.
     * @param alertType The type of alert (e.g., "LARGE_TRANSACTION").
     * @param description A detailed description of the alert.
     * @return The saved FraudAlert object.
     */
    @Transactional
    public FraudAlert createAlert(Transaction transaction, String alertType, String description) {
        // First, check if an alert already exists for this exact transaction to avoid duplicates
        // or if an alert of this type already exists for this transaction (e.g. large transaction, but no other type)
        Optional<FraudAlert> existingAlert = fraudAlertRepository.findByTransaction_Id(transaction.getId());
        if (existingAlert.isPresent()) {
            // Optional: You might want to update an existing alert here if the alertType is new
            // For now, we'll just skip if any alert for this transaction exists.
            System.out.println("DEBUG: Alert already exists for transaction ID " + transaction.getId() + ". Skipping new alert creation.");
            return existingAlert.get(); // Return existing alert
        }

        FraudAlert alert = new FraudAlert();
        alert.setTransaction(transaction);
        alert.setAlertType(alertType);
        alert.setDescription(description);
        alert.setStatus(AlertStatus.PENDING); // New alerts are always pending review

        System.out.println("DEBUG: Creating new fraud alert for Transaction ID: " + transaction.getId() +
                ", Type: " + alertType + ", Description: " + description);
        return fraudAlertRepository.save(alert);
    }

    /**
     * Evaluates a transaction for potential fraud.
     * This method will contain the fraud detection rules.
     * It will be called by the TransactionService after a transaction is successfully saved.
     *
     * @param transaction The transaction to evaluate.
     */
    @Transactional
    public void evaluateTransactionForFraud(Transaction transaction) {
        // Rule 1: Large Transaction Detection
        if (transaction.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            String description = "Transaction amount (" + transaction.getAmount() + ") exceeds large transaction threshold (" + LARGE_TRANSACTION_THRESHOLD + ").";
            createAlert(transaction, "LARGE_TRANSACTION", description);
            System.out.println("INFO: Detected potential fraud: LARGE_TRANSACTION for Txn ID " + transaction.getId());
        }

        // NEW Rule 2: Frequent Transactions Detection
        // Applies to source account for withdrawals/transfers, or destination account for deposits
        Long relevantAccountId = null;
        if (transaction.getSourceAccount() != null) {
            relevantAccountId = transaction.getSourceAccount().getId();
        } else if (transaction.getDestinationAccount() != null) {
            relevantAccountId = transaction.getDestinationAccount().getId();
        }

        if (relevantAccountId != null) {
            Instant timeWindowStart = transaction.getTransactionDate().minus(FREQUENT_TRANSACTIONS_PERIOD_MINUTES, ChronoUnit.MINUTES);
            List<Transaction> recentTransactions = transactionRepository.findBySourceAccountIdAndTransactionDateAfterOrDestinationAccountIdAndTransactionDateAfter(
                    relevantAccountId, timeWindowStart, relevantAccountId, timeWindowStart
            );

            // Filter to ensure we only count distinct transactions and exclude the current one if necessary
            // Although the current transaction will be included in the query result, the logic below checks total.
            // If the count >= threshold, and it includes the current one, it means the rule is met.
            if (recentTransactions.size() >= FREQUENT_TRANSACTIONS_COUNT) {
                String description = "Account ID " + relevantAccountId + " has " + recentTransactions.size() +
                        " transactions within the last " + FREQUENT_TRANSACTIONS_PERIOD_MINUTES + " minutes. Threshold: " + FREQUENT_TRANSACTIONS_COUNT;
                createAlert(transaction, "FREQUENT_TRANSACTIONS", description);
                System.out.println("INFO: Detected potential fraud: FREQUENT_TRANSACTIONS for Account ID " + relevantAccountId);
            }
        }
        // TODO: Add more fraud detection rules here in the future
    }

    /**
     * Retrieves all fraud alerts.
     * @return A list of all FraudAlert objects.
     */
    @Transactional(readOnly = true)
    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepository.findAll();
    }

    /**
     * Retrieves fraud alerts by their status.
     * @param status The status to filter by (PENDING, REVIEWED, DISMISSED, ESCALATED).
     * @return A list of FraudAlert objects matching the status.
     */
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByStatus(AlertStatus status) {
        return fraudAlertRepository.findByStatus(status);
    }

    /**
     * Updates the status of a fraud alert.
     * @param alertId The ID of the alert to update.
     * @param newStatus The new status for the alert.
     * @return The updated FraudAlert object.
     * @throws IllegalArgumentException if the alert is not found.
     */
    @Transactional
    public FraudAlert updateAlertStatus(Long alertId, AlertStatus newStatus) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud Alert not found with ID: " + alertId));
        alert.setStatus(newStatus);
        return fraudAlertRepository.save(alert);
    }

    /**
     * Deletes a fraud alert by its ID.
     * @param alertId The ID of the alert to delete.
     * @throws IllegalArgumentException if the alert is not found.
     */
    @Transactional
    public void deleteAlert(Long alertId) {
        if (!fraudAlertRepository.existsById(alertId)) {
            throw new IllegalArgumentException("Fraud Alert not found with ID: " + alertId);
        }
        fraudAlertRepository.deleteById(alertId);
        System.out.println("INFO: Deleted Fraud Alert ID: " + alertId);
    }
}
