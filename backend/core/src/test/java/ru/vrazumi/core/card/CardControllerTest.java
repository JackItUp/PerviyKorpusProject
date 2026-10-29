package ru.vrazumi.core.card;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.DefaultTestUserConfig;
import ru.vrazumi.core.WebLayerTest;
import ru.vrazumi.core.card.dto.CardResponse;
import ru.vrazumi.core.deck.DeckNotFoundException;

@WebLayerTest(CardController.class)
class CardControllerTest {

  private static final long USER = DefaultTestUserConfig.USER_ID;

  private static final Instant NOW = Instant.parse("2026-10-01T09:35:00Z");

  @Autowired private MockMvc mvc;
  @MockitoBean private CardService cardService;

  @Test
  void createReturns201WithLocation() throws Exception {
    when(cardService.create(eq(1L), any(), eq(USER))).thenReturn(card(10L, "go"));

    postCard(1, "{\"question\": \" go \", \"answer\": \"went, gone\"}")
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/cards/10")))
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.deckId").value(1))
        .andExpect(jsonPath("$.type").value("NORMAL"))
        .andExpect(jsonPath("$.question").value("go"));
  }

  @Test
  void createAcceptsTextOf2000Chars() throws Exception {
    when(cardService.create(eq(1L), any(), eq(USER))).thenReturn(card(10L, "x".repeat(2000)));

    postCard(1, cardJson("x".repeat(2000), "y".repeat(2000))).andExpect(status().isCreated());
  }

  @Test
  void createRejectsBlankQuestion() throws Exception {
    expectFieldError(postCard(1, cardJson("   ", "went")), "question");
  }

  @Test
  void createRejectsMissingAnswer() throws Exception {
    expectFieldError(postCard(1, "{\"question\": \"go\"}"), "answer");
  }

  @Test
  void createRejectsQuestionOf2001Chars() throws Exception {
    expectFieldError(postCard(1, cardJson("x".repeat(2001), "went")), "question");
  }

  @Test
  void createRejectsAnswerOf2001Chars() throws Exception {
    expectFieldError(postCard(1, cardJson("go", "y".repeat(2001))), "answer");
  }

  @Test
  void createReportsBothFieldsWhenBothInvalid() throws Exception {
    postCard(1, cardJson("", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2));
  }

  @Test
  void createRejectsMalformedJson() throws Exception {
    postCard(1, "{question: go}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    verifyNoInteractions(cardService);
  }

  @Test
  void createReturns404ForMissingDeck() throws Exception {
    when(cardService.create(eq(99L), any(), eq(USER))).thenThrow(new DeckNotFoundException(99L));

    postCard(99, cardJson("go", "went"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Колода с id=99 не найдена"));
  }

  @Test
  void createRejectsNonNumericDeckId() throws Exception {
    mvc.perform(
            post("/api/decks/abc/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cardJson("go", "went")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(cardService);
  }

  @Test
  void findByDeckReturnsCards() throws Exception {
    when(cardService.findByDeck(1L, USER)).thenReturn(List.of(card(10L, "go"), card(11L, "be")));

    mvc.perform(get("/api/decks/1/cards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[1].id").value(11));
  }

  @Test
  void findByDeckReturnsEmptyListForEmptyDeck() throws Exception {
    when(cardService.findByDeck(1L, USER)).thenReturn(List.of());

    mvc.perform(get("/api/decks/1/cards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void findByDeckReturns404ForMissingDeck() throws Exception {
    when(cardService.findByDeck(99L, USER)).thenThrow(new DeckNotFoundException(99L));

    mvc.perform(get("/api/decks/99/cards"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void findByDeckRejectsNonNumericDeckId() throws Exception {
    mvc.perform(get("/api/decks/abc/cards")).andExpect(status().isBadRequest());
  }

  private ResultActions postCard(long deckId, String json) throws Exception {
    return mvc.perform(
        post("/api/decks/{deckId}/cards", deckId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private void expectFieldError(ResultActions result, String field) throws Exception {
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field));
    verifyNoInteractions(cardService);
  }

  private static String cardJson(String question, String answer) {
    return "{\"question\": \"%s\", \"answer\": \"%s\"}".formatted(question, answer);
  }

  private static CardResponse card(Long id, String question) {
    return new CardResponse(id, 1L, CardType.NORMAL, question, "ответ", NOW, NOW);
  }
}
