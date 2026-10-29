
package com.aimonitor.aipersonalassistant.controller;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aimonitor.aipersonalassistant.dto.AuthResponse;
import com.aimonitor.aipersonalassistant.dto.LoginRequest;
import com.aimonitor.aipersonalassistant.dto.RegisterRequest;
import com.aimonitor.aipersonalassistant.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail().trim(),
                                    request.getPassword()
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    servletRequest,
                    servletResponse
            );

            return ResponseEntity.ok(authService.login(request));

        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
    }
    
        @GetMapping("/me")
        public ResponseEntity<AuthResponse> getCurrentUser(
                Authentication authentication) {

        AuthResponse response =
                authService.getCurrentUser(authentication.getName());

        return ResponseEntity.ok(response);
        }

        
        
        @PostMapping("/logout")
        public ResponseEntity<Map<String, String>> logout(
                HttpServletRequest request) {

        // Clear the current authentication context.
        SecurityContextHolder.clearContext();

        // Invalidate the current user's session.
        HttpSession session = request.getSession(false);

        if (session != null) {
                session.invalidate();
        }

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully")
        );
        }



}
