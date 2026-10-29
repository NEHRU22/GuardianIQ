
package com.aimonitor.aipersonalassistant.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aimonitor.aipersonalassistant.dto.AuthResponse;
import com.aimonitor.aipersonalassistant.dto.LoginRequest;
import com.aimonitor.aipersonalassistant.dto.RegisterRequest;
import com.aimonitor.aipersonalassistant.entity.User;
import com.aimonitor.aipersonalassistant.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (request.getDateOfBirth() == null
                || !request.getDateOfBirth().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid date of birth."
            );
        }

        int age = Period.between(
                request.getDateOfBirth(),
                LocalDate.now()
        ).getYears();

        if (age >= 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Registration is available only to users under 20"
            );
        }

        if (age < 18 && !request.isGuardianConsentGiven()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guardian consent is required for users under 18"
            );
        }

        String email = normalizeEmail(request.getEmail());
        String parentEmail = normalizeEmail(request.getParentEmail());

        if (email.equals(parentEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use a different email address for your parent or guardian"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setDateOfBirth(request.getDateOfBirth());
        user.setParentEmail(parentEmail);

        // These are submitted choices, not proof of verified consent.
        user.setParentReportsConsent(
                request.isParentReportsConsent()
        );
        user.setGuardianConsentGiven(
                request.isGuardianConsentGiven()
        );

        // Parent email verification will be implemented separately.
        user.setParentEmailVerified(false);

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                "Registration successful",
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        return new AuthResponse(
                "Login successful",
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

    
        @Transactional(readOnly = true)
        public AuthResponse getCurrentUser(String email) {

        User user = userRepository.findByEmailIgnoreCase(
                normalizeEmail(email)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User account not found"
        ));

        return new AuthResponse(
                "Authenticated",
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
        }


    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
