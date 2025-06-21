package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.TransactionType;
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList; // NEW: Import ArrayList
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors; // NEW: Import Collectors

@Service // Marks this class as a Spring Service component
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Spring will automatically "inject" instances of AccountRepository and TransactionRepository
    @Autowired
    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Handles a deposit into an account.
     *
     * @param accountId The ID of the account to deposit into.
     * @param amount The amount to deposit. Must be positive.
     * @param description A brief description for the transaction.
     * @return The updated Account object.
     * @throws IllegalArgumentException if the account is not found or amount is not positive.
     */
    @Transactional // Ensures the entire method runs as a single database operation (atomicity)
    public Account deposit(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Update account balance
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now()); // Update timestamp
        Account updatedAccount = accountRepository.save(account);

        // Record the transaction
        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                amount,
                description != null ? description : "Cash Deposit",
                null, // No source account for a deposit from external source
                updatedAccount // Destination account is the one being deposited into
        );
        transactionRepository.save(transaction);

        return updatedAccount;
    }

    /**
     * Handles a withdrawal from an account.
     *
     * @param accountId The ID of the account to withdraw from.
     * @param amount The amount to withdraw. Must be positive.
     * @param description A brief description for the transaction.
     * @return The updated Account object.
     * @throws IllegalArgumentException if the account is not found, amount is not positive, or insufficient funds.
     */
    @Transactional // Ensures atomicity
    public Account withdraw(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Check for sufficient funds
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for withdrawal. Current balance: " + account.getBalance());
        }

        // Update account balance
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now()); // Update timestamp
        Account updatedAccount = accountRepository.save(account);

        // Record the transaction
        Transaction transaction = new Transaction(
                TransactionType.WITHDRAWAL,
                amount,
                description != null ? description : "Cash Withdrawal",
                updatedAccount, // Source account is the one being withdrawn from
                null // No destination account for a withdrawal to external destination
        );
        transactionRepository.save(transaction);

        return updatedAccount;
    }

    /**
     * Handles a transfer of funds between two accounts.
     *
     * @param sourceAccountId The ID of the account to transfer from.
     * @param destinationAccountId The ID of the account to transfer to.
     * @param amount The amount to transfer. Must be positive.
     * @param description A brief description for the transaction.
     * @throws IllegalArgumentException if accounts are not found, amount is not positive, or insufficient funds.
     */
    @Transactional // Ensures atomicity for both debit and credit operations
    public void transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        // Withdraw from source (this will handle insufficient funds check)
        // Note: We're calling our own withdraw and deposit methods.
        // This is safe because they are also @Transactional, and Spring handles nested transactions.
        Account sourceAccount = withdraw(sourceAccountId, amount,
                "Transfer to Account " + destinationAccountId + (description != null ? ": " + description : ""));

        // Deposit to destination
        Account destinationAccount = deposit(destinationAccountId, amount,
                "Transfer from Account " + sourceAccountId + (description != null ? ": " + description : ""));

        // For a transfer, we want to record ONE transaction that links both accounts.
        // We'll create a new Transaction object here directly, bypassing the single-account
        // constructors from deposit/withdraw, as those implicitly handle the null account.
        // This unified record is better for traceability of transfers.
        Transaction transferRecord = new Transaction(
                TransactionType.TRANSFER,
                amount,
                description != null ? description : "Funds Transfer",
                sourceAccount,
                destinationAccount
        );
        transactionRepository.save(transferRecord);
    }

    /**
     * Retrieves all transactions for a given account.
     * This uses the custom query method we defined in TransactionRepository.
     * @param accountId The ID of the account.
     * @return A list of transactions related to the account.
     */
    public List<Transaction> getTransactionsForAccount(Long accountId) {
        return transactionRepository.findBySourceAccount_IdOrDestinationAccount_Id(accountId, accountId);
    }

    /**
     * NEW METHOD: Retrieves all transactions for all accounts owned by a specific user.
     * @param userId The ID of the user.
     * @return A consolidated list of all transactions related to the user's accounts.
     */
    @Transactional(readOnly = true) // Read-only transaction for fetching data
    public List<Transaction> getTransactionsForUser(Long userId) {
        // Find all accounts belonging to the user
        List<Account> userAccounts = accountRepository.findByUserId(userId); // Assuming findByUserId exists in AccountRepository

        List<Transaction> allUserTransactions = new ArrayList<>();

        // For each account, get its transactions
        for (Account account : userAccounts) {
            // Use the existing method to get transactions for each account
            List<Transaction> accountTransactions = getTransactionsForAccount(account.getId());
            allUserTransactions.addAll(accountTransactions);
        }

        // Optional: Remove duplicates if a transaction might be linked to multiple of a user's accounts (e.g., self-transfer)
        // Or if you want to sort them (e.g., by date)
        // For simplicity, let's just return the list for now. Sorting can be done in the controller or template.

        // Sort transactions by date (most recent first)
        return allUserTransactions.stream()
                .distinct() // Remove duplicates if a transaction is recorded for both source and destination if both are user's accounts
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate())) // Sort by transactionDate descending
                .collect(Collectors.toList());
    }
}
