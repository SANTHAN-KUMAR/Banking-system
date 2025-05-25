package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType; // THIS IMPORT IS CRUCIAL
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

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
            if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "Initial balance cannot be negative.");
                model.addAttribute("users", userService.getAllUsers());
                model.addAttribute("accountTypes", AccountType.values()); // Use AccountType enum
                return "account-create";
            }
            accountService.createAccount(userId, account);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully!");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userService.getAllUsers()); // Changed to getAllUsers() for consistency
            model.addAttribute("accountTypes", AccountType.values()); // Use AccountType enum
            return "account-create";
        }
    }

    @GetMapping("/details/{id}")
    public String viewAccountDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Account> accountOptional = accountService.getAccountById(id);
        if (accountOptional.isPresent()) {
            model.addAttribute("account", accountOptional.get());
            return "account-details";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Account not found with ID: " + id);
            return "redirect:/accounts";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditAccountForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Account> accountOptional = accountService.getAccountById(id);
        if (accountOptional.isPresent()) {
            model.addAttribute("account", accountOptional.get());
            model.addAttribute("accountTypes", AccountType.values()); // Use AccountType enum
            return "account-edit";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Account not found with ID: " + id);
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
            redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully!");
            return "redirect:/accounts/details/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values()); // Use AccountType enum
            return "account-edit";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values()); // Use AccountType enum
            return "account-edit";
        }
    }
}