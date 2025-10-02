package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.PageResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.SecurityService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;
    private final SecurityService securityService;

    public CardController(CardService cardService, SecurityService securityService) {
        this.cardService = cardService;
        this.securityService = securityService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  Authentication auth) {
        Page<CardDto> p = cardService.list(q, PageRequest.of(page, size), auth);

        PageResponse<CardDto> resp = new PageResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        );

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCardOwner(#id, authentication)")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        CardDto d = cardService.get(id);
        return ResponseEntity.ok(d);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateCardRequest req) {
        CardDto d = cardService.create(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(d.getId())
                .toUri();

        return ResponseEntity.created(location).body(d);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody CreateCardRequest req) {
        CardDto updated = cardService.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> block(@PathVariable UUID id) {
        var d = cardService.block(id);
        return ResponseEntity.ok(d);
    }

    @PostMapping("/{id}/request-block")
    @PreAuthorize("hasRole('USER') and @securityService.isCardOwner(#id, authentication)")
    public ResponseEntity<?> requestBlock(@PathVariable UUID id, Authentication auth) {

        // В реальном проекте: создать запись в заявках; здесь — просто возвращаем accepted
        return ResponseEntity.accepted().body(java.util.Map.of("message", "Block request submitted"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest req, Authentication auth) {
        String username = auth.getName();
        var res = cardService.transfer(req, username);
        return ResponseEntity.ok(res);
    }
}