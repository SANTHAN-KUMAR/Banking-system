package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    private final UserService userService;

    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        System.out.println("DEBUG: GET /login endpoint hit.");
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        System.out.println("DEBUG: GET /register endpoint hit. Preparing registration form.");
        model.addAttribute("user", new User()); // Pass an empty User object to the form
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        System.out.println("DEBUG: POST /register endpoint hit!");
        System.out.println("DEBUG: Received User object from form: ");
        System.out.println("DEBUG:   Username: '" + user.getUsername() + "'");
        System.out.println("DEBUG:   Email:    '" + user.getEmail() + "'");
        System.out.println("DEBUG:   Password: '" + user.getPassword() + "' (should be raw input)");

        try {
            userService.createUser(user); // Attempt to create user
            model.addAttribute("success", "Registration successful! You can now log in.");
            System.out.println("DEBUG: User creation successful. Redirecting to login.");
            return "login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            System.err.println("ERROR: User creation failed: " + e.getMessage());
            return "register";
        }
    }
}