package com.santhan.banking_system.service;

import com.santhan.banking_system.model.*;
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudAlertService fraudAlertService;

    @InjectMocks
    private TransactionService transactionService;

    private Account sourceAccount;
    private Account destinationAccount;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.ROLE_CUSTOMER);

        sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setAccountNumber("ACC001");
        sourceAccount.setAccountType(AccountType.SAVINGS);
        sourceAccount.setBalance(new BigDecimal("1000.00"));
        sourceAccount.setUser(testUser);
        sourceAccount.setCreatedAt(LocalDateTime.now());
        sourceAccount.setUpdatedAt(LocalDateTime.now());

        destinationAccount = new Account();
        destinationAccount.setId(2L);
        destinationAccount.setAccountNumber("ACC002");
        destinationAccount.setAccountType(AccountType.CHECKING);
        destinationAccount.setBalance(new BigDecimal("500.00"));
        destinationAccount.setUser(testUser);
        destinationAccount.setCreatedAt(LocalDateTime.now());
        destinationAccount.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testDeposit_Success() {
        // Given
        BigDecimal depositAmount = new BigDecimal("100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.findLatestTransaction()).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        // When
        transactionService.deposit(1L, depositAmount, "Test deposit");

        // Then
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(sourceAccount);
        verify(transactionRepository).save(any(Transaction.class));
        assertEquals(new BigDecimal("1100.00"), sourceAccount.getBalance());
    }

    @Test
    void testDeposit_AccountNotFound() {
        // Given
        BigDecimal depositAmount = new BigDecimal("100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.deposit(1L, depositAmount, "Test deposit");
        });
        assertEquals("Account not found with ID: 1", exception.getMessage());
    }

    @Test
    void testDeposit_InvalidAmount() {
        // Given
        BigDecimal invalidAmount = new BigDecimal("-100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.deposit(1L, invalidAmount, "Test deposit");
        });
        assertEquals("Deposit amount must be positive.", exception.getMessage());
    }

    @Test
    void testWithdraw_Success() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.findLatestTransaction()).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        // When
        transactionService.withdraw(1L, withdrawAmount, "Test withdrawal");

        // Then
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(sourceAccount);
        verify(transactionRepository).save(any(Transaction.class));
        assertEquals(new BigDecimal("900.00"), sourceAccount.getBalance());
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("1500.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.withdraw(1L, withdrawAmount, "Test withdrawal");
        });
        assertEquals("Insufficient funds for withdrawal.", exception.getMessage());
    }

    @Test
    void testTransfer_Success() {
        // Given
        BigDecimal transferAmount = new BigDecimal("200.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.findLatestTransaction()).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        // When
        transactionService.transfer(1L, 2L, transferAmount, "Test transfer");

        // Then
        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        assertEquals(new BigDecimal("800.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), destinationAccount.getBalance());
    }

    @Test
    void testTransfer_SameAccount() {
        // Given
        BigDecimal transferAmount = new BigDecimal("200.00");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transfer(1L, 1L, transferAmount, "Test transfer");
        });
        assertEquals("Cannot transfer to the same account.", exception.getMessage());
    }

    @Test
    void testTransfer_InsufficientFunds() {
        // Given
        BigDecimal transferAmount = new BigDecimal("1500.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinationAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transfer(1L, 2L, transferAmount, "Test transfer");
        });
        assertEquals("Insufficient funds for transfer.", exception.getMessage());
    }

    @Test
    void testGetTransactionsForUser() {
        // Given
        Long userId = 1L;
        Account userAccount = new Account();
        userAccount.setId(1L);
        
        Transaction transaction1 = new Transaction(TransactionType.DEPOSIT, new BigDecimal("100.00"), "Deposit", sourceAccount, null);
        Transaction transaction2 = new Transaction(TransactionType.WITHDRAWAL, new BigDecimal("50.00"), "Withdrawal", sourceAccount, null);
        List<Transaction> mockTransactions = List.of(transaction1, transaction2);
        
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(userAccount));
        when(transactionRepository.findBySourceAccountOrDestinationAccount(userAccount, userAccount))
                .thenReturn(mockTransactions);

        // When
        List<Transaction> result = transactionService.getTransactionsForUser(userId);

        // Then
        assertEquals(2, result.size());
        verify(accountRepository).findByUserId(userId);
        verify(transactionRepository).findBySourceAccountOrDestinationAccount(userAccount, userAccount);
    }
}