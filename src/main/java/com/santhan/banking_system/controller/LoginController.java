package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

    private final UserService userService;

    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        System.out.println("DEBUG: GET /login endpoint hit.");
        if (error != null) {
            model.addAttribute("error", "Invalid username or password, or account not verified.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        System.out.println("DEBUG: GET /register endpoint hit. Preparing registration form.");
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes) {
        System.out.println("DEBUG: POST /register endpoint hit!");
        System.out.println("DEBUG: Received User object from form: ");
        System.out.println("DEBUG:   Username: '" + user.getUsername() + "'");
        System.out.println("DEBUG:   Email:    '" + user.getEmail() + "'");
        System.out.println("DEBUG:   Mobile:   '" + user.getMobileNumber() + "'");
        System.out.println("DEBUG:   Password: '[PROTECTED]'");

        if (result.hasErrors()) {
            System.err.println("ERROR: Registration form validation errors: " + result.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        }

        try {
            User registeredUser = userService.createUser(user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! An email verification OTP has been sent to " + registeredUser.getEmail() + ". Please verify your email to activate your account.");
            return "redirect:/verify-email?username=" + registeredUser.getUsername();
        } catch (IllegalArgumentException e) { // For username/email/mobile already exists
            System.err.println("ERROR: User creation failed: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        } catch (RuntimeException e) { // For OTP generation/email sending failure
            System.err.println("ERROR: Registration completed, but OTP generation/email sending failed: " + e.getMessage());
            e.printStackTrace();
            // IMPORTANT: User is created, so redirect to verification page with instructions
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Registration completed, but failed to send verification email. Please click 'Resend OTP' on the next page to receive your OTP.");
            redirectAttributes.addFlashAttribute("user", user); // Keep user data for potential resend
            return "redirect:/verify-email?username=" + user.getUsername();
        }
    }

    // --- Email Verification Endpoints ---

    @GetMapping("/verify-email")
    public String showEmailVerificationForm(@RequestParam("username") String username, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found for verification.");
            return "redirect:/login";
        }
        User user = userOptional.get();

        if (user.isEmailVerified()) {
            redirectAttributes.addFlashAttribute("successMessage", "Your email is already verified. You can now log in.");
            return "redirect:/login";
        }

        model.addAttribute("username", username);
        model.addAttribute("email", user.getEmail());
        if (model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", model.getAttribute("successMessage"));
        }
        if (model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", model.getAttribute("errorMessage"));
        }
        return "email-verification";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam("username") String username,
                              @RequestParam("otpCode") String otpCode,
                              RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found for verification.");
            return "redirect:/login";
        }
        User user = userOptional.get();

        try {
            if (userService.verifyUserEmail(user, otpCode)) {
                redirectAttributes.addFlashAttribute("successMessage", "Email verified successfully! You can now log in.");
                return "redirect:/login";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired OTP. Please try again.");
                return "redirect:/verify-email?username=" + username;
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/verify-email?username=" + username;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during verification.");
            e.printStackTrace();
            return "redirect:/verify-email?username=" + username;
        }
    }

    @PostMapping("/resend-email-otp")
    public String resendEmailOtp(@RequestParam("username") String username, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found for resending OTP.");
            return "redirect:/login";
        }
        User user = userOptional.get();

        try {
            userService.resendEmailVerificationOtp(user);
            redirectAttributes.addFlashAttribute("successMessage", "New OTP sent to " + user.getEmail() + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error resending OTP: " + e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to send OTP email. Please check application logs.");
            System.err.println("Error resending OTP email: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/verify-email?username=" + username;
    }

    // Removed password reset related methods for now
    // @GetMapping("/forgot-password")
    // @PostMapping("/forgot-password")
    // @GetMapping("/verify-password-reset")
    // @PostMapping("/verify-password-reset")

}
    