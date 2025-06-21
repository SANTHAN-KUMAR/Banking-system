package com.santhan.banking_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // This is the generic dashboard after login
        model.addAttribute("message", "Welcome to your Dashboard!");
        return "dashboard"; // Assuming dashboard.html
    }

    @GetMapping("/")
    public String redirectToDashboard() {
        // This redirects the root URL to the dashboard after login
        return "redirect:/dashboard";
    }

    // --- REMOVED: Duplicate /admin/dashboard mapping ---
    // The method 'showAdminDashboard' was here,
    // but it conflicted with AdminController.viewAdminDashboard.
    // It has been removed to resolve the "Ambiguous mapping" error.
    // The /admin/dashboard mapping should ONLY reside in AdminController.
}
