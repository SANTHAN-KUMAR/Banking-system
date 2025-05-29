package com.santhan.banking_system.model;

public enum UserRole {
    ROLE_CUSTOMER,  // Standard user, can manage their own accounts
    ROLE_EMPLOYEE,  // Can view customer accounts (for support), perform specific employee tasks
    ROLE_ADMIN      // Full control over the system, can manage users, roles, etc.
}