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
import java.time.Instant; // Changed to Instant
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertService fraudAlertService; // NEW: Inject FraudAlertService

    @Autowired
    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              FraudAlertService fraudAlertService) { // NEW: Inject FraudAlertService
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertService = fraudAlertService; // Initialize FraudAlertService
    }

    private Transaction createAndSaveChainedTransaction(
            TransactionType type, BigDecimal amount, String description,
            Account sourceAccount, Account destinationAccount) {

        Transaction newTransaction = new Transaction(type, amount, description, sourceAccount, destinationAccount);

        Optional<Transaction> latestTransactionOptional = transactionRepository.findLatestTransaction();
        String previousHash = latestTransactionOptional.map(Transaction::getTransactionHash).orElse("0");

        newTransaction.setPreviousTransactionHash(previousHash);

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
        account.setUpdatedAt(LocalDateTime.now());
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
        account.setUpdatedAt(LocalDateTime.now());
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
        String baseDescriptionForTransferType = description != null ? description : "Funds Transfer";

        Account sourceAccount = withdraw(sourceAccountId, amount, "Transfer related withdrawal");
        Account destinationAccount = deposit(destinationAccountId, amount, "Transfer related deposit");

        createAndSaveChainedTransaction(
                TransactionType.TRANSFER,
                amount,
                baseDescriptionForTransferType,
                sourceAccount,
                destinationAccount
        );
    }

    public List<Transaction> getTransactionsForAccount(Long accountId) {
        // Updated to use findAllByOrderByTransactionDateAscIdAsc from TransactionRepository
        // This makes sure we fetch transactions including their associated accounts/users.
        // Then filter them by the accountId as source or destination.
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
}
