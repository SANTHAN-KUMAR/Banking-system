package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.TransactionType;
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.TransactionRepository;
import com.santhan.banking_system.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime; // Keep for Account updated_at field, if still used
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertService fraudAlertService;

    @Autowired
    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              FraudAlertService fraudAlertService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertService = fraudAlertService;
    }

    private Transaction createAndSaveChainedTransaction(
            TransactionType type, BigDecimal amount, String description,
            Account sourceAccount, Account destinationAccount) {

        Transaction newTransaction = new Transaction(type, amount, description, sourceAccount, destinationAccount);

        Optional<Transaction> latestTransactionOptional = transactionRepository.findLatestTransaction();
        String previousHash = latestTransactionOptional.map(Transaction::getTransactionHash).orElse("0");

        newTransaction.setPreviousTransactionHash(previousHash);
        newTransaction.setStatus("COMPLETED"); // Ensure status is set for new transactions
        newTransaction.setReversed(false); // Ensure new transactions are not marked reversed

        Transaction savedTransaction = transactionRepository.save(newTransaction);

        String transactionDataString = HashUtil.generateTransactionDataString(
                savedTransaction.getId(),
                savedTransaction.getTransactionType(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription(),
                savedTransaction.getSourceAccount() != null ? savedTransaction.getSourceAccount().getId() : null,
                savedTransaction.getDestinationAccount() != null ? savedTransaction.getDestinationAccount().getId() : null,
                savedTransaction.getTransactionDate()
        );

        // --- START SUPER DEBUG LOGGING (for saving) ---
        System.out.println("\nDEBUG HASH INPUT FOR TRANSACTION ID " + savedTransaction.getId());
        System.out.println("------------------------------------------------------------------");
        System.out.println("Full String for Hashing: [" + transactionDataString + "]");
        System.out.println("Length of string: " + transactionDataString.length());
        System.out.println("Character by character breakdown:");
        for (int i = 0; i < transactionDataString.length(); i++) {
            char c = transactionDataString.charAt(i);
            System.out.println("  Index " + i + ": Char='" + c + "' (Unicode: " + (int) c + ")");
        }
        System.out.println("------------------------------------------------------------------\n");
        // --- END SUPER DEBUG LOGGING ---


        String currentHash = HashUtil.calculateSHA256Hash(transactionDataString);
        savedTransaction.setTransactionHash(currentHash);

        Transaction finalSavedTransaction = transactionRepository.save(savedTransaction); // Save again with hash

        // NEW: Evaluate the transaction for fraud after it's successfully chained and saved
        fraudAlertService.evaluateTransactionForFraud(finalSavedTransaction);

        return finalSavedTransaction;
    }

    @Transactional
    public Account deposit(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now()); // Assuming you still use LocalDateTime for updated_at
        Account updatedAccount = accountRepository.save(account);

        createAndSaveChainedTransaction(
                TransactionType.DEPOSIT,
                amount,
                description != null ? description : "Cash Deposit",
                null,
                updatedAccount
        );

        return updatedAccount;
    }

    @Transactional
    public Account withdraw(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for withdrawal. Current balance: " + account.getBalance());
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now()); // Assuming you still use LocalDateTime for updated_at
        Account updatedAccount = accountRepository.save(account);

        createAndSaveChainedTransaction(
                TransactionType.WITHDRAWAL,
                amount,
                description != null ? description : "Cash Withdrawal",
                updatedAccount,
                null
        );

        return updatedAccount;
    }

    @Transactional
    public void transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        // Fetch accounts first to avoid multiple queries or potential issues if one is not found
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source Account not found with ID: " + sourceAccountId));
        Account destinationAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination Account not found with ID: " + destinationAccountId));

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account " + sourceAccount.getAccountNumber() + " for transfer.");
        }

        // Perform balance updates
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));

        // Update timestamps (if used)
        sourceAccount.setUpdatedAt(LocalDateTime.now());
        destinationAccount.setUpdatedAt(LocalDateTime.now());

        // Save updated accounts
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        // Create the transfer transaction (this will also save it and evaluate fraud)
        createAndSaveChainedTransaction(
                TransactionType.TRANSFER,
                amount,
                description != null ? description : "Funds Transfer",
                sourceAccount,
                destinationAccount
        );
    }

    public List<Transaction> getAllTransactions() {
        // Fetch all transactions and ensure associated accounts/users are eagerly loaded if needed for display.
        // For `findAll`, JPA might do lazy loading by default, so ensure your repository queries or @Transactional
        // contexts load the necessary relationships or use DTOs.
        // The `findAllByOrderByTransactionDateAscIdAsc()` method implicitly loads related entities if mapped correctly
        // for the ledger verification, which might be suitable here as well.
        return transactionRepository.findAllByOrderByTransactionDateAscIdAsc(); // Or just findAll()
    }


    public List<Transaction> getTransactionsForAccount(Long accountId) {
        List<Transaction> allTransactions = transactionRepository.findAllByOrderByTransactionDateAscIdAsc();
        return allTransactions.stream()
                .filter(t -> (t.getSourceAccount() != null && t.getSourceAccount().getId().equals(accountId)) ||
                        (t.getDestinationAccount() != null && t.getDestinationAccount().getId().equals(accountId)))
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate())) // Sort by date descending
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForUser(Long userId) {
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        List<Transaction> allUserTransactions = new ArrayList<>();

        for (Account account : userAccounts) {
            List<Transaction> accountTransactions = getTransactionsForAccount(account.getId());
            allUserTransactions.addAll(accountTransactions);
        }

        return allUserTransactions.stream()
                .distinct()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .collect(Collectors.toList());
    }

    /**
     * Verifies the integrity of the transaction ledger.
     * This method re-calculates hashes and checks if the chain is valid.
     * It uses @EntityGraph in the repository to eager fetch associated data.
     * It also handles potential null previous hashes gracefully.
     * @return true if the ledger is intact, false if tampering is detected.
     */
    @Transactional(readOnly = true)
    public boolean verifyLedgerIntegrity() {
        List<Transaction> allTransactions = transactionRepository.findAllByOrderByTransactionDateAscIdAsc();

        String expectedPreviousHash = "0";

        for (Transaction currentTransaction : allTransactions) {
            String actualPreviousHash = currentTransaction.getPreviousTransactionHash();

            boolean previousHashMatches = (actualPreviousHash == null && expectedPreviousHash.equals("0")) ||
                    (actualPreviousHash != null && actualPreviousHash.equals(expectedPreviousHash));

            if (!previousHashMatches) {
                System.err.println("TAMPERING DETECTED: Previous hash mismatch for transaction ID " + currentTransaction.getId());
                System.err.println("  Expected previous hash: " + expectedPreviousHash);
                System.err.println("  Actual previous hash in DB: " + (actualPreviousHash == null ? "NULL" : actualPreviousHash));
                return false;
            }

            Long sourceAccountId = (currentTransaction.getSourceAccount() != null) ? currentTransaction.getSourceAccount().getId() : null;
            Long destinationAccountId = (currentTransaction.getDestinationAccount() != null) ? currentTransaction.getDestinationAccount().getId() : null;

            String recalculatedHashInput = HashUtil.generateTransactionDataString(
                    currentTransaction.getId(),
                    currentTransaction.getTransactionType(),
                    currentTransaction.getAmount(),
                    currentTransaction.getDescription(),
                    sourceAccountId,
                    destinationAccountId,
                    currentTransaction.getTransactionDate()
            );

            // --- START SUPER DEBUG LOGGING (Verification Side) ---
            System.out.println("\nDEBUG HASH INPUT (VERIFICATION) FOR TRANSACTION ID " + currentTransaction.getId());
            System.out.println("------------------------------------------------------------------");
            System.out.println("Full String for Recalculation: [" + recalculatedHashInput + "]");
            System.out.println("Length of string: " + recalculatedHashInput.length());
            System.out.println("Character by character breakdown:");
            for (int i = 0; i < recalculatedHashInput.length(); i++) {
                char c = recalculatedHashInput.charAt(i);
                System.out.println("  Index " + i + ": Char='" + c + "' (Unicode: " + (int) c + ")");
            }
            System.out.println("------------------------------------------------------------------\n");
            // --- END SUPER DEBUG LOGGING (Verification Side) ---


            String expectedCurrentHash = HashUtil.calculateSHA256Hash(recalculatedHashInput);

            String actualCurrentHash = currentTransaction.getTransactionHash();
            if (actualCurrentHash == null || !actualCurrentHash.equals(expectedCurrentHash)) {
                System.err.println("TAMPERING DETECTED: Current hash mismatch for transaction ID " + currentTransaction.getId());
                System.err.println("  Expected current hash (recalculated): " + expectedCurrentHash);
                System.err.println("  Actual current hash in DB: " + (actualCurrentHash == null ? "NULL" : actualCurrentHash));
                System.err.println("  Data string used for recalculation: [" + recalculatedHashInput + "]");
                return false;
            }

            expectedPreviousHash = currentTransaction.getTransactionHash();
        }
        return true;
    }


    // NEW: Transaction Reversal Logic
    @Transactional // Crucial for atomicity of financial operations
    public void reverseTransaction(Long transactionId) {
        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Original transaction not found with ID: " + transactionId));

        if (originalTransaction.isReversed()) {
            throw new IllegalStateException("Transaction " + transactionId + " has already been reversed.");
        }
        if (!originalTransaction.getStatus().equals("COMPLETED")) { // Only reverse completed transactions
            throw new IllegalStateException("Transaction " + transactionId + " cannot be reversed as its status is not COMPLETED.");
        }
        if (originalTransaction.getOriginalTransactionId() != null) {
            throw new IllegalStateException("Transaction " + transactionId + " is itself a reversal and cannot be reversed again.");
        }


        // Determine source and destination accounts
        Account sourceAccount = originalTransaction.getSourceAccount();
        Account destinationAccount = originalTransaction.getDestinationAccount();
        BigDecimal amount = originalTransaction.getAmount();
        String originalDescription = originalTransaction.getDescription();
        TransactionType originalTxnType = originalTransaction.getTransactionType(); // CORRECTED LINE HERE


        String reversalDescription = "Reversal of Transaction ID " + originalTransaction.getId() + ": " + originalDescription;
        Transaction reversalTransaction = new Transaction();
        reversalTransaction.setAmount(amount);
        reversalTransaction.setTransactionDate(Instant.now()); // Set current time for reversal
        reversalTransaction.setDescription(reversalDescription);
        reversalTransaction.setStatus("COMPLETED"); // Reversal transaction is also completed
        reversalTransaction.setReversed(false); // A reversal transaction is not itself reversed
        reversalTransaction.setOriginalTransactionId(originalTransaction.getId()); // Link to original transaction

        switch (originalTxnType) {
            case TRANSFER:
                if (sourceAccount == null || destinationAccount == null) {
                    throw new IllegalArgumentException("Transfer transaction must have both source and destination accounts.");
                }
                // Reversal for Transfer: Debit original destination, Credit original source
                // Ensure original destination account has enough balance to reverse the credit
                if (destinationAccount.getBalance().compareTo(amount) < 0) {
                    throw new IllegalStateException("Destination account (" + destinationAccount.getAccountNumber() + ") has insufficient funds (" + destinationAccount.getBalance() + ") for transfer reversal of amount " + amount + ".");
                }
                destinationAccount.setBalance(destinationAccount.getBalance().subtract(amount)); // Debit original destination
                accountRepository.save(destinationAccount);

                sourceAccount.setBalance(sourceAccount.getBalance().add(amount)); // Credit original source
                accountRepository.save(sourceAccount);

                reversalTransaction.setTransactionType(TransactionType.TRANSFER_REVERSAL); // Use enum value directly
                reversalTransaction.setSourceAccount(destinationAccount); // Source of reversal is original destination
                reversalTransaction.setDestinationAccount(sourceAccount); // Destination of reversal is original source
                break;

            case DEPOSIT:
                if (destinationAccount == null) {
                    throw new IllegalArgumentException("Deposit transaction must have a destination account.");
                }
                // Reversal for Deposit: Debit the account where deposit was made
                // Ensure account has enough balance to reverse the deposit
                if (destinationAccount.getBalance().compareTo(amount) < 0) {
                    throw new IllegalStateException("Account (" + destinationAccount.getAccountNumber() + ") has insufficient funds (" + destinationAccount.getBalance() + ") for deposit reversal of amount " + amount + ".");
                }
                destinationAccount.setBalance(destinationAccount.getBalance().subtract(amount)); // Debit the account
                accountRepository.save(destinationAccount);

                reversalTransaction.setTransactionType(TransactionType.DEPOSIT_REVERSAL); // Use enum value directly
                reversalTransaction.setSourceAccount(destinationAccount); // The account is the source of the reversal outflow
                reversalTransaction.setDestinationAccount(null); // No destination for a deposit reversal (it's an outflow from the account)
                break;

            case WITHDRAWAL:
                if (sourceAccount == null) {
                    throw new IllegalArgumentException("Withdrawal transaction must have a source account.");
                }
                // Reversal for Withdrawal: Credit the account from where withdrawal was made
                sourceAccount.setBalance(sourceAccount.getBalance().add(amount)); // Credit the account
                accountRepository.save(sourceAccount);

                reversalTransaction.setTransactionType(TransactionType.WITHDRAWAL_REVERSAL); // Use enum value directly
                reversalTransaction.setSourceAccount(null); // No source for a withdrawal reversal (it's an inflow to the account)
                reversalTransaction.setDestinationAccount(sourceAccount); // The account is the destination of the reversal inflow
                break;

            default:
                throw new IllegalArgumentException("Reversal logic not implemented for transaction type: " + originalTxnType.name());
        }

        // Save the reversal transaction, which will also handle its hash generation and chaining
        createAndSaveChainedTransaction(
                reversalTransaction.getTransactionType(),
                reversalTransaction.getAmount(),
                reversalTransaction.getDescription(),
                reversalTransaction.getSourceAccount(),
                reversalTransaction.getDestinationAccount()
        );

        // Mark the original transaction as reversed
        originalTransaction.setReversed(true);
        originalTransaction.setStatus("REVERSED"); // Update status to REVERSED
        transactionRepository.save(originalTransaction);
    }
}
