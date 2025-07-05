package com.santhan.banking_system.service;

import com.santhan.banking_system.model.*;
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.ROLE_CUSTOMER);

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("ACC001");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setUser(testUser);
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateAccount_Success() {
        // Given
        Account newAccount = new Account();
        newAccount.setAccountType(AccountType.CHECKING);
        newAccount.setBalance(new BigDecimal("500.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // When
        Account result = accountService.createAccount(1L, newAccount);

        // Then
        assertNotNull(result);
        assertEquals(AccountType.CHECKING, result.getAccountType());
        assertEquals(new BigDecimal("500.00"), result.getBalance());
        assertEquals(testUser, result.getUser());
        assertNotNull(result.getAccountNumber());
        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testCreateAccount_UserNotFound() {
        // Given
        Account newAccount = new Account();
        newAccount.setAccountType(AccountType.CHECKING);
        newAccount.setBalance(new BigDecimal("500.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount(1L, newAccount);
        });
        assertEquals("User not found with ID: 1", exception.getMessage());
    }

    @Test
    void testCreateAccount_NegativeBalance() {
        // Given
        Account newAccount = new Account();
        newAccount.setAccountType(AccountType.CHECKING);
        newAccount.setBalance(new BigDecimal("-100.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // When
        Account result = accountService.createAccount(1L, newAccount);

        // Then
        assertEquals(BigDecimal.ZERO, result.getBalance());
    }

    @Test
    void testGetAccountById_Success() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When
        Account result = accountService.getAccountById(1L);

        // Then
        assertEquals(testAccount, result);
        verify(accountRepository).findById(1L);
    }

    @Test
    void testGetAccountById_NotFound() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.getAccountById(1L);
        });
        assertEquals("Account not found with ID: 1", exception.getMessage());
    }

    @Test
    void testGetAccountsByUserId() {
        // Given
        List<Account> userAccounts = List.of(testAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);

        // When
        List<Account> result = accountService.getAccountsByUserId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(testAccount, result.get(0));
        verify(userRepository).findById(1L);
        verify(accountRepository).findByUser(testUser);
    }

    @Test
    void testGetAllAccounts() {
        // Given
        List<Account> allAccounts = List.of(testAccount);
        when(accountRepository.findAll()).thenReturn(allAccounts);

        // When
        List<Account> result = accountService.getAllAccounts();

        // Then
        assertEquals(1, result.size());
        assertEquals(testAccount, result.get(0));
        verify(accountRepository).findAll();
    }

    @Test
    void testUpdateAccountDetails_Success() {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(AccountType.CHECKING);
        updatedAccount.setUser(testUser);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Account result = accountService.updateAccountDetails(1L, updatedAccount);

        // Then
        assertEquals(AccountType.CHECKING, result.getAccountType());
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(testAccount);
    }

    @Test
    void testUpdateAccountDetails_NotFound() {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(AccountType.CHECKING);

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.updateAccountDetails(1L, updatedAccount);
        });
        assertEquals("Account not found with ID: 1", exception.getMessage());
    }

    @Test
    void testDeleteAccount_Success() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findBySourceAccount_IdOrDestinationAccount_Id(1L, 1L))
                .thenReturn(List.of());

        // When
        accountService.deleteAccount(1L);

        // Then
        verify(accountRepository).findById(1L);
        verify(transactionRepository).findBySourceAccount_IdOrDestinationAccount_Id(1L, 1L);
        verify(accountRepository).deleteById(1L);
    }

    @Test
    void testDeleteAccount_NotFound() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deleteAccount(1L);
        });
        assertEquals("Account not found with ID: 1", exception.getMessage());
    }

    @Test
    void testDeleteAccount_HasTransactions() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findBySourceAccount_IdOrDestinationAccount_Id(1L, 1L))
                .thenReturn(List.of(transaction));

        // When
        accountService.deleteAccount(1L);

        // Then
        verify(accountRepository).findById(1L);
        verify(transactionRepository).findBySourceAccount_IdOrDestinationAccount_Id(1L, 1L);
        verify(transactionRepository).deleteAll(List.of(transaction));
        verify(accountRepository).deleteById(1L);
    }
}