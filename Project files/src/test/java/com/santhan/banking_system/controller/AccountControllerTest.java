// Create this file in: src/test/java/com/santhan/banking_system/controller/AccountControllerTest.java

package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.UserRole; // NEW: Import UserRole
import com.santhan.banking_system.model.TransactionType; // NEW: Import TransactionType
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.TransactionService;
import com.santhan.banking_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.context.annotation.Bean; // New import for @Bean

@WebMvcTest(AccountController.class)
@Import({com.santhan.banking_system.config.SecurityConfig.class, AccountControllerTest.TestConfig.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountService accountService;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void setup() {
        // Reset mocks before each test to ensure test isolation
        org.mockito.Mockito.reset(accountService, userService, transactionService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // This inner class provides the "fake" (mock) versions of our services
    @TestConfiguration
    static class TestConfig {
        @Bean
        public AccountService accountService() {
            return mock(AccountService.class);
        }

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public TransactionService transactionService() {
            return mock(TransactionService.class);
        }
    }

    // --- Helper for creating fake users and accounts ---
    private User createMockUser(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encodedPassword");
        user.setEmail(username + "@example.com");
        user.setRole(UserRole.valueOf(role)); // FIX: Use UserRole enum
        return user;
    }

    private Account createMockAccount(Long id, String accountNumber, BigDecimal balance, User user) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(accountNumber);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(balance);
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }

    private Transaction createMockTransaction(Long id, BigDecimal amount, String type, Account source, Account destination) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.valueOf(type)); // FIX: Use TransactionType enum
        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setTransactionDate(LocalDateTime.now()); // FIX: Correct method name
        return transaction;
    }

    // --- TESTS START HERE ---

    // Test for GET /accounts or /accounts/list
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void listAccounts_ReturnsAccountListPage_ForLoggedInUser() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account acc1 = createMockAccount(101L, "ACC001", BigDecimal.valueOf(1000), currentUser);
        Account acc2 = createMockAccount(102L, "ACC002", BigDecimal.valueOf(2000), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountsByUserId(currentUser.getId())).thenReturn(Arrays.asList(acc1, acc2));

        mockMvc.perform(get("/accounts/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-list"))
                .andExpect(model().attributeExists("accounts", "username"))
                .andExpect(model().attribute("username", "testuser"))
                .andExpect(model().attribute("accounts", Arrays.asList(acc1, acc2)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void listAccounts_UserNotFound_ThrowsException() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/list"))
                .andExpect(status().isInternalServerError()); // Or whatever status your global exception handler returns
        // for IllegalArgumentException. Default is 500.
    }


    // Test for GET /accounts/create
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showCreateAccountForm_ReturnsCreatePage() throws Exception {
        mockMvc.perform(get("/accounts/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-create"))
                .andExpect(model().attributeExists("account", "allAccountTypes"));
    }

    // Test for POST /accounts/create (Successful)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void createAccount_Successful_RedirectsToList() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account newAccountData = new Account("NEWACC003", AccountType.SAVINGS, BigDecimal.ZERO, null);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.createAccount(eq(currentUser.getId()), any(Account.class)))
                .thenReturn(createMockAccount(103L, "NEWACC003", BigDecimal.ZERO, currentUser));

        mockMvc.perform(post("/accounts/create")
                        .param("accountNumber", "NEWACC003")
                        .param("accountType", "SAVINGS")
                        .param("balance", "0.00")
                        .flashAttr("account", newAccountData))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("success", "Account created successfully!"));

        verify(accountService, times(1)).createAccount(eq(currentUser.getId()), any(Account.class));
    }

    // Test for POST /accounts/create (Error)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void createAccount_Error_StaysOnCreatePage() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account newAccountData = new Account("DUPLICATE", AccountType.SAVINGS, BigDecimal.valueOf(100.00), null);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.createAccount(eq(currentUser.getId()), any(Account.class)))
                .thenThrow(new IllegalArgumentException("Account with this number already exists."));

        mockMvc.perform(post("/accounts/create")
                        .param("accountNumber", "DUPLICATE")
                        .param("accountType", "SAVINGS")
                        .param("balance", "100.00")
                        .flashAttr("account", newAccountData))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/create"))
                .andExpect(flash().attribute("error", "Error creating account: Account with this number already exists."));

        verify(accountService, times(1)).createAccount(eq(currentUser.getId()), any(Account.class));
    }

    // Test for GET /accounts/details/{id} - Own Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showAccountDetails_OwnAccount_ReturnsDetailsPage() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account ownAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(5000), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(ownAccount);

        mockMvc.perform(get("/accounts/details/{id}", 101L))
                .andExpect(status().isOk())
                .andExpect(view().name("account-details"))
                .andExpect(model().attribute("account", ownAccount));
    }

    // Test for GET /accounts/details/{id} - Other User's Account (Forbidden)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showAccountDetails_OtherUsersAccount_RedirectsWithPermissionError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(5000), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(102L)).thenReturn(otherAccount);

        mockMvc.perform(get("/accounts/details/{id}", 102L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "You do not have permission to view this account."));
    }

    // Test for GET /accounts/details/{id} - Non-existent Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showAccountDetails_NonExistentAccount_RedirectsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(anyLong())).thenThrow(new IllegalArgumentException("Account not found."));

        mockMvc.perform(get("/accounts/details/{id}", 999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "Account not found."));
    }

    // Test for GET /{accountId}/deposit - Own Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showDepositForm_OwnAccount_ReturnsDepositPage() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(100), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);

        mockMvc.perform(get("/accounts/{accountId}/deposit", 101L))
                .andExpect(status().isOk())
                .andExpect(view().name("deposit"))
                .andExpect(model().attribute("account", userAccount));
    }

    // Test for GET /{accountId}/deposit - Other User's Account (Forbidden)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showDepositForm_OtherUsersAccount_RedirectsWithPermissionError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(100), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(102L)).thenReturn(otherAccount);

        mockMvc.perform(get("/accounts/{accountId}/deposit", 102L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "You do not have permission to deposit into this account."));
    }

    // Test for POST /{accountId}/deposit - Successful
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processDeposit_Successful_RedirectsToDetails() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(100), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);
        doNothing().when(transactionService).deposit(eq(101L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{accountId}/deposit", 101L)
                        .param("amount", "50.00")
                        .param("description", "Online Deposit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("success", "Deposit successful!"));

        verify(transactionService, times(1)).deposit(eq(101L), BigDecimal.valueOf(50.00), "Online Deposit");
    }

    // Test for POST /{accountId}/deposit - Error (e.g., negative amount from service)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processDeposit_Error_RedirectsToDetailsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(100), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);
        doThrow(new IllegalArgumentException("Deposit amount must be positive."))
                .when(transactionService).deposit(eq(101L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{accountId}/deposit", 101L)
                        .param("amount", "-10.00") // Simulate invalid input caught by service
                        .param("description", "Invalid deposit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("error", "Deposit amount must be positive."));

        verify(transactionService, times(1)).deposit(eq(101L), BigDecimal.valueOf(-10.00), "Invalid deposit");
    }

    // Test for GET /{accountId}/withdraw - Own Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showWithdrawForm_OwnAccount_ReturnsWithdrawPage() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(200), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);

        mockMvc.perform(get("/accounts/{accountId}/withdraw", 101L))
                .andExpect(status().isOk())
                .andExpect(view().name("withdraw"))
                .andExpect(model().attribute("account", userAccount));
    }

    // Test for GET /{accountId}/withdraw - Other User's Account (Forbidden)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showWithdrawForm_OtherUsersAccount_RedirectsWithPermissionError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(200), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(102L)).thenReturn(otherAccount);

        mockMvc.perform(get("/accounts/{accountId}/withdraw", 102L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "You do not have permission to withdraw from this account."));
    }

    // Test for POST /{accountId}/withdraw - Successful
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processWithdraw_Successful_RedirectsToDetails() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(500), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);
        doNothing().when(transactionService).withdraw(eq(101L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{accountId}/withdraw", 101L)
                        .param("amount", "100.00")
                        .param("description", "ATM Withdrawal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("success", "Withdrawal successful!"));

        verify(transactionService, times(1)).withdraw(eq(101L), BigDecimal.valueOf(100.00), "ATM Withdrawal");
    }

    // Test for POST /{accountId}/withdraw - Insufficient Balance
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processWithdraw_InsufficientBalance_RedirectsToDetailsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(50), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);
        doThrow(new IllegalArgumentException("Insufficient funds."))
                .when(transactionService).withdraw(eq(101L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{accountId}/withdraw", 101L)
                        .param("amount", "100.00")
                        .param("description", "Large withdrawal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("error", "Insufficient funds."));

        verify(transactionService, times(1)).withdraw(eq(101L), BigDecimal.valueOf(100.00), "Large withdrawal");
    }

    // Test for GET /{sourceAccountId}/transfer - Own Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showTransferForm_OwnAccount_ReturnsTransferPage() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account sourceAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(500), currentUser);
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(100), otherUser);
        Account currentUserOtherAccount = createMockAccount(103L, "ACC003", BigDecimal.valueOf(50), currentUser);


        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount);
        // getAllAccounts should return all accounts, including the current user's and others
        when(accountService.getAllAccounts()).thenReturn(Arrays.asList(sourceAccount, otherAccount, currentUserOtherAccount));


        mockMvc.perform(get("/accounts/{sourceAccountId}/transfer", 101L))
                .andExpect(status().isOk())
                .andExpect(view().name("transfer"))
                .andExpect(model().attribute("sourceAccount", sourceAccount))
                .andExpect(model().attributeExists("allAccounts"))
                .andExpect(model().attribute("allAccounts", Arrays.asList(otherAccount, currentUserOtherAccount))); // Should not include sourceAccount
    }

    // Test for GET /{sourceAccountId}/transfer - Other User's Account (Forbidden)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void showTransferForm_OtherUsersAccount_RedirectsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(100), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(102L)).thenReturn(otherAccount);

        mockMvc.perform(get("/accounts/{sourceAccountId}/transfer", 102L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "You do not have permission to transfer from this account."));
    }

    // Test for POST /{sourceAccountId}/transfer - Successful
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processTransfer_Successful_RedirectsToDetails() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account sourceAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(500), currentUser);
        Account destinationAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(100), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount);
        when(accountService.getAccountById(102L)).thenReturn(destinationAccount); // Controller also needs to retrieve dest account
        doNothing().when(transactionService).transfer(eq(101L), eq(102L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{sourceAccountId}/transfer", 101L)
                        .param("destinationAccountId", "102")
                        .param("amount", "50.00")
                        .param("description", "Gift to friend"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("success", "Transfer successful!"));

        verify(transactionService, times(1)).transfer(eq(101L), eq(102L), BigDecimal.valueOf(50.00), "Gift to friend");
    }

    // Test for POST /{sourceAccountId}/transfer - Insufficient Balance
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processTransfer_InsufficientBalance_RedirectsToDetailsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account sourceAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(20), currentUser);
        Account destinationAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(100), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount);
        when(accountService.getAccountById(102L)).thenReturn(destinationAccount);
        doThrow(new IllegalArgumentException("Insufficient funds for transfer."))
                .when(transactionService).transfer(eq(101L), eq(102L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{sourceAccountId}/transfer", 101L)
                        .param("destinationAccountId", "102")
                        .param("amount", "50.00")
                        .param("description", "Transfer attempt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("error", "Insufficient funds for transfer."));

        verify(transactionService, times(1)).transfer(eq(101L), eq(102L), BigDecimal.valueOf(50.00), "Transfer attempt");
    }

    // Test for POST /{sourceAccountId}/transfer - Source Account Not Owned by User
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processTransfer_SourceAccountNotOwned_RedirectsToAccountListWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account sourceAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(500), otherUser); // Owned by otherUser
        Account destinationAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(100), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount); // Controller will find other user's account

        mockMvc.perform(post("/accounts/{sourceAccountId}/transfer", 101L)
                        .param("destinationAccountId", "102")
                        .param("amount", "50.00")
                        .param("description", "Unauthorized transfer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "You do not have permission to transfer from this account."));

        // Verify that transactionService.transfer was NOT called
        verify(transactionService, times(0)).transfer(anyLong(), anyLong(), any(BigDecimal.class), any(String.class));
    }


    // Test for GET /{accountId}/transactions - Own Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void viewAccountTransactions_OwnAccount_ReturnsTransactionsPage() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account userAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(1000), currentUser);
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(500), createMockUser(2L, "other", "CUSTOMER"));


        Transaction t1 = createMockTransaction(1L, BigDecimal.valueOf(100), "DEPOSIT", null, userAccount);
        Transaction t2 = createMockTransaction(2L, BigDecimal.valueOf(50), "WITHDRAW", userAccount, null);
        Transaction t3 = createMockTransaction(3L, BigDecimal.valueOf(200), "TRANSFER", userAccount, otherAccount);
        Transaction t4 = createMockTransaction(4L, BigDecimal.valueOf(150), "TRANSFER", otherAccount, userAccount); // Incoming transfer

        List<Transaction> transactions = Arrays.asList(t1, t2, t3, t4);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(userAccount);
        when(transactionService.getTransactionsForAccount(101L)).thenReturn(transactions);

        mockMvc.perform(get("/accounts/{accountId}/transactions", 101L))
                .andExpect(status().isOk())
                .andExpect(view().name("account-transactions"))
                .andExpect(model().attribute("account", userAccount))
                .andExpect(model().attribute("transactions", transactions));

        verify(transactionService, times(1)).getTransactionsForAccount(101L);
    }

    // Test for GET /{accountId}/transactions - Other User's Account (Forbidden)
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void viewAccountTransactions_OtherUsersAccount_RedirectsWithPermissionError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        User otherUser = createMockUser(2L, "otheruser", "CUSTOMER");
        Account otherAccount = createMockAccount(102L, "ACC002", BigDecimal.valueOf(500), otherUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(102L)).thenReturn(otherAccount);

        mockMvc.perform(get("/accounts/{accountId}/transactions", 102L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "You do not have permission to view transactions for this account."));

        verify(transactionService, times(0)).getTransactionsForAccount(anyLong()); // Should not call service if unauthorized
    }

    // Test for GET /{accountId}/transactions - Non-existent Account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void viewAccountTransactions_NonExistentAccount_RedirectsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(anyLong())).thenThrow(new IllegalArgumentException("Account not found."));

        mockMvc.perform(get("/accounts/{accountId}/transactions", 999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/list"))
                .andExpect(flash().attribute("error", "Account not found."));

        verify(transactionService, times(0)).getTransactionsForAccount(anyLong());
    }

    // Test for POST /accounts/{sourceAccountId}/transfer - Transfer to non-existent destination account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processTransfer_NonExistentDestination_RedirectsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account sourceAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(500), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount);
        // Simulate destination account not found by service
        doThrow(new IllegalArgumentException("Destination account not found."))
                .when(transactionService).transfer(eq(101L), eq(999L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{sourceAccountId}/transfer", 101L)
                        .param("destinationAccountId", "999")
                        .param("amount", "50.00")
                        .param("description", "Transfer to non-existent account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("error", "Destination account not found."));

        verify(transactionService, times(1)).transfer(eq(101L), eq(999L), BigDecimal.valueOf(50.00), "Transfer to non-existent account");
    }

    // Test for POST /accounts/{sourceAccountId}/transfer - Transfer to same account
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void processTransfer_SameAccount_RedirectsWithError() throws Exception {
        User currentUser = createMockUser(1L, "testuser", "CUSTOMER");
        Account sourceAccount = createMockAccount(101L, "ACC001", BigDecimal.valueOf(500), currentUser);

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount);
        when(accountService.getAccountById(101L)).thenReturn(sourceAccount); // Destination is also source
        doThrow(new IllegalArgumentException("Cannot transfer to the same account."))
                .when(transactionService).transfer(eq(101L), eq(101L), any(BigDecimal.class), any(String.class));

        mockMvc.perform(post("/accounts/{sourceAccountId}/transfer", 101L)
                        .param("destinationAccountId", "101")
                        .param("amount", "10.00")
                        .param("description", "Self transfer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/details/101"))
                .andExpect(flash().attribute("error", "Cannot transfer to the same account."));

        verify(transactionService, times(1)).transfer(eq(101L), eq(101L), BigDecimal.valueOf(10.00), "Self transfer");
    }

    // Test if user not found (e.g., deleted during session) - general case for all methods
    @Test
    @WithMockUser(username = "deleteduser", roles = "CUSTOMER")
    void controllerMethod_UserNotFound_ThrowsException() throws Exception {
        when(userService.findByUsername("deleteduser")).thenReturn(Optional.empty());

        // Test any method that fetches current user (e.g., listAccounts)
        mockMvc.perform(get("/accounts/list"))
                .andExpect(status().isInternalServerError()); // Expecting 500 for IllegalArgumentException
    }
}