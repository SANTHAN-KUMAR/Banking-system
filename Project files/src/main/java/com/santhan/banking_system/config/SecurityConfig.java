package com.santhan.banking_system.config;

import com.santhan.banking_system.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;

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
        auth.setUserDetailsService(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity in development, re-enable for production
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible paths
                        .requestMatchers("/register", "/login", "/").permitAll()

                        // Admin specific paths
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Employee specific paths
                        .requestMatchers("/employee/**").hasAnyRole("ADMIN", "EMPLOYEE")

                        // User/Customer specific paths handled by DashboardController at root level
                        .requestMatchers("/dashboard", "/kyc", "/kyc/submit").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")

                        // General authenticated paths that might not be under /user (adjust as needed)
                        .requestMatchers("/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers("/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")


                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true) // Reverted to /dashboard
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
