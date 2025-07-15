package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/transaction-pin")
// FIX: Changed from hasRole('ROLE_CUSTOMER') to isAuthenticated()
// Any logged-in user should be able to manage their own PIN.
@PreAuthorize("isAuthenticated()")
public class TransactionPinController {

    private final UserService userService;

    @Autowired
    public TransactionPinController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the form to set or change the transaction PIN.
     */
    @GetMapping
    public String showSetTransactionPinForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isEmpty()) {
            model.addAttribute("error", "User not found.");
            return "redirect:/dashboard"; // Or a dedicated error page
        }

        User user = userOptional.get();
        boolean pinExists = user.getTransactionPin() != null && !user.getTransactionPin().isEmpty();
        model.addAttribute("pinExists", pinExists); // To show appropriate message (set vs change)

        return "user/set-transaction-pin"; // New Thymeleaf template
    }

    /**
     * Handles the submission of the form to set or change the transaction PIN.
     * @param newPin The new PIN entered by the user.
     * @param confirmPin The confirmation PIN entered by the user.
     * @param authentication The Spring Security authentication object.
     * @param redirectAttributes For flash messages.
     * @return Redirect to the PIN page or dashboard.
     */
    @PostMapping("/set")
    public String setTransactionPin(@RequestParam("newPin") String newPin,
                                    @RequestParam("confirmPin") String confirmPin,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (!newPin.equals(confirmPin)) {
            redirectAttributes.addFlashAttribute("error", "New PIN and Confirm PIN do not match.");
            return "redirect:/transaction-pin";
        }

        // Basic validation: PIN should be numeric and a specific length (e.g., 4-6 digits)
        if (!newPin.matches("\\d{4,6}")) { // Example: 4 to 6 digit numeric PIN
            redirectAttributes.addFlashAttribute("error", "Transaction PIN must be 4 to 6 digits long and numeric.");
            return "redirect:/transaction-pin";
        }

        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found for PIN update.");
            return "redirect:/dashboard";
        }

        Long userId = userOptional.get().getId();

        try {
            userService.setTransactionPin(userId, newPin);
            redirectAttributes.addFlashAttribute("success", "Transaction PIN updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error setting PIN: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while setting PIN.");
            e.printStackTrace();
        }
        return "redirect:/transaction-pin";
    }
}
