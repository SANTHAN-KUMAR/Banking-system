package com.santhan.banking_system.controller;

import org.springframework.security.core.Authentication; // NEW import
import org.springframework.security.core.context.SecurityContextHolder; // NEW import
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // NEW import
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String viewDashboard(Model model) {
        // You can access the authenticated user's details here
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        // You can also get roles/authorities if needed for the dashboard display
        // Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        model.addAttribute("username", currentUsername);
        model.addAttribute("message", "Welcome to your Banking Dashboard!");

        // This will resolve to src/main/resources/templates/dashboard.html
        return "dashboard";
    }

    // --- Optional: Admin Dashboard Controller ---
    @GetMapping("/admin/dashboard")
    // @PreAuthorize("hasRole('ADMIN')") // This would enforce the role directly on the method
    public String viewAdminDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Admin! This is the Admin Dashboard.");
        // This will resolve to src/main/resources/templates/admin/dashboard.html
        return "admin/dashboard";
    }

    // --- Optional: Employee Dashboard Controller ---
    @GetMapping("/employee/dashboard")
    // @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String viewEmployeeDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("message", "Welcome, Employee! This is the Employee Dashboard.");
        // This will resolve to src/main/resources/templates/employee/dashboard.html
        return "employee/dashboard";
    }
}