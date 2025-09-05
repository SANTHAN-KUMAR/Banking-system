package com.santhan.banking_system.controller;

import com.santhan.banking_system.dto.KycSubmissionDto;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.model.KycStatus;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/") // Changed from "/user" to "/"
public class DashboardController {

    private final UserService userService;

    @Autowired
    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard") // Now accessible directly at /dashboard
    public String userDashboard(@AuthenticationPrincipal User currentUser, Model model) {
        User user;
        try {
            user = userService.getUserById(currentUser.getId());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Authenticated user with ID " + currentUser.getId() + " not found in database. Logging out. " + e.getMessage());
            return "redirect:/logout";
        }
        model.addAttribute("user", user);

        if (user.getKycStatus() == KycStatus.PENDING || user.getKycStatus() == KycStatus.REQUIRES_RESUBMISSION) {
            model.addAttribute("showKycPrompt", true);
            model.addAttribute("kycStatusMessage", "Your KYC is " + user.getKycStatus().getDisplayName() + ". Please complete or resubmit your details to fully activate your account.");
        } else if (user.getKycStatus() == KycStatus.VERIFIED) {
            model.addAttribute("kycStatusMessage", "Your KYC is Verified. Your account is fully active.");
        } else if (user.getKycStatus() == KycStatus.REJECTED) {
            model.addAttribute("showKycPrompt", true);
            model.addAttribute("kycStatusMessage", "Your KYC has been Rejected. Please contact support.");
        }

        return "dashboard";
    }

    @GetMapping("/kyc") // Now accessible directly at /kyc
    public String showKycSubmissionForm(@AuthenticationPrincipal User currentUser, Model model) {
        User user;
        try {
            user = userService.getUserById(currentUser.getId());
        } catch (IllegalArgumentException e) {
            throw new EntityNotFoundException("Authenticated user not found for KYC submission: " + e.getMessage());
        }

        KycSubmissionDto kycDto;
        if (user.getFirstName() != null || user.getNationalIdNumber() != null) {
            kycDto = new KycSubmissionDto(
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDateOfBirth(),
                    user.getAddress(),
                    user.getNationalIdNumber(),
                    user.getDocumentType()
            );
        } else {
            kycDto = new KycSubmissionDto();
        }

        model.addAttribute("kycSubmissionDto", kycDto);
        return "kyc-submission";
    }

    @PostMapping("/kyc/submit") // Now accessible directly at /kyc/submit
    public String submitKycDetails(@AuthenticationPrincipal User currentUser,
                                   @Valid @ModelAttribute("kycSubmissionDto") KycSubmissionDto kycDto,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {

        if (result.hasErrors()) {
            model.addAttribute("error", "Please correct the errors in the form.");
            return "kyc-submission";
        }

        try {
            userService.submitKycDetails(currentUser.getId(), kycDto);
            redirectAttributes.addFlashAttribute("successMessage", "Your KYC details have been submitted successfully for review!");
            return "redirect:/dashboard"; // Changed from "/user/dashboard" to "/dashboard"
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Authenticated user not found. Please log in again.");
            return "redirect:/logout";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "kyc-submission";
        }
    }
}
