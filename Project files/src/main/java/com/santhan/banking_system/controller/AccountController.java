package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.User; // Ensure User is imported if you use it directly
import com.santhan.banking_system.model.Transaction; // Import Transaction
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.TransactionService; // Import TransactionService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
// Removed Optional import from here as it's handled in service, and controller
// now directly expects Account or catches IllegalArgumentException.
// If your AccountService's getAccountById still returns Optional,
// you might need to re-add 'import java.util.Optional;' and adjust the logic.

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;
    private final TransactionService transactionService;

    // This is the ONLY @Autowired constructor. Spring will use this one
    // to inject all three required service instances.
    @Autowired
    public AccountController(AccountService accountService, UserService userService, TransactionService transactionService) {
        this.accountService = accountService;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    // --- Existing Account Management Methods ---

    @GetMapping
    public String listAccounts(Model model) {
        List<Account> accounts = accountService.getAllAccounts();
        model.addAttribute("accounts", accounts);
        return "account-list";
    }

    @GetMapping("/create")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("accountTypes", AccountType.values()); // Use AccountType enum
        return "account-create";
    }

    @PostMapping("/create")
    public String createAccount(@ModelAttribute("account") Account account,
                                @RequestParam("userId") Long userId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            // Basic balance validation moved here for immediate user feedback before service call
            if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "Initial balance cannot be negative.");
                model.addAttribute("users", userService.getAllUsers());
                model.addAttribute("accountTypes", AccountType.values());
                return "account-create"; // Stay on form with error
            }
            accountService.createAccount(userId, account);
            redirectAttributes.addFlashAttribute("success", "Account created successfully!"); // Changed to 'success' for consistency with new messages
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("accountTypes", AccountType.values());
            return "account-create"; // Stay on form with error
        }
    }

    @GetMapping("/details/{id}")
    public String showAccountDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            model.addAttribute("account", account);
            return "account-details";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage()); // Changed to 'error' for consistency
            return "redirect:/accounts";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage()); // Changed to 'error' for consistency
            return "redirect:/accounts";
        }
    }

    @PostMapping("/update/{id}")
    public String updateAccount(@PathVariable Long id,
                                @ModelAttribute("account") Account account,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccountDetails(id, account);
            redirectAttributes.addFlashAttribute("success", "Account updated successfully!"); // Changed to 'success' for consistency
            return "redirect:/accounts/details/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            // Re-add necessary model attributes to keep the form populated if there's an error
            model.addAttribute("account", account); // The account object with user's input
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit";
        } catch (Exception e) { // Catch any other unexpected exceptions
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            // Re-add necessary model attributes to keep the form populated if there's an error
            model.addAttribute("account", account); // The account object with user's input
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit";
        }
    }

    // --- New Transaction-Related Methods ---

    // Show Deposit Form
    @GetMapping("/{id}/deposit")
    public String showDepositForm(@PathVariable Long id, Model model) {
        try {
            Account account = accountService.getAccountById(id);
            model.addAttribute("account", account);
            model.addAttribute("amount", BigDecimal.ZERO); // Default amount
            model.addAttribute("description", ""); // Default description
            return "deposit"; // Corresponds to deposit.html
        } catch (IllegalArgumentException e) {
            // If account not found, redirect to list with error
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    // Process Deposit
    @PostMapping("/{id}/deposit")
    public String deposit(@PathVariable Long id,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam(value = "description", required = false) String description,
                          RedirectAttributes redirectAttributes) {
        try {
            transactionService.deposit(id, amount, description);
            redirectAttributes.addFlashAttribute("success", "Deposit successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id + "/deposit"; // Redirect back to the deposit form with error
        }
        return "redirect:/accounts/details/" + id; // Redirect to account details after deposit
    }

    // Show Withdrawal Form
    @GetMapping("/{id}/withdraw")
    public String showWithdrawForm(@PathVariable Long id, Model model) {
        try {
            Account account = accountService.getAccountById(id);
            model.addAttribute("account", account);
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            return "withdraw"; // Corresponds to withdraw.html
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    // Process Withdrawal
    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id,
                           @RequestParam("amount") BigDecimal amount,
                           @RequestParam(value = "description", required = false) String description,
                           RedirectAttributes redirectAttributes) {
        try {
            transactionService.withdraw(id, amount, description);
            redirectAttributes.addFlashAttribute("success", "Withdrawal successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id + "/withdraw";
        }
        return "redirect:/accounts/details/" + id;
    }

    // Show Transfer Form
    @GetMapping("/{id}/transfer")
    public String showTransferForm(@PathVariable Long id, Model model) {
        try {
            Account sourceAccount = accountService.getAccountById(id);
            model.addAttribute("sourceAccount", sourceAccount);
            model.addAttribute("accounts", accountService.getAllAccounts()); // For selecting destination
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            return "transfer"; // Corresponds to transfer.html
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    // Process Transfer
    @PostMapping("/{id}/transfer")
    public String transfer(@PathVariable Long id, // This 'id' is the sourceAccountId
                           @RequestParam("destinationAccountId") Long destinationAccountId,
                           @RequestParam("amount") BigDecimal amount,
                           @RequestParam(value = "description", required = false) String description,
                           RedirectAttributes redirectAttributes) {
        try {
            transactionService.transfer(id, destinationAccountId, amount, description);
            redirectAttributes.addFlashAttribute("success", "Transfer successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // If error, redirect back to transfer form for source account
            return "redirect:/accounts/" + id + "/transfer";
        }
        return "redirect:/accounts/details/" + id; // Redirect to source account details after transfer
    }

    // View Account Transactions
    @GetMapping("/{id}/transactions")
    public String viewAccountTransactions(@PathVariable Long id, Model model) {
        try {
            Account account = accountService.getAccountById(id);
            List<Transaction> transactions = transactionService.getTransactionsForAccount(id);
            model.addAttribute("account", account);
            model.addAttribute("transactions", transactions);
            return "account-transactions"; // Corresponds to account-transactions.html
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }
}