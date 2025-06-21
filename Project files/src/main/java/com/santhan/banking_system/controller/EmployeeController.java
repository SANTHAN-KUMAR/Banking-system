package com.santhan.banking_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    @GetMapping("/dashboard")
    public String viewEmployeeDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Employee! This is the Employee Dashboard.");
        // Add any employee-specific data to the model here
        return "employee/dashboard"; // Assuming employee/dashboard.html
    }

    // Add other employee-specific mappings here
}
