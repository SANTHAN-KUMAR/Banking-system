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
class TransactionServiceIntegrationTest {

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
    void testDepositValidation_NegativeAmount() {
        // Given
        BigDecimal invalidAmount = new BigDecimal("-100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.deposit(1L, invalidAmount, "Test deposit");
        });
        assertTrue(exception.getMessage().contains("Deposit amount must be positive"));
    }

    @Test
    void testWithdrawValidation_InsufficientFunds() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("1500.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.withdraw(1L, withdrawAmount, "Test withdrawal");
        });
        assertTrue(exception.getMessage().contains("Insufficient funds for withdrawal"));
    }

    @Test
    void testTransferValidation_SameAccount() {
        // Given
        BigDecimal transferAmount = new BigDecimal("200.00");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transfer(1L, 1L, transferAmount, "Test transfer");
        });
        assertTrue(exception.getMessage().contains("Cannot transfer to the same account"));
    }

    @Test
    void testAccountNotFound() {
        // Given
        BigDecimal depositAmount = new BigDecimal("100.00");
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.deposit(999L, depositAmount, "Test deposit");
        });
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void testGetTransactionsForUser_Integration() {
        // Given
        Long userId = 1L;
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(sourceAccount));
        when(transactionRepository.findBySourceAccountOrDestinationAccount(sourceAccount, sourceAccount))
                .thenReturn(List.of());

        // When
        List<Transaction> result = transactionService.getTransactionsForUser(userId);

        // Then
        assertNotNull(result);
        verify(accountRepository).findByUserId(userId);
    }
}