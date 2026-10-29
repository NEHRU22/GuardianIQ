package com.aimonitor.aipersonalassistant.entity;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 20, message = "Full name must be between 3 and 20 characters")
    @Column(nullable = false, length = 20)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "Password hash is required")
    @Column(nullable = false)
    private String passwordHash;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Parent or guardian email is required")
    @Email(message = "Enter a valid parent or guardian email")
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String parentEmail;

    @Column(nullable = false)
    private boolean parentEmailVerified = false;

    @Column(nullable = false)
    private boolean parentReportsConsent = false;

    @Column(nullable = false)
    private boolean guardianConsentGiven = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }

    public boolean isParentEmailVerified() {
        return parentEmailVerified;
    }

    public void setParentEmailVerified(boolean parentEmailVerified) {
        this.parentEmailVerified = parentEmailVerified;
    }

    public boolean isParentReportsConsent() {
        return parentReportsConsent;
    }

    public void setParentReportsConsent(boolean parentReportsConsent) {
        this.parentReportsConsent = parentReportsConsent;
    }

    public boolean isGuardianConsentGiven() {
        return guardianConsentGiven;
    }

    public void setGuardianConsentGiven(boolean guardianConsentGiven) {
        this.guardianConsentGiven = guardianConsentGiven;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

