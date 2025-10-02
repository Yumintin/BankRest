package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.NotCardOwnerException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CipherUtil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CipherUtil cipherUtil;
    private final UserRepository userRepository;

    public CardService(CardRepository cardRepository,
                       CipherUtil cipherUtil,
                       UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.cipherUtil = cipherUtil;
        this.userRepository = userRepository;
    }

    public Page<CardDto> list(String q, Pageable pageable, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            Page<Card> page = cardRepository.search(q, pageable);
            return page.map(this::toDto);
        } else {
            Long userId = resolveCurrentUserId(auth);
            Page<Card> page = cardRepository.searchForUser(userId, q, pageable);
            return page.map(this::toDto);
        }
    }

    public CardDto get(UUID id) {
        Card c = cardRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Card not found"));
        return toDto(c);
    }

    @Transactional
    public CardDto create(CreateCardRequest req) {
        Card c = new Card();
        c.setEncryptedCardNumber(cipherUtil.encrypt(req.getCardNumber()));
        c.setExpiryDate(req.getExpiryDate());
        c.setBalance(Optional.ofNullable(req.getInitialBalance()).orElse(BigDecimal.ZERO));
        c.setStatus(CardStatus.ACTIVE);

        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + req.getOwnerId() + " не найден"));
        c.setOwner(owner);

        String digits = req.getCardNumber().replaceAll("\\D", "");
        if (digits.length() >= 4) {
            c.setLast4(digits.substring(digits.length() - 4));
        }

        cardRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CardDto update(UUID id, CreateCardRequest req) {
        Card c = cardRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Card not found"));
        if (req.getCardNumber() != null && !req.getCardNumber().isBlank()) {
            c.setEncryptedCardNumber(cipherUtil.encrypt(req.getCardNumber()));
            String digits = req.getCardNumber().replaceAll("\\D", "");
            c.setLast4(digits.length() >= 4 ? digits.substring(digits.length() - 4) : null);
        }
        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + req.getOwnerId() + " не найден"));
        c.setOwner(owner);
        if (req.getOwnerId() != null) c.setOwner(owner);
        if (req.getExpiryDate() != null) c.setExpiryDate(req.getExpiryDate());
        if (req.getInitialBalance() != null) c.setBalance(req.getInitialBalance());
        c.setUpdatedAt(java.time.LocalDateTime.now());
        cardRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public void delete(UUID id) {
        cardRepository.deleteById(id);
    }

    @Transactional
    public CardDto block(UUID id) {
        Card c = cardRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Card not found"));
        c.setStatus(CardStatus.BLOCKED);
        c.setUpdatedAt(java.time.LocalDateTime.now());
        cardRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CardDto unblock(UUID id) {
        Card c = cardRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Card not found"));
        c.setStatus(CardStatus.ACTIVE);
        c.setUpdatedAt(java.time.LocalDateTime.now());
        cardRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CardDto transfer(TransferRequest req, String requesterUsername) {
        if (req.getFromCardId().equals(req.getToCardId())) {
            throw new IllegalArgumentException("from and to cards are the same");
        }

        var user = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new SecurityException("Requester user not found"));
        Long requesterId = user.getId();

        Card from = cardRepository.findByIdForUpdate(req.getFromCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found"));
        Card to = cardRepository.findByIdForUpdate(req.getToCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found"));

        if (!Objects.equals(from.getOwner().getId(), requesterId) || !Objects.equals(to.getOwner().getId(), requesterId)) {
            throw new NotCardOwnerException("Both cards must belong to the authenticated user");
        }

        if (!from.getStatus().equals(CardStatus.ACTIVE)) {
            throw new CardNotActiveException("Source card is not ACTIVE");
        }
        if (to.getStatus() != CardStatus.ACTIVE) {
            throw new CardNotActiveException("Destination card is not ACTIVE");
        }

        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));
        from.setUpdatedAt(java.time.LocalDateTime.now());
        to.setUpdatedAt(java.time.LocalDateTime.now());

        cardRepository.save(from);
        cardRepository.save(to);
        return toDto(from);
    }

    private CardDto toDto(Card c) {
        CardDto d = new CardDto();
        d.setId(c.getId());
        try {
            String plain = cipherUtil.decrypt(c.getEncryptedCardNumber());
            d.setMaskedCardNumber(cipherUtil.mask(plain));
        } catch (Exception ex) {
            d.setMaskedCardNumber("**** **** **** ****");
        }
        d.setOwnerId(c.getOwner() != null ? c.getOwner().getId() : null);
        d.setExpiryDate(c.getExpiryDate());
        d.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        d.setBalance(c.getBalance());
        d.setLast4(c.getLast4());
        return d;
    }

    private Long resolveCurrentUserId(Authentication auth) {
        if (auth == null) throw new SecurityException("No authentication");
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new SecurityException("Current user not found"));
    }
}