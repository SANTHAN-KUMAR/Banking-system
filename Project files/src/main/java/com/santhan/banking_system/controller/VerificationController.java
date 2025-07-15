package com.santhan.banking_system.controller;

import com.santhan.banking_system.dto.ApiResponse;
import com.santhan.banking_system.dto.VerificationRequest;
import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.Map; // NEW: Import Map

@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private final UserService userService;

    @Autowired
    public VerificationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerificationRequest request) {
        try {
            Optional<User> userOptional = userService.findByUsername(request.getUserIdentifier());
            if (userOptional.isEmpty()) {
                userOptional = userService.findByEmail(request.getUserIdentifier()); // Corrected method call
            }

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "User not found."));
            }

            User user = userOptional.get();
            Otp.OtpPurpose purpose = Otp.OtpPurpose.valueOf(request.getPurpose().toUpperCase());

            if (purpose != Otp.OtpPurpose.EMAIL_VERIFICATION) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "Invalid purpose for email verification."));
            }

            if (userService.verifyUserEmail(user, request.getOtpCode())) {
                return ResponseEntity.ok(new ApiResponse(true, "Email verified successfully!"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, "Invalid or expired OTP for email verification."));
            }
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Email verification error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An error occurred during email verification."));
        }
    }

    @PostMapping("/mobile")
    public ResponseEntity<?> verifyMobile(@Valid @RequestBody VerificationRequest request) {
        try {
            Optional<User> userOptional = userService.findByUsername(request.getUserIdentifier());
            if (userOptional.isEmpty()) {
                userOptional = userService.findByEmail(request.getUserIdentifier()); // Corrected method call
            }
            if (userOptional.isEmpty()) {
                userOptional = userService.findByMobileNumber(request.getUserIdentifier()); // Corrected method call
            }

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "User not found."));
            }

            User user = userOptional.get();
            Otp.OtpPurpose purpose = Otp.OtpPurpose.valueOf(request.getPurpose().toUpperCase());

            if (purpose != Otp.OtpPurpose.MOBILE_VERIFICATION) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "Invalid purpose for mobile verification."));
            }

            if (userService.verifyUserMobile(user, request.getOtpCode())) {
                return ResponseEntity.ok(new ApiResponse(true, "Mobile number verified successfully!"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, "Invalid or expired OTP for mobile verification."));
            }
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Mobile verification error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An error occurred during mobile verification."));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String userIdentifier = request.get("userIdentifier");
        String purposeString = request.get("purpose");

        if (userIdentifier == null || userIdentifier.isEmpty() || purposeString == null || purposeString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "User identifier and purpose are required."));
        }

        try {
            Optional<User> userOptional = userService.findByUsername(userIdentifier);
            if (userOptional.isEmpty()) {
                userOptional = userService.findByEmail(userIdentifier); // Corrected method call
            }
            if (userOptional.isEmpty()) {
                userOptional = userService.findByMobileNumber(userIdentifier); // Corrected method call
            }

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "User not found."));
            }

            User user = userOptional.get();
            Otp.OtpPurpose purpose = Otp.OtpPurpose.valueOf(purposeString.toUpperCase());

            if (purpose == Otp.OtpPurpose.EMAIL_VERIFICATION) {
                if (user.isEmailVerified()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse(false, "Email is already verified."));
                }
                userService.resendEmailVerificationOtp(user);
                return ResponseEntity.ok(new ApiResponse(true, "Email verification OTP resent successfully."));
            } else if (purpose == Otp.OtpPurpose.MOBILE_VERIFICATION) {
                if (user.isMobileVerified()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse(false, "Mobile number is already verified."));
                }
                userService.resendMobileVerificationOtp(user);
                return ResponseEntity.ok(new ApiResponse(true, "Mobile verification OTP resent successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, "Unsupported OTP purpose for resending."));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse(false, e.getMessage()));
        }
        catch (Exception e) {
            System.err.println("Resend OTP error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An error occurred while resending OTP."));
        }
    }
}
