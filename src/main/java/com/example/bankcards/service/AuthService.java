package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.repository.UserRepository;

import com.example.bankcards.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public String registerAndLogin(String username, String password, String fullname) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("User exists: " + username);
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setFullName(fullname);
        Role r = new Role("ROLE_USER");
        u.getRoles().add(r);
        userRepository.save(u);
        return jwtUtil.generateToken(u.getUsername(), u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}