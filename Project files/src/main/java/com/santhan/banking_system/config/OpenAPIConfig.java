package com.santhan.banking_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Banking System API documentation.
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI bankingSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking System API")
                        .description("A comprehensive banking system with user management, account operations, " +
                                   "transactions, and fraud detection capabilities.")
                        .version("v3.1")
                        .contact(new Contact()
                                .name("Santhan Kumar")
                                .email("santhan@example.com")
                                .url("https://github.com/SANTHAN-KUMAR/Banking-system"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("https://banking-system-prod.example.com")
                                .description("Production server")))
                .addSecurityItem(new SecurityRequirement().addList("sessionAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("sessionAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Session-based authentication using Spring Security")));
    }
}