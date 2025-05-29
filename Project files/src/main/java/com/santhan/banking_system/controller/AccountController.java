package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/accounts")
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

    // Existing listAccounts method (already updated and working)
    @GetMapping
    public String listAccounts(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Optional<User> currentUserOptional = userService.findByUsername(currentUsername);

        if (currentUserOptional.isEmpty()) {
            model.addAttribute("error", "Could not retrieve current user information.");
            return "redirect:/login?logout";
        }

        User currentUser = currentUserOptional.get();
        List<Account> accounts = accountService.getAccountsByUserId(currentUser.getId());
        model.addAttribute("accounts", accounts);
        return "account-list";
    }

    // Modified showCreateAccountForm
    @GetMapping("/create")
    public String showCreateAccountForm(Model model, Authentication authentication) {
        // You no longer need to add all users to the model
        model.addAttribute("account", new Account());
        model.addAttribute("accountTypes", AccountType.values());
        // Optionally, you can add the current user's username for display if desired
        // model.addAttribute("currentUsername", authentication.getName());
        return "account-create";
    }

    // Modified createAccount to use the logged-in user's ID
    @PostMapping("/create")
    public String createAccount(@ModelAttribute("account") Account account,
                                // Remove @RequestParam("userId") as we will get it from security context
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName(); // Get username from logged-in user

        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found.")); // Should not happen

        try {
            if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "Initial balance cannot be negative.");
                model.addAttribute("accountTypes", AccountType.values()); // Add back for form re-display
                return "account-create";
            }
            // Pass the current user's ID to the service
            accountService.createAccount(currentUser.getId(), account);
            redirectAttributes.addFlashAttribute("success", "Account created successfully!");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", AccountType.values()); // Add back for form re-display
            return "account-create";
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