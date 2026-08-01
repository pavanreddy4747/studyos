package com.studyos.app.controller;

import com.studyos.app.dto.LoginRequest;
import com.studyos.app.dto.SignupRequest;
import com.studyos.app.model.User;
import com.studyos.app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "An account with this email already exists"));
        }

        String displayName = (request.getDisplayName() == null || request.getDisplayName().isBlank())
                ? request.getEmail().split("@")[0] : request.getDisplayName();

        User user = new User(request.getEmail(), passwordEncoder.encode(request.getPassword()), displayName);
        userRepository.save(user);

        authenticateAndCreateSession(request.getEmail(), request.getPassword(), httpRequest, httpResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        try {
            authenticateAndCreateSession(request.getEmail(), request.getPassword(), httpRequest, httpResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "currentStreak", user.getCurrentStreak()
        ));
    }

    private void authenticateAndCreateSession(String email, String password,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        Authentication authRequest = UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        Authentication authResult = authenticationManager.authenticate(authRequest);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
