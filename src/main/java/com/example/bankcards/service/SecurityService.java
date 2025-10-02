package com.example.bankcards.service;

import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public SecurityService(UserRepository userRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    public boolean isCardOwner(java.util.UUID cardId, Authentication auth) {
        if (auth == null) return false;
        String username = auth.getName();
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        Long userId = userOpt.get().getId();
        return cardRepository.findById(cardId)
                .map(c -> c.getOwner().getId().equals(userId))
                .orElse(false);
    }
}