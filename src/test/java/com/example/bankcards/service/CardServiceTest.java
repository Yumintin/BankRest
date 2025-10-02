package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.NotCardOwnerException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CipherUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для CardService")
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CipherUtil cipherUtil;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private User testOtherUser;
    private Card fromCard;
    private Card toCard;
    private final UUID fromCardId = UUID.randomUUID();
    private final UUID toCardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testOtherUser = new User();
        testOtherUser.setId(2L);

        fromCard = new Card();
        fromCard.setId(fromCardId);
        fromCard.setOwner(testUser);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setStatus(CardStatus.ACTIVE);

        toCard = new Card();
        toCard.setId(toCardId);
        toCard.setOwner(testUser);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(CardStatus.ACTIVE);
    }

    @Nested
    @DisplayName("Метод transfer()")
    class TransferTests {

        @Test
        @DisplayName("успешно переводит средства при корректных данных")
        void shouldTransferSuccessfully() {
            TransferRequest request = new TransferRequest(fromCardId, toCardId, new BigDecimal("100.00"));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

            cardService.transfer(request, "testuser");

            assertEquals(new BigDecimal("900.00"), fromCard.getBalance());
            assertEquals(new BigDecimal("600.00"), toCard.getBalance());
            verify(cardRepository, times(2)).save(any(Card.class));
        }

        @Test
        @DisplayName("выбрасывает InsufficientFundsException при нехватке средств")
        void shouldThrowExceptionForInsufficientFunds() {
            TransferRequest request = new TransferRequest(fromCardId, toCardId, new BigDecimal("2000.00")); // Сумма больше баланса
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

            assertThrows(InsufficientFundsException.class, () -> cardService.transfer(request, "testuser"));

            assertEquals(new BigDecimal("1000.00"), fromCard.getBalance());
            verify(cardRepository, never()).save(any(Card.class));
        }

        @Test
        @DisplayName("выбрасывает ResourceNotFoundException, если карта списания не найдена")
        void shouldThrowExceptionWhenFromCardNotFound() {
            TransferRequest request = new TransferRequest(fromCardId, toCardId, new BigDecimal("100.00"));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> cardService.transfer(request, "testuser"));
        }

        @Test
        @DisplayName("выбрасывает NotCardOwnerException, если пользователь не владелец карты")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            toCard.setOwner(testOtherUser);
            TransferRequest request = new TransferRequest(fromCardId, toCardId, new BigDecimal("100.00"));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

            assertThrows(NotCardOwnerException.class, () -> cardService.transfer(request, "testuser"));
        }
    }
}