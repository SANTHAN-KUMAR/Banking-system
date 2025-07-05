package com.santhan.banking_system.config;

import com.santhan.banking_system.service.UserService; // Import your UserService
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // NEW import
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // For logout

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // NEW: Enables @PreAuthorize and other method-level security annotations
public class SecurityConfig {

    private final UserService userService; // Inject your UserService

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userService); // Set your custom UserDetailsService
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CSRF protection for production security
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/h2-console/**") // Ignore for H2 console in dev/test
                )
                // Security headers
                .headers(headers -> headers
                        .frameOptions().sameOrigin() // Allow frames from same origin (for H2 console)
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)
                        )
                )
                // Session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible paths
                        .requestMatchers("/register", "/login", "/", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll() // H2 console for development

                        // Admin specific paths
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Only ADMIN can access /admin and sub-paths

                        // Employee specific paths
                        .requestMatchers("/employee/**").hasAnyRole("ADMIN", "EMPLOYEE") // ADMIN and EMPLOYEE can access /employee

                        // Customer specific paths (e.g., viewing their own accounts)
                        // Note: Actual data filtering (e.g., only user's own accounts) still needs to be in controller/service
                        .requestMatchers("/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers("/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")         // Specify custom login page
                        .loginProcessingUrl("/login") // URL to submit the username and password
                        .defaultSuccessUrl("/dashboard", true) // Redirect to /dashboard after successful login
                        .failureUrl("/login?error=true") // Redirect back to login page on failure
                        .permitAll()                 // Allow everyone to access login page
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // URL to trigger logout
                        .logoutSuccessUrl("/login?logout") // Redirect to login page after logout
                        .invalidateHttpSession(true) // Invalidate HTTP session
                        .deleteCookies("JSESSIONID") // Delete session cookies
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}