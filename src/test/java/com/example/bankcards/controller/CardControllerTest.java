package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.SecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@EnableMethodSecurity
class CardControllerTest {
    @TestConfiguration
    static class TestMocksConfig {
        @Bean
        public CardService cardService() { return Mockito.mock(CardService.class); }

        @Bean
        public SecurityService securityService() { return Mockito.mock(SecurityService.class); }

        @Bean
        public com.example.bankcards.util.JwtUtil jwtUtil() { return Mockito.mock(com.example.bankcards.util.JwtUtil.class); }

        @Bean
        public com.example.bankcards.security.JwtAuthenticationFilter jwtAuthenticationFilter() {
            return Mockito.mock(com.example.bankcards.security.JwtAuthenticationFilter.class);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private SecurityService securityService;

    @MockitoBean
    private com.example.bankcards.util.JwtUtil jwtUtil;

    @MockitoBean
    private com.example.bankcards.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private CardDto buildCardDto(UUID id) {
        return new CardDto(
                id,
                "4111-****-****-1111",
                123L,
                "12/29",
                "ACTIVE",
                BigDecimal.valueOf(1000.00),
                "1111"
        );
    }

    private CreateCardRequest buildCreateCardRequest() {
        return new CreateCardRequest(
                "4111222233334444",
                123L,
                "Ivan Petrov",
                "12/29",
                BigDecimal.valueOf(100.00)
        );
    }

    private TransferRequest buildTransferRequest() {
        return new TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(10.00)
        );
    }

    @BeforeEach
    void setupFilterMocks() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        try {
            doAnswer(invocation -> {
                HttpServletRequest req = invocation.getArgument(0);
                HttpServletResponse res = invocation.getArgument(1);
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(req, res);
                return null;
            }).when(jwtAuthenticationFilter).doFilterInternal(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
        } catch (Throwable ignore) {
        }
    }

    @Nested
    @DisplayName("GET /api/cards")
    class ListTests {
        @Test
        @DisplayName("должен вернуть страницу с ответом и отобразить поля пагинации")
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturnPageResponse() throws Exception {
            UUID id = UUID.randomUUID();
            CardDto dto = buildCardDto(id);

            var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
            when(cardService.list(anyString(), any(PageRequest.class), any())).thenReturn(page);

            mvc.perform(get("/api/cards")
                            .param("q", "query"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(cardService, times(1)).list(eq("query"), any(PageRequest.class), any());
        }
    }

    @Nested
    @DisplayName("GET /api/cards/{id}")
    class GetTests {
        @Test
        @DisplayName("admin может получить карту")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminCanGet() throws Exception {
            UUID id = UUID.randomUUID();
            CardDto dto = buildCardDto(id);

            when(cardService.get(eq(id))).thenReturn(dto);

            mvc.perform(get("/api/cards/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(cardService, times(1)).get(id);
        }

        @Test
        @DisplayName("user-владелец может получить карту через securityService")
        @WithMockUser(username = "owner", roles = {"USER"})
        void ownerCanGet() throws Exception {
            UUID id = UUID.randomUUID();
            CardDto dto = buildCardDto(id);

            doReturn(true).when(securityService).isCardOwner(any(UUID.class), any(org.springframework.security.core.Authentication.class));
            when(cardService.get(eq(id))).thenReturn(dto);

            mvc.perform(get("/api/cards/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(securityService, atLeastOnce()).isCardOwner(any(UUID.class), any(org.springframework.security.core.Authentication.class));
            verify(cardService, times(1)).get(eq(id));
        }
    }

    @Nested
    @DisplayName("POST /api/cards")
    class CreateTests {
        @Test
        @DisplayName("admin может создать карту -> возвращает 201 с заголовком Location")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminCreates() throws Exception {
            UUID id = UUID.randomUUID();
            CreateCardRequest req = buildCreateCardRequest();
            CardDto created = buildCardDto(id);

            when(cardService.create(any(CreateCardRequest.class))).thenReturn(created);

            mvc.perform(post("/api/cards")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/" + id.toString())))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            ArgumentCaptor<CreateCardRequest> captor = ArgumentCaptor.forClass(CreateCardRequest.class);
            verify(cardService).create(captor.capture());
            assertThat(captor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("ошибка 400, если тело запроса некорректно (валидация)")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void createValidationFail() throws Exception {
            mvc.perform(post("/api/cards")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/cards/{id}")
    class UpdateTests {
        @Test
        @DisplayName("admin может обновить карту")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminUpdates() throws Exception {
            UUID id = UUID.randomUUID();
            CreateCardRequest req = buildCreateCardRequest();
            CardDto updated = buildCardDto(id);

            when(cardService.update(eq(id), any(CreateCardRequest.class))).thenReturn(updated);

            mvc.perform(put("/api/cards/{id}", id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(cardService).update(eq(id), any(CreateCardRequest.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/cards/{id}/block")
    class BlockTests {
        @Test
        @DisplayName("admin может заблокировать карту")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminBlocks() throws Exception {
            UUID id = UUID.randomUUID();
            CardDto blocked = buildCardDto(id);
            when(cardService.block(eq(id))).thenReturn(blocked);

            mvc.perform(patch("/api/cards/{id}/block", id).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(cardService).block(eq(id));
        }
    }

    @Nested
    @DisplayName("POST /api/cards/{id}/request-block")
    class RequestBlockTests {
        @Test
        @DisplayName("user-владелец может запросить блокировку -> возвращает 202 Accepted")
        @WithMockUser(username = "owner", roles = {"USER"})
        void ownerRequestsBlock() throws Exception {
            UUID id = UUID.randomUUID();
            when(securityService.isCardOwner(any(UUID.class), any())).thenReturn(true);

            mvc.perform(post("/api/cards/{id}/request-block", id).with(csrf()))
                    .andExpect(status().isAccepted())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Block request submitted"));

            verify(securityService).isCardOwner(any(UUID.class), any());
        }
    }

    @Test
    @DisplayName("user может перевести средства -> возвращает 200 и вызывает сервис с username")
    @WithMockUser(username = "alice", roles = {"USER"})
    void userTransfers() throws Exception {
        TransferRequest req = buildTransferRequest();

        UUID newId = UUID.randomUUID();
        CardDto transferResult = buildCardDto(newId);

        Mockito.<CardDto>when(
                cardService.transfer(any(TransferRequest.class), eq("alice"))
        ).thenReturn(transferResult);

        mvc.perform(post("/api/cards/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(newId.toString()))
                .andExpect(jsonPath("$.maskedCardNumber").exists());

        verify(cardService).transfer(any(TransferRequest.class), eq("alice"));
    }

    @Nested
    @DisplayName("DELETE /api/cards/{id}")
    class DeleteTests {
        @Test
        @DisplayName("admin может удалить карту -> 204 без содержимого")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminDeletes() throws Exception {
            UUID id = UUID.randomUUID();

            doNothing().when(cardService).delete(eq(id));

            mvc.perform(delete("/api/cards/{id}", id).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(cardService).delete(eq(id));
        }
    }
}