package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.TransactionService;
import com.santhan.banking_system.service.FraudAlertService;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.FraudAlert;
import com.santhan.banking_system.model.FraudAlert.AlertStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final FraudAlertService fraudAlertService;

    @Autowired
    public AdminController(UserService userService, AccountService accountService,
                           TransactionService transactionService,
                           FraudAlertService fraudAlertService) {
        this.userService = userService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.fraudAlertService = fraudAlertService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String viewAdminDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Admin! This is the Admin Dashboard.");

        List<User> allUsers = userService.getAllUsers();
        allUsers.forEach(user -> {
            if (user.getRole() != null) {
                user.getRole().name();
            }
        });

        List<Account> allAccounts = accountService.getAllAccounts();
        allAccounts.forEach(account -> {
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
        });

        List<FraudAlert> pendingAlerts = fraudAlertService.getAlertsByStatus(AlertStatus.PENDING);
        pendingAlerts.forEach(alert -> {
            if (alert.getTransaction() != null) {
                if (alert.getTransaction().getSourceAccount() != null) {
                    alert.getTransaction().getSourceAccount().getAccountNumber();
                }
                if (alert.getTransaction().getDestinationAccount() != null) {
                    alert.getTransaction().getDestinationAccount().getAccountNumber();
                }
            }
        });


        model.addAttribute("users", allUsers);
        model.addAttribute("accounts", allAccounts);
        model.addAttribute("pendingAlerts", pendingAlerts);

        return "admin/dashboard";
    }

    // --- USER MANAGEMENT ---

    @GetMapping("/users/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("allAccountTypes", AccountType.values());

            List<User> allUsers = userService.getAllUsers();
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success", "Account deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // --- LEDGER INTEGRITY VERIFICATION ---
    @PostMapping("/verify-ledger")
    @PreAuthorize("hasRole('ADMIN')")
    public String verifyLedger(RedirectAttributes redirectAttributes) {
        try {
            boolean isLedgerIntact = transactionService.verifyLedgerIntegrity();
            if (isLedgerIntact) {
                redirectAttributes.addFlashAttribute("success", "Transaction ledger integrity verified successfully. No tampering detected.");
            } else {
                redirectAttributes.addFlashAttribute("error", "WARNING: Transaction ledger integrity check FAILED! Tampering detected.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error verifying ledger integrity: " + e.getMessage());
            System.err.println("Error verifying ledger: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/admin/dashboard";
    }

    // --- FRAUD ALERT MANAGEMENT (Dedicated Page) ---
    @GetMapping("/fraud-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')") // Admins and Employees can view all alerts
    @Transactional // Ensure lazy loading works for transaction and its associated accounts/users
    public String manageFraudAlerts(@RequestParam(value = "status", required = false) String statusFilter, Model model) {
        List<FraudAlert> alerts;
        AlertStatus selectedStatus = null;

        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                selectedStatus = AlertStatus.valueOf(statusFilter.toUpperCase());
                alerts = fraudAlertService.getAlertsByStatus(selectedStatus);
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Invalid alert status provided: " + statusFilter);
                alerts = fraudAlertService.getAllAlerts(); // Fallback to all alerts
            }
        } else {
            alerts = fraudAlertService.getAllAlerts(); // Default to all alerts if no filter
        }

        // Manually initialize lazy-loaded data for display in Thymeleaf
        alerts.forEach(alert -> {
            if (alert.getTransaction() != null) {
                if (alert.getTransaction().getSourceAccount() != null) {
                    alert.getTransaction().getSourceAccount().getAccountNumber();
                    if (alert.getTransaction().getSourceAccount().getUser() != null) {
                        alert.getTransaction().getSourceAccount().getUser().getUsername();
                    }
                }
                if (alert.getTransaction().getDestinationAccount() != null) {
                    alert.getTransaction().getDestinationAccount().getAccountNumber();
                    if (alert.getTransaction().getDestinationAccount().getUser() != null) {
                        alert.getTransaction().getDestinationAccount().getUser().getUsername();
                    }
                }
            }
        });

        model.addAttribute("alerts", alerts);
        model.addAttribute("allAlertStatuses", AlertStatus.values()); // For the filter dropdown
        model.addAttribute("selectedStatus", selectedStatus); // To pre-select in the dropdown

        return "admin/fraud-alerts"; // New Thymeleaf template
    }


    // --- FRAUD ALERT ACTIONS (from dashboard or dedicated page) ---
    @PostMapping("/alerts/update-status/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String updateAlertStatus(@PathVariable Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes) {
        try {
            FraudAlert.AlertStatus newStatus = AlertStatus.valueOf(status.toUpperCase());
            fraudAlertService.updateAlertStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("success", "Alert ID " + id + " status updated to " + newStatus.getDisplayName() + ".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid status or Alert not found: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating alert status: " + e.getMessage());
        }
        // Redirect back to the fraud alerts page, preserving filter if possible
        return "redirect:/admin/fraud-alerts";
    }

    @PostMapping("/alerts/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAlert(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fraudAlertService.deleteAlert(id);
            redirectAttributes.addFlashAttribute("success", "Alert ID " + id + " deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Alert not found: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting alert: " + e.getMessage());
        }
        return "redirect:/admin/fraud-alerts";
    }
}
