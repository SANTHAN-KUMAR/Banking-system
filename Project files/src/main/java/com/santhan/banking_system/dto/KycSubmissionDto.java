package com.santhan.banking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// This DTO will be used to capture KYC submission data from the user's form.
// It includes validation annotations to ensure data quality.
public class KycSubmissionDto {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past") // Ensures DOB is not in the future
    @DateTimeFormat(pattern = "yyyy-MM-dd") // Specifies the expected date format from the form
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @NotBlank(message = "National ID number is required")
    @Size(min = 5, max = 50, message = "National ID number must be between 5 and 50 characters")
    private String nationalIdNumber; // e.g., Aadhaar, SSN, Passport Number

    @NotBlank(message = "Document type is required")
    private String documentType;     // e.g., "Aadhaar Card", "Passport", "Driver's License"

    // --- Constructors ---
    public KycSubmissionDto() {
    }

    public KycSubmissionDto(String firstName, String lastName, LocalDate dateOfBirth, String address, String nationalIdNumber, String documentType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.nationalIdNumber = nationalIdNumber;
        this.documentType = documentType;
    }

    // --- Getters and Setters ---
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNationalIdNumber() {
        return nationalIdNumber;
    }

    public void setNationalIdNumber(String nationalIdNumber) {
        this.nationalIdNumber = nationalIdNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    @Override
    public String toString() {
        return "KycSubmissionDto{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", address='" + address + '\'' +
                ", nationalIdNumber='" + nationalIdNumber + '\'' +
                ", documentType='" + documentType + '\'' +
                '}';
    }
}
