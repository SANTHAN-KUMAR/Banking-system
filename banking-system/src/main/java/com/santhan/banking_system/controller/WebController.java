package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // Marks this as a Spring MVC Controller that returns views
@RequestMapping("/users") // Base URL path for this controller's views
public class WebController {

    private final UserService userService; // Inject the UserService

    public WebController(UserService userService) {
        this.userService = userService;
    }

    // Handles GET requests to /users
    @GetMapping
    public String showUsersPage(Model model) {
        // Add a new User object to the model for the form
        model.addAttribute("user", new User());
        // Fetch all existing users to display in the table
        model.addAttribute("users", userService.getAllUsers()); // You'll need to create getAllUsers()
        return "user-list"; // This refers to src/main/resources/templates/users.html
    }

    // Handles POST requests to /users/create from the form submission
    @PostMapping("/create")
    public String createUserFromForm(@ModelAttribute("user") User user) {
        // @ModelAttribute binds form data to the User object
        userService.createUser(user); // Call the service to save the user
        return "redirect:/users"; // Redirects back to /users to show updated list
    }
}