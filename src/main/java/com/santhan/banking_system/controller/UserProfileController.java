package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.KycStatus;
import com.santhan.banking_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime; // Import LocalDateTime
import java.time.temporal.ChronoUnit; // Import ChronoUnit
import java.util.Optional;

@Controller
@RequestMapping("/profile")
@PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class UserProfileController {

    private final UserService userService;

    // Define the cooldown period for profile updates (e.g., 24 hours)
    private static final long PROFILE_UPDATE_COOLDOWN_HOURS = 24;

    @Autowired
    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the current user's profile.
     */
    @GetMapping
    public String viewProfile(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User profile not found.");
            return "redirect:/dashboard";
        }

        User user = userOptional.get();
        model.addAttribute("user", user);
        model.addAttribute("kycStatuses", KycStatus.values());

        if (user.getDateOfBirth() != null) {
            model.addAttribute("dateOfBirthFormatted", user.getDateOfBirth().toString());
        } else {
            model.addAttribute("dateOfBirthFormatted", "");
        }

        // Add cooldown information to the model for display
        if (user.getLastProfileUpdate() != null) {
            LocalDateTime nextAllowedUpdate = user.getLastProfileUpdate().plusHours(PROFILE_UPDATE_COOLDOWN_HOURS);
            if (LocalDateTime.now().isBefore(nextAllowedUpdate)) {
                model.addAttribute("updateCooldownActive", true);
                model.addAttribute("nextAllowedUpdate", nextAllowedUpdate);
            }
        } else {
            model.addAttribute("updateCooldownActive", false); // No previous update, so no cooldown
        }

        return "profile/view-profile";
    }

    /**
     * Handles updating the user's profile.
     */
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User userDetails, Authentication authentication, RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User profile not found for update.");
            return "redirect:/dashboard";
        }

        User existingUser = userOptional.get();
        Long userId = existingUser.getId();

        // --- Rate Limiting Check ---
        if (existingUser.getLastProfileUpdate() != null) {
            LocalDateTime nextAllowedUpdate = existingUser.getLastProfileUpdate().plusHours(PROFILE_UPDATE_COOLDOWN_HOURS);
            if (LocalDateTime.now().isBefore(nextAllowedUpdate)) {
                long minutesRemaining = ChronoUnit.MINUTES.between(LocalDateTime.now(), nextAllowedUpdate);
                long hoursRemaining = minutesRemaining / 60;
                minutesRemaining = minutesRemaining % 60;
                redirectAttributes.addFlashAttribute("error",
                        "Profile can only be updated once every " + PROFILE_UPDATE_COOLDOWN_HOURS + " hours. " +
                                "Please try again in " + hoursRemaining + " hours and " + minutesRemaining + " minutes.");
                return "redirect:/profile";
            }
        }
        // --- End Rate Limiting Check ---

        try {
            userDetails.setId(userId); // Ensure the ID matches the authenticated user

            userService.updateUserProfile(userId, userDetails);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/profile";
    }
}
