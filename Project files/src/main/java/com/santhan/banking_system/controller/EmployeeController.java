package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account; // NEW: Import Account
import com.santhan.banking_system.model.User;    // NEW: Import User
import com.santhan.banking_system.service.AccountService; // NEW: Import AccountService
import com.santhan.banking_system.service.UserService;    // NEW: Import UserService
import org.springframework.beans.factory.annotation.Autowired; // NEW: Import Autowired
import org.springframework.security.access.prepost.PreAuthorize; // NEW: Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.transaction.annotation.Transactional; // NEW: Import Transactional

import java.util.List; // NEW: Import List

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    // NEW: Inject UserService and AccountService
    private final UserService userService;
    private final AccountService accountService;

    @Autowired // Constructor injection
    public EmployeeController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')") // Explicitly allow ADMIN and EMPLOYEE
    public String viewEmployeeDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Employee! This is the Employee Dashboard.");
        // Add any employee-specific data to the model here
        return "employee/dashboard"; // Assuming employee/dashboard.html
    }

    // NEW: View all users (for employees) - no direct edit/delete from this view
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional // To fetch lazy-loaded user roles if needed in the view
    public String viewAllUsersForEmployee(Model model) {
        List<User> allUsers = userService.getAllUsers();
        // Ensure roles are initialized for Thymeleaf
        allUsers.forEach(user -> {
            if (user.getRole() != null) {
                user.getRole().name();
            }
        });
        model.addAttribute("users", allUsers);
        model.addAttribute("title", "All System Users (Employee View)");
        return "employee/user-list"; // A new template for employee user list
    }

    // NEW: View all accounts (for employees) - no direct edit/delete from this view
    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional // To fetch lazy-loaded user details in accounts
    public String viewAllAccountsForEmployee(Model model) {
        List<Account> allAccounts = accountService.getAllAccounts();
        // Ensure user details in accounts are initialized for Thymeleaf
        allAccounts.forEach(account -> {
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
        });
        model.addAttribute("accounts", allAccounts);
        model.addAttribute("title", "All System Accounts (Employee View)");
        return "employee/account-list"; // A new template for employee account list
    }
}
