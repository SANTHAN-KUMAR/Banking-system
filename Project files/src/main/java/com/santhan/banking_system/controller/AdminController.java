package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.model.AccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional; // Import for @Transactional

import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final AccountService accountService;

    @Autowired
    public AdminController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/dashboard")
    @Transactional // FIX: Ensure this method runs within a transaction to prevent LazyInitializationException
    public String viewAdminDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Admin! This is the Admin Dashboard.");

        List<User> allUsers = userService.getAllUsers();
        // FIX: Explicitly access lazy-loaded fields within the transaction for Thymeleaf
        allUsers.forEach(user -> {
            if (user.getRole() != null) {
                user.getRole().name(); // Access to initialize role
            }
        });

        List<Account> allAccounts = accountService.getAllAccounts();
        // FIX: Explicitly access lazy-loaded fields within the transaction for Thymeleaf
        allAccounts.forEach(account -> {
            if (account.getUser() != null) {
                account.getUser().getUsername(); // Access to initialize user within account
            }
        });

        model.addAttribute("users", allUsers);
        model.addAttribute("accounts", allAccounts);

        return "admin/dashboard";
    }

    // --- USER MANAGEMENT ---

    @GetMapping("/users/edit/{id}")
    @Transactional // FIX: Add transactional if lazy-loaded fields are accessed in edit-user.html
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            // Ensure user's role is initialized if edit-user.html uses it
            if (user.getRole() != null) {
                user.getRole().name();
            }
            model.addAttribute("user", user);
            model.addAttribute("allRoles", Arrays.asList("ROLE_CUSTOMER", "ROLE_EMPLOYEE", "ROLE_ADMIN"));
            return "admin/edit-user";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "User not found: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute("user") User userDetails, RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, userDetails);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // --- ACCOUNT MANAGEMENT ---

    @GetMapping("/accounts/edit/{id}")
    @Transactional // FIX: Add transactional if lazy-loaded fields are accessed in edit-account.html
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            // Ensure account's user is initialized if edit-account.html uses it
            if (account.getUser() != null) {
                account.getUser().getUsername(); // Access to initialize user
            }
            model.addAttribute("account", account);
            model.addAttribute("allAccountTypes", AccountType.values());
            List<User> allUsers = userService.getAllUsers();
            // Ensure all users fetched for owner selection have their roles initialized
            allUsers.forEach(user -> {
                if (user.getRole() != null) {
                    user.getRole().name();
                }
            });
            model.addAttribute("allUsers", allUsers);
            return "admin/edit-account";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Account not found: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/accounts/update/{id}")
    public String updateAccount(@PathVariable Long id, @ModelAttribute("account") Account accountDetails, RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccountDetails(id, accountDetails);
            redirectAttributes.addFlashAttribute("success", "Account updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating account: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/accounts/delete/{id}")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success", "Account deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
