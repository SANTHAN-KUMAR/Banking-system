package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.KycStatus;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.FraudAlert;
import com.santhan.banking_system.model.FraudAlert.AlertStatus;
import com.santhan.banking_system.model.Transaction; // Import Transaction model

import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.TransactionService; // Import TransactionService
import com.santhan.banking_system.service.FraudAlertService;

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
@PreAuthorize("hasRole('ADMIN')") // Apply base authorization for the entire controller
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
    @Transactional
    public String viewAdminDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Admin! This is the Admin Dashboard.");

        List<User> allUsers = userService.getAllUsers();
        allUsers.forEach(user -> {
            if (user.getRole() != null) {
                user.getRole().name(); // Eagerly fetch role if needed for display
            }
        });

        List<Account> allAccounts = accountService.getAllAccounts();
        allAccounts.forEach(account -> {
            if (account.getUser() != null) {
                account.getUser().getUsername(); // Eagerly fetch username if needed for display
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

    // --- TRANSACTION MANAGEMENT (NEW/UPDATED SECTION) ---

    @GetMapping("/transactions")
    @Transactional(readOnly = true) // Use readOnly for GET requests to optimize
    public String manageTransactions(Model model) {
        List<Transaction> transactions = transactionService.getAllTransactions();
        // Eagerly fetch associated accounts and users for display in the view
        transactions.forEach(transaction -> {
            if (transaction.getSourceAccount() != null) {
                transaction.getSourceAccount().getAccountNumber();
                if (transaction.getSourceAccount().getUser() != null) {
                    transaction.getSourceAccount().getUser().getUsername();
                }
            }
            if (transaction.getDestinationAccount() != null) {
                transaction.getDestinationAccount().getAccountNumber();
                if (transaction.getDestinationAccount().getUser() != null) {
                    transaction.getDestinationAccount().getUser().getUsername();
                }
            }
        });
        model.addAttribute("transactions", transactions);
        return "admin/transactions"; // Ensure you have a Thymeleaf template named transactions.html
    }

    // NEW: Endpoint to trigger transaction reversal
    @PostMapping("/transactions/reverse/{transactionId}")
    public String reverseTransaction(@PathVariable Long transactionId, RedirectAttributes redirectAttributes) {
        try {
            transactionService.reverseTransaction(transactionId);
            redirectAttributes.addFlashAttribute("success", "Transaction " + transactionId + " reversed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error reversing transaction " + transactionId + ": " + e.getMessage());
            e.printStackTrace(); // Log the error for debugging
        }
        return "redirect:/admin/transactions"; // Redirect back to the transaction list
    }

    // --- LEDGER INTEGRITY VERIFICATION ---
    @PostMapping("/verify-ledger")
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

    // --- KYC MANAGEMENT ---
    @GetMapping("/kyc-pending")
    public String listPendingKycSubmissions(Model model) {
        List<User> pendingKycUsers = userService.getUsersByKycStatus(KycStatus.PENDING);
        model.addAttribute("pendingKycUsers", pendingKycUsers);
        model.addAttribute("kycStatusMessage", "Users with Pending KYC Submissions");
        return "admin/admin-kyc-pending-list";
    }

    @GetMapping("/kyc-review/{id}")
    @Transactional
    public String showKycReviewDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found for KYC review.");
                return "redirect:/admin/kyc-pending";
            }
            user.getFirstName();
            user.getLastName();
            user.getAddress();
            user.getNationalIdNumber();
            user.getDocumentType();
            user.getDateOfBirth();
            user.getKycSubmissionDate();

            model.addAttribute("userToReview", user);
            model.addAttribute("allKycStatuses", KycStatus.values());
            return "admin/kyc-review-details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error fetching KYC details: " + e.getMessage());
            return "redirect:/admin/kyc-pending";
        }
    }

    // --- FRAUD ALERT MANAGEMENT ---
    @GetMapping("/fraud-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional
    public String manageFraudAlerts(@RequestParam(value = "status", required = false) String statusFilter, Model model) {
        List<FraudAlert> alerts;
        AlertStatus selectedStatus = null;

        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                selectedStatus = AlertStatus.valueOf(statusFilter.toUpperCase());
                alerts = fraudAlertService.getAlertsByStatus(selectedStatus);
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Invalid alert status provided: " + statusFilter);
                alerts = fraudAlertService.getAllAlerts();
            }
        } else {
            alerts = fraudAlertService.getAllAlerts();
        }

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
        model.addAttribute("allAlertStatuses", AlertStatus.values());
        model.addAttribute("selectedStatus", selectedStatus);

        return "admin/fraud-alerts";
    }

    // --- FRAUD ALERT ACTIONS ---
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
        return "redirect:/admin/fraud-alerts";
    }

    @PostMapping("/alerts/delete/{id}")
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
