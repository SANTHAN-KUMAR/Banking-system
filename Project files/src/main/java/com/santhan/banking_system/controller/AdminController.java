// src/main/java/com/santhan/banking_system/controller/AdminController.java

package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User; // Import User model
import com.santhan.banking_system.model.Account; // Import Account model
import com.santhan.banking_system.service.UserService; // Import UserService
import com.santhan.banking_system.service.AccountService; // Import AccountService
import org.springframework.beans.factory.annotation.Autowired; // For @Autowired
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
// You might add more imports later for @PathVariable, @PostMapping etc.

@Controller
@RequestMapping("/admin") // Base mapping for all admin-related endpoints
public class AdminController {

    private final UserService userService;
    private final AccountService accountService;

    // Use @Autowired for constructor injection
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

    // --- FUTURE ADMIN PRIVILEGES (Placeholders) ---
    // You will add more methods here to handle typical admin actions:

    // Example: Display a form to edit a user
    // @GetMapping("/users/edit/{id}")
    // public String showEditUserForm(@PathVariable Long id, Model model) {
    //     User user = userService.getUserById(id);
    //     model.addAttribute("user", user);
    //     return "admin/edit-user"; // Needs a new Thymeleaf template: admin/edit-user.html
    // }

    // Example: Process the user update form
    // @PostMapping("/users/update/{id}")
    // public String updateUser(@PathVariable Long id, @ModelAttribute("user") User userDetails) {
    //     userService.updateUser(id, userDetails);
    //     return "redirect:/admin/dashboard"; // Redirect back to dashboard after update
    // }

    // Example: Delete a user
    // @PostMapping("/users/delete/{id}")
    // public String deleteUser(@PathVariable Long id) {
    //     userService.deleteUser(id);
    //     return "redirect:/admin/dashboard"; // Redirect back to dashboard after deletion
    // }

    // (Similar methods would be added for account management)
}