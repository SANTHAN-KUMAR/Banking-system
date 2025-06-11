// Create this file in: src/test/java/com/santhan/banking_system/config/SecurityConfigTest.java

package com.santhan.banking_system.config;
import org.springframework.context.annotation.Import;
import com.santhan.banking_system.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// No longer need: import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean; // New import for @Bean
import org.springframework.context.annotation.Configuration; // New import for @Configuration
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.mockito.Mockito.mock; // New import for mock()

@WebMvcTest
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // --- NEW WAY TO MOCK UserService ---
    // This inner class tells Spring how to create the 'fake' UserService for this test.
    @Configuration // This marks it as a configuration class
    static class TestConfig {
        @Bean // This method will provide the UserService object
        public UserService userService() {
            return mock(UserService.class); // This creates a fake UserService object
        }
    }
    // Note: You don't need to @Autowired UserService directly in SecurityConfigTest class anymore.
    // Spring will automatically use the mock created by TestConfig for SecurityConfig.

    // --- Test Publicly Accessible Paths ---

    @Test
    void publicPage_AllowsAccess_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPage_AllowsAccess_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPage_AllowsAccess_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // --- Test Protected Paths (Requires Authentication) ---

    @Test
    void protectedPage_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- Test Role-Based Access ---

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerAccess_Allowed_ForAccountsList() throws Exception {
        mockMvc.perform(get("/accounts/list"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerAccess_Denied_ForAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employee", roles = {"EMPLOYEE"})
    void employeeAccess_Allowed_ForEmployeeDashboard() throws Exception {
        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "employee", roles = {"EMPLOYEE"})
    void employeeAccess_Denied_ForAdminSpecificPage() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAccess_Allowed_ForAllAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAccess_Allowed_ForEmployeePages() throws Exception {
        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isOk());
    }
}