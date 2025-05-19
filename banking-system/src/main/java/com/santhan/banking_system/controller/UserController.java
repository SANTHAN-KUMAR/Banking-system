package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.User; // Import User model
import com.santhan.banking_system.service.UserService; // Import User Service
import org.springframework.http.HttpStatus; // Import for HTTP status codes
import org.springframework.http.ResponseEntity; // Import for building responses
import org.springframework.web.bind.annotation.*; // Import web annotations

@RestController // Marks this class as a REST Controller
@RequestMapping("/api/users") // Base URL path for this controller
public class UserController {

    private final UserService userService; // Declare the Service dependency

    // Constructor Injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- Example Endpoint: Create a new User ---
    @PostMapping // Maps POST requests to /api/users
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // @RequestBody automatically converts the incoming JSON request body into a User object

        User createdUser = userService.createUser(user); // Call the Service layer to perform the logic

        // Return the created user and a 201 (Created) HTTP status code
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // --- Example Endpoint: Get a User by ID ---
    @GetMapping("/{id}") // Maps GET requests to /api/users/{id} (e.g., /api/users/1)
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // @PathVariable extracts the 'id' from the URL path

        User user = userService.getUserById(id); // Call the Service layer

        // Return the user and a 200 (OK) HTTP status code
        return ResponseEntity.ok(user); // ResponseEntity.ok() is a shortcut for status 200
    }

    // You'll add more endpoints here (e.g., GET all users, PUT update user, DELETE user)
}