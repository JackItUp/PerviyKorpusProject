package ru.vrazumi.core.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.deck.dto.CreateDeckRequest;
import ru.vrazumi.core.deck.dto.DeckResponse;

@WebMvcTest(DeckController.class)
class DeckControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-01T09:30:00Z");

  @Autowired private MockMvc mvc;
  @MockitoBean private DeckService deckService;

  @Test
  void createReturns201WithLocation() throws Exception {
    when(deckService.create(any())).thenReturn(deck(1L, "Английские глаголы", 0));

    postDeck(
            """
            {"title": "  Английские глаголы  ", "description": "Неправильные глаголы"}
            """)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/decks/1")))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Английские глаголы"))
        .andExpect(jsonPath("$.cardCount").value(0))
        .andExpect(jsonPath("$.createdAt").value("2026-10-01T09:30:00Z"));

    ArgumentCaptor<CreateDeckRequest> request = ArgumentCaptor.forClass(CreateDeckRequest.class);
    verify(deckService).create(request.capture());
    assertThat(request.getValue().title()).isEqualTo("Английские глаголы");
  }

  @Test
  void createAcceptsTitleOf100CharsSurroundedBySpaces() throws Exception {
    when(deckService.create(any())).thenReturn(deck(1L, "x".repeat(100), 0));

    postDeck("{\"title\": \"   " + "x".repeat(100) + "   \"}").andExpect(status().isCreated());
  }

  @Test
  void createRejectsMissingTitle() throws Exception {
    expectFieldError(postDeck("{\"description\": \"Без названия\"}"), "title");
  }

  @Test
  void createRejectsEmptyTitle() throws Exception {
    expectFieldError(postDeck("{\"title\": \"\"}"), "title");
  }

  @Test
  void createRejectsWhitespaceTitle() throws Exception {
    expectFieldError(postDeck("{\"title\": \"   \\t  \"}"), "title");
  }

  @Test
  void createRejectsTitleOf101Chars() throws Exception {
    expectFieldError(postDeck("{\"title\": \"" + "x".repeat(101) + "\"}"), "title")
        .andExpect(jsonPath("$.errors[0].message").value(endsWith("100 символов")));
  }

  @Test
  void createRejectsDescriptionOf1001Chars() throws Exception {
    expectFieldError(
        postDeck("{\"title\": \"Колода\", \"description\": \"" + "x".repeat(1001) + "\"}"),
        "description");
  }

  @Test
  void createRejectsMalformedJson() throws Exception {
    postDeck("{\"title\": ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Некорректный запрос"));
    verifyNoInteractions(deckService);
  }

  @Test
  void findAllReturnsEmptyList() throws Exception {
    when(deckService.findAll()).thenReturn(List.of());

    mvc.perform(get("/api/decks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void findAllReturnsDecks() throws Exception {
    when(deckService.findAll()).thenReturn(List.of(deck(2L, "Вторая", 3), deck(1L, "Первая", 0)));

    mvc.perform(get("/api/decks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(2))
        .andExpect(jsonPath("$[0].cardCount").value(3));
  }

  @Test
  void findByIdReturnsDeck() throws Exception {
    when(deckService.findById(1L)).thenReturn(deck(1L, "Колода", 5));

    mvc.perform(get("/api/decks/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Колода"))
        .andExpect(jsonPath("$.cardCount").value(5));
  }

  @Test
  void findByIdReturns404ForMissingDeck() throws Exception {
    when(deckService.findById(99L)).thenThrow(new DeckNotFoundException(99L));

    mvc.perform(get("/api/decks/99"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Колода с id=99 не найдена"))
        .andExpect(jsonPath("$.instance").value("/api/decks/99"));
  }

  @Test
  void findByIdRejectsNonNumericId() throws Exception {
    mvc.perform(get("/api/decks/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(endsWith("'abc'.")));
    verifyNoInteractions(deckService);
  }

  @Test
  void unexpectedErrorReturns500WithoutStackTrace() throws Exception {
    when(deckService.findAll()).thenThrow(new IllegalStateException("секретная деталь"));

    mvc.perform(get("/api/decks"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("Внутренняя ошибка сервера. Попробуйте позже."))
        .andExpect(jsonPath("$.trace").doesNotExist());
  }

  private ResultActions postDeck(String json) throws Exception {
    return mvc.perform(post("/api/decks").contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private ResultActions expectFieldError(ResultActions result, String field) throws Exception {
    verifyNoInteractions(deckService);
    return result
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
  }

  private static DeckResponse deck(Long id, String title, long cardCount) {
    return new DeckResponse(id, title, null, cardCount, NOW, NOW);
  }
}
