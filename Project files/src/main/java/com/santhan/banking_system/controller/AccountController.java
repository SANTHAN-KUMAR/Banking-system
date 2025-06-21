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
import org.springframework.transaction.annotation.Transactional; // Import for @Transactional

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

    @GetMapping
    @Transactional
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
        accounts.forEach(account -> {
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
        });

        model.addAttribute("accounts", accounts);
        return "account-list";
    }

    @GetMapping("/create")
    public String showCreateAccountForm(Model model, Authentication authentication) {
        model.addAttribute("account", new Account());
        model.addAttribute("accountTypes", AccountType.values());
        return "account-create";
    }

    @PostMapping("/create")
    public String createAccount(@ModelAttribute("account") Account account,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        try {
            if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "Initial balance cannot be negative.");
                model.addAttribute("accountTypes", AccountType.values());
                return "account-create";
            }
            accountService.createAccount(currentUser.getId(), account);
            redirectAttributes.addFlashAttribute("success", "Account created successfully!");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", AccountType.values());
            return "account-create";
        }
    }

    @GetMapping("/details/{id}")
    @Transactional
    public String showAccountDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            return "account-details";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @GetMapping("/edit/{id}")
    @Transactional
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values());
            return "account-edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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

    @GetMapping("/{id}/deposit")
    @Transactional
    public String showDepositForm(@PathVariable Long id, Model model) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            return "deposit";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

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
            return "redirect:/accounts/" + id + "/deposit";
        }
        return "redirect:/accounts/details/" + id;
    }

    @GetMapping("/{id}/withdraw")
    @Transactional
    public String showWithdrawForm(@PathVariable Long id, Model model) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getUser() != null) {
                account.getUser().getUsername();
            }
            model.addAttribute("account", account);
            model.addAttribute("amount", BigDecimal.ZERO);
            model.addAttribute("description", "");
            return "withdraw";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

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

    @GetMapping("/{id}/transfer")
    @Transactional
    public String showTransferForm(@PathVariable Long id, Model model) {
        try {
            Account sourceAccount = accountService.getAccountById(id);
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
            return "transfer";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    @PostMapping("/{id}/transfer")
    public String transfer(@PathVariable Long id,
                           @RequestParam("destinationAccountId") Long destinationAccountId,
                           @RequestParam("amount") BigDecimal amount,
                           @RequestParam(value = "description", required = false) String description,
                           RedirectAttributes redirectAttributes) {
        try {
            transactionService.transfer(id, destinationAccountId, amount, description);
            redirectAttributes.addFlashAttribute("success", "Transfer successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id + "/transfer";
        }
        return "redirect:/accounts/details/" + id;
    }

    @GetMapping("/{id}/transactions")
    @Transactional
    public String viewAccountTransactions(@PathVariable Long id, Model model) {
        try {
            Account account = accountService.getAccountById(id);
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
            return "account-transactions";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/accounts";
        }
    }

    // NEW METHOD: Handles the general "View All My Transactions" link from the dashboard
    @GetMapping("/all-transactions") // Or just "/transactions" if you prefer
    @Transactional // Ensure lazy-loaded data is initialized
    public String viewAllMyTransactions(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        List<Transaction> allUserTransactions = transactionService.getTransactionsForUser(currentUser.getId());

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
        return "all-transactions"; // Points to a new Thymeleaf template
    }
}
