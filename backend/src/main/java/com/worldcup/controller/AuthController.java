package com.worldcup.controller;

import com.worldcup.dto.AuthResponse;
import com.worldcup.dto.LoginRequest;
import com.worldcup.dto.RegisterRequest;
import com.worldcup.entity.User;
import com.worldcup.security.CustomUserDetailsService;
import com.worldcup.security.JwtTokenProvider;
import com.worldcup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Create new user
        User user = userService.createUser(request.getEmail(), request.getPassword());

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = tokenProvider.generateToken(userDetails);

        AuthResponse response = new AuthResponse(token, "Bearer", user.getId(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Authenticate user credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        // Generate token for authenticated user
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = tokenProvider.generateToken(userDetails);

        User user = userService.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        AuthResponse response = new AuthResponse(token, "Bearer", user.getId(), user.getEmail());
        return ResponseEntity.ok(response);
    }
}

