// src/test/java/com/santhan/banking_system/controller/AdminControllerTest.java

package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.UserRole;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Updated import
import org.springframework.security.test.context.support.WithMockUser; // For simulating authenticated user
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Added for User and Account constructors if they use LocalDateTime
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// This annotation configures Spring to test only AdminController, mocking its dependencies
@WebMvcTest(AdminController.class)
public class AdminControllerTest { // Class name is AdminControllerTest

    @Autowired
    private MockMvc mockMvc; // Used to perform HTTP requests in tests

    @MockitoBean // Replaced @MockBean
    private UserService userService;

    @MockitoBean // Replaced @MockBean
    private AccountService accountService;

    // Helper method to create a mock user (assuming User has a constructor for these fields)
    private User createMockUser(Long id, String username, String email, String password, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // In real app, this would be encoded
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    // Helper method to create a mock account (assuming Account has a constructor for these fields)
    private Account createMockAccount(Long id, String accountNumber, AccountType accountType, BigDecimal balance, User user) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(accountNumber);
        account.setAccountType(accountType);
        account.setBalance(balance);
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }


    @Test
    @WithMockUser(roles = "ADMIN") // Simulate an authenticated admin user
    void viewAdminDashboard_shouldReturnDashboardViewWithData() throws Exception {
        User user1 = createMockUser(1L, "user1", "user1@example.com", "pass", UserRole.ROLE_CUSTOMER);
        User user2 = createMockUser(2L, "user2", "user2@example.com", "pass", UserRole.ROLE_EMPLOYEE);
        List<User> allUsers = Arrays.asList(user1, user2);

        Account acc1 = createMockAccount(1L, "ACC001", AccountType.SAVINGS, new BigDecimal("1000.00"), user1);
        Account acc2 = createMockAccount(2L, "ACC002", AccountType.CHECKING, new BigDecimal("2000.00"), user2);
        List<Account> allAccounts = Arrays.asList(acc1, acc2);

        when(userService.getAllUsers()).thenReturn(allUsers);
        when(accountService.getAllAccounts()).thenReturn(allAccounts);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("username", "message", "users", "accounts"))
                .andExpect(model().attribute("users", allUsers))
                .andExpect(model().attribute("accounts", allAccounts));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void showEditUserForm_shouldReturnEditUserView() throws Exception {
        User user = createMockUser(1L, "testuser", "test@example.com", "password", UserRole.ROLE_CUSTOMER);
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/admin/users/edit/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/edit-user"))
                .andExpect(model().attributeExists("user", "allRoles"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("allRoles", Arrays.asList("CUSTOMER", "EMPLOYEE", "ADMIN"))); // Correct enum names
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_shouldRedirectWithSuccess() throws Exception {
        User existingUser = createMockUser(1L, "oldname", "old@example.com", "pass", UserRole.ROLE_CUSTOMER);
        // We only mock the service call, the actual update logic happens in the controller using data from params
        when(userService.getUserById(1L)).thenReturn(existingUser);
        doNothing().when(userService).updateUser(eq(1L), any(User.class));

        mockMvc.perform(post("/admin/users/update/{id}", 1L)
                        .param("username", "newname")
                        .param("email", "new@example.com")
                        .param("role", "EMPLOYEE") // Send the string value of the role
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "User updated successfully!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_shouldRedirectWithSuccess() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(post("/admin/users/delete/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "User deleted successfully!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void showEditAccountForm_shouldReturnEditAccountView() throws Exception {
        User user = createMockUser(1L, "testuser", "test@example.com", "password", UserRole.ROLE_CUSTOMER);
        Account account = createMockAccount(1L, "ACC001", AccountType.SAVINGS, new BigDecimal("1000.00"), user);

        when(accountService.getAccountById(1L)).thenReturn(account);
        when(userService.getAllUsers()).thenReturn(Arrays.asList(user));

        mockMvc.perform(get("/admin/accounts/edit/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/edit-account"))
                .andExpect(model().attributeExists("account", "allAccountTypes", "allUsers"))
                .andExpect(model().attribute("account", account));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAccount_shouldRedirectWithSuccess() throws Exception {
        User user = createMockUser(1L, "testuser", "test@example.com", "password", UserRole.ROLE_CUSTOMER);
        Account existingAccount = createMockAccount(1L, "OLDACC", AccountType.SAVINGS, new BigDecimal("500.00"), user);

        when(accountService.getAccountById(1L)).thenReturn(existingAccount);
        when(userService.getUserById(1L)).thenReturn(user); // Mock getting the user for the account
        doNothing().when(accountService).updateAccountDetails(eq(1L), any(Account.class));

        mockMvc.perform(post("/admin/accounts/update/{id}", 1L)
                        .param("accountNumber", "NEWACC")
                        .param("balance", "1500.00")
                        .param("accountType", "CHECKING") // Send the string value of the account type
                        .param("userId", "1") // Send the user ID
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Account updated successfully!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAccount_shouldRedirectWithSuccess() throws Exception {
        doNothing().when(accountService).deleteAccount(1L);

        mockMvc.perform(post("/admin/accounts/delete/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Account deleted successfully!"));
    }
}