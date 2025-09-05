package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.TransactionType; // NEW: Import TransactionType for specific transaction forms
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // NEW: Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/accounts") // Base mapping for account-related endpoints
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;
    private final TransactionService transactionService;

    @Autowired
    public AccountController(AccountService accountService, UserService userService, TransactionService transactionService) {
        this.accountService = accountService;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    // --- Account Listing and Details ---
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional
    public String listAccounts(Model model, Authentication authentication) { // Added Authentication parameter
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

        List<Account> accounts;
        // Check if the current user is a customer to show only their accounts
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            accounts = accountService.getAccountsByUserId(currentUser.getId());
        } else {
            // For Employee/Admin, show all accounts
            accounts = accountService.getAllAccounts();
        }

        // Initialize user data for Thymeleaf (if lazy loaded)
        accounts.forEach(account -> {
            if (account.getUser() != null) {
                account.getUser().getUsername(); // Access to initialize
            }
        });

        model.addAttribute("accounts", accounts);
        return "account-list"; // Existing template name
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Only employees/admins can create accounts
    public String showCreateAccountForm(Model model) { // Removed Authentication parameter as it's not directly used here
        model.addAttribute("account", new Account());
        model.addAttribute("accountTypes", AccountType.values());
        // You might want to pass a list of users for selection here if employees/admins create accounts for specific users
        model.addAttribute("users", userService.getAllUsers());
        return "account-create"; // Existing template name
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Only employees/admins can create accounts
    public String createAccount(@ModelAttribute("account") Account account,
                                @RequestParam("userId") Long userId, // Explicitly expect userId from form
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "Initial balance cannot be negative.");
                model.addAttribute("accountTypes", AccountType.values());
                model.addAttribute("users", userService.getAllUsers()); // Re-add users for form if error
                return "account-create";
            }
            accountService.createAccount(userId, account); // Use the provided userId
            redirectAttributes.addFlashAttribute("success", "Account created successfully!");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("users", userService.getAllUsers()); // Re-add users for form if error
            return "account-create";
        }
    }

    @GetMapping("/details/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional
    public String showAccountDetails(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + authentication.getName()));

            // Security check for customers: ensure they only view their own accounts
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!account.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to view this account.");
                    return "redirect:/accounts"; // Redirect to their own account list
                }
            }

            // Initialize user data for Thymeleaf (if lazy loaded)
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            return "account-details"; // Existing template name
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Only employees/admins can edit accounts
    @Transactional
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit"; // Existing template name
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Only employees/admins can update accounts
    public String updateAccount(@PathVariable Long id,
                                @ModelAttribute("account") Account account,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccountDetails(id, account);
            redirectAttributes.addFlashAttribute("success", "Account updated successfully!");
            return "redirect:/accounts/details/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit";
        }
    }

    // --- Deposit Operations ---
    @GetMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional
    public String showDepositForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // For customers, ensure they own the account
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!account.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to deposit to this account.");
                    return "redirect:/accounts";
                }
            }

            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            model.addAttribute("transactionType", TransactionType.DEPOSIT); // Added for context in HTML if needed
            return "deposit"; // Existing template name
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    public String deposit(@PathVariable Long id,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam(value = "description", required = false) String description,
                          @RequestParam("transactionPin") String transactionPin, // NEW: Transaction PIN
                          Authentication authentication, // NEW: Added Authentication for current user
                          RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

            // NEW: Verify Transaction PIN
            if (!userService.verifyTransactionPin(currentUser.getId(), transactionPin)) {
                redirectAttributes.addFlashAttribute("error", "Invalid Transaction PIN.");
                return "redirect:/accounts/" + id + "/deposit";
            }

            // For customers, ensure they own the account they are depositing to
            Account targetAccount = accountService.getAccountById(id);
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!targetAccount.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to deposit to this account.");
                    return "redirect:/accounts";
                }
            }

            transactionService.deposit(id, amount, description);
            redirectAttributes.addFlashAttribute("success", "Deposit successful!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id + "/deposit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/accounts/" + id + "/deposit";
        }
        return "redirect:/accounts/details/" + id; // Redirect to account details on success
    }

    // --- Withdrawal Operations ---
    @GetMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional
    public String showWithdrawForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // For customers, ensure they own the account
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!account.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to withdraw from this account.");
                    return "redirect:/accounts";
                }
            }

            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            model.addAttribute("transactionType", TransactionType.WITHDRAWAL); // Added for context in HTML if needed
            return "withdraw"; // Existing template name
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    public String withdraw(@PathVariable Long id,
                           @RequestParam("amount") BigDecimal amount,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam("transactionPin") String transactionPin, // NEW: Transaction PIN
                           Authentication authentication, // NEW: Added Authentication for current user
                           RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

            // NEW: Verify Transaction PIN
            if (!userService.verifyTransactionPin(currentUser.getId(), transactionPin)) {
                redirectAttributes.addFlashAttribute("error", "Invalid Transaction PIN.");
                return "redirect:/accounts/" + id + "/withdraw";
            }

            // For customers, ensure they own the account they are withdrawing from
            Account sourceAccount = accountService.getAccountById(id);
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!sourceAccount.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to withdraw from this account.");
                    return "redirect:/accounts";
                }
            }

            transactionService.withdraw(id, amount, description);
            redirectAttributes.addFlashAttribute("success", "Withdrawal successful!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id + "/withdraw";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/accounts/" + id + "/withdraw";
        }
        return "redirect:/accounts/details/" + id;
    }

    // --- Transfer Operations ---
    @GetMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional
    public String showTransferForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Account sourceAccount = accountService.getAccountById(id);
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // For customers, ensure they own the account
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!sourceAccount.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to transfer from this account.");
                    return "redirect:/accounts";
                }
            }

            if (sourceAccount.getUser() != null) {
                sourceAccount.getUser().getUsername();
            }
            model.addAttribute("sourceAccount", sourceAccount);

            List<Account> allAccounts = accountService.getAllAccounts();
            allAccounts.forEach(account -> {
                if (account.getUser() != null) {
                    account.getUser().getUsername();
                }
            });
            model.addAttribute("accounts", allAccounts);
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            model.addAttribute("transactionType", TransactionType.TRANSFER); // Added for context in HTML if needed
            return "transfer"; // Existing template name
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    public String transfer(@PathVariable Long id,
                           @RequestParam("destinationAccountId") Long destinationAccountId,
                           @RequestParam("amount") BigDecimal amount,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam("transactionPin") String transactionPin, // NEW: Transaction PIN
                           Authentication authentication, // NEW: Added Authentication for current user
                           RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

            // NEW: Verify Transaction PIN
            if (!userService.verifyTransactionPin(currentUser.getId(), transactionPin)) {
                redirectAttributes.addFlashAttribute("error", "Invalid Transaction PIN.");
                return "redirect:/accounts/" + id + "/transfer";
            }

            // For customers, ensure they own the source account
            Account sourceAccount = accountService.getAccountById(id);
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!sourceAccount.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to transfer from this account.");
                    return "redirect:/accounts";
                }
            }

            transactionService.transfer(id, destinationAccountId, amount, description);
            redirectAttributes.addFlashAttribute("success", "Transfer successful!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id + "/transfer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/accounts/" + id + "/transfer";
        }
        return "redirect:/accounts/details/" + id;
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional
    public String viewAccountTransactions(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // Security check for customers: ensure they only view their own account transactions
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!account.getUser().getId().equals(currentUser.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to view transactions for this account.");
                    return "redirect:/accounts"; // Redirect to their own account list
                }
            }

            if (account.getUser() != null) {
                account.getUser().getUsername();
            }

            List<Transaction> transactions = transactionService.getTransactionsForAccount(id);
            transactions.forEach(transaction -> {
                if (transaction.getSourceAccount() != null && transaction.getSourceAccount().getUser() != null) {
                    transaction.getSourceAccount().getUser().getUsername();
                }
                if (transaction.getDestinationAccount() != null && transaction.getDestinationAccount().getUser() != null) {
                    transaction.getDestinationAccount().getUser().getUsername();
                }
            });

            model.addAttribute("account", account);
            model.addAttribute("transactions", transactions);
            return "account-transactions"; // Existing template name
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    // Handles the general "View All My Transactions" link from the dashboard
    @GetMapping("/all-transactions")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')") // Accessible by all relevant roles
    @Transactional // Ensure lazy-loaded data is initialized
    public String viewAllMyTransactions(Model model, Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        List<Transaction> allUserTransactions;

        // If customer, show only their transactions. If admin/employee, show all transactions.
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            allUserTransactions = transactionService.getTransactionsForUser(currentUser.getId());
        } else {
            allUserTransactions = transactionService.getAllTransactions(); // Assuming you have this method or need to create it
        }

        // Ensure associated accounts and users are initialized for the template
        allUserTransactions.forEach(transaction -> {
            if (transaction.getSourceAccount() != null && transaction.getSourceAccount().getUser() != null) {
                transaction.getSourceAccount().getUser().getUsername();
            }
            if (transaction.getDestinationAccount() != null && transaction.getDestinationAccount().getUser() != null) {
                transaction.getDestinationAccount().getUser().getUsername();
            }
        });

        model.addAttribute("transactions", allUserTransactions);
        model.addAttribute("title", "All My Transactions"); // Title for the page
        return "all-transactions"; // Existing template name
    }
}
