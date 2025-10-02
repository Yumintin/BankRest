package com.example.bankcards.controller;

import com.example.bankcards.service.AuthService;
import com.example.bankcards.util.JwtUtil;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    public static record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public static record RegisterRequest(@NotBlank String username, @NotBlank String password, String fullName) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest r) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(r.username(), r.password())
        );

        UserDetails ud = (UserDetails) auth.getPrincipal();
        var roles = ud.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        String token = jwtUtil.generateToken(ud.getUsername(), roles);
        return ResponseEntity.ok().body(java.util.Map.of("token", token));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> register(@RequestBody RegisterRequest r) {
        String token = authService.registerAndLogin(r.username(), r.password(), r.fullName());
        return ResponseEntity.ok().body(java.util.Map.of("token", token));
    }
}