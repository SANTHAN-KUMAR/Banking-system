// src/main/java/com/santhan/banking_system/controller/AdminController.java

package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.model.AccountType; // Import AccountType for account creation/editing
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Import all annotations needed (GetMapping, PostMapping, PathVariable, ModelAttribute)
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // For flash attributes

import java.util.List;
import java.util.Arrays; // For AccountType enum values

@Controller
@RequestMapping("/admin") // Base mapping for all admin-related endpoints
public class AdminController {

    private final UserService userService;
    private final AccountService accountService;

    // Use @Autowired for constructor injection (best practice)
    @Autowired
    public AdminController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/dashboard")
    // This method handles GET requests to /admin/dashboard
    // Spring Security configuration (in SecurityConfig) already ensures
    // only users with ROLE_ADMIN can access URLs under /admin/**
    public String viewAdminDashboard(Model model) {
        // Get the currently authenticated user's details for display
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Admin! This is the Admin Dashboard.");

        // Fetch all users and all accounts using your existing service methods
        List<User> allUsers = userService.getAllUsers();
        List<Account> allAccounts = accountService.getAllAccounts();

        // Add these lists to the model so they can be displayed in the Thymeleaf template
        model.addAttribute("users", allUsers);
        model.addAttribute("accounts", allAccounts);

        // Return the name of the Thymeleaf template (admin/dashboard.html)
        return "admin/dashboard";
    }

    // --- USER MANAGEMENT ---

    // 1. Display a form to edit a user
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            // Add all possible roles for a dropdown in the edit form
            model.addAttribute("allRoles", Arrays.asList("ROLE_CUSTOMER", "ROLE_EMPLOYEE", "ROLE_ADMIN"));
            return "admin/edit-user"; // This template needs to be created
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "User not found: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    // 2. Process the user update form
    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute("user") User userDetails, RedirectAttributes redirectAttributes) {
        try {
            // Call the userService.updateUser with both ID and userDetails
            userService.updateUser(id, userDetails);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/dashboard"; // Redirect back to dashboard after update
    }

    // 3. Delete a user
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/dashboard"; // Redirect back to dashboard after deletion
    }

    // --- ACCOUNT MANAGEMENT ---

    // 1. Display a form to edit an account
    @GetMapping("/accounts/edit/{id}")
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            model.addAttribute("account", account);
            model.addAttribute("allAccountTypes", AccountType.values()); // Pass all enum values for dropdown
            model.addAttribute("allUsers", userService.getAllUsers()); // Pass all users for owner selection
            return "admin/edit-account"; // This template needs to be created
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Account not found: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    // 2. Process the account update form
    @PostMapping("/accounts/update/{id}")
    public String updateAccount(@PathVariable Long id, @ModelAttribute("account") Account accountDetails, RedirectAttributes redirectAttributes) {
        try {
            // Call the accountService.updateAccountDetails with both ID and accountDetails
            accountService.updateAccountDetails(id, accountDetails);
            redirectAttributes.addFlashAttribute("success", "Account updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating account: " + e.getMessage());
        }
        return "redirect:/admin/dashboard"; // Redirect back to dashboard after update
    }

    // 3. Delete an account
    @PostMapping("/accounts/delete/{id}")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success", "Account deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
        }
        return "redirect:/admin/dashboard"; // Redirect back to dashboard after deletion
    }
}
