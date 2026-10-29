package ru.vrazumi.core;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vrazumi.core.card.CardController;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.deck.DeckController;
import ru.vrazumi.core.deck.DeckService;
import ru.vrazumi.core.review.AnswerController;
import ru.vrazumi.core.review.AnswerService;
import ru.vrazumi.core.review.ReviewController;
import ru.vrazumi.core.review.ReviewService;
import ru.vrazumi.core.study.StudyController;
import ru.vrazumi.core.study.StudyService;

/** Все эндпоинты колод, карточек, изучения и оценок закрыты: без токена — 401 ProblemDetail. */
@WebLayerTest({
  DeckController.class,
  CardController.class,
  StudyController.class,
  ReviewController.class,
  AnswerController.class
})
class ApiRequiresTokenTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private DeckService deckService;
  @MockitoBean private CardService cardService;
  @MockitoBean private StudyService studyService;
  @MockitoBean private ReviewService reviewService;
  @MockitoBean private AnswerService answerService;

  @ParameterizedTest
  @CsvSource({
    "POST, /api/decks",
    "GET, /api/decks",
    "GET, /api/decks/1",
    "POST, /api/decks/1/cards",
    "GET, /api/decks/1/cards",
    "GET, /api/decks/1/study",
    "POST, /api/cards/1/review",
    "POST, /api/cards/1/answer"
  })
  void requestWithoutTokenIsUnauthorized(String method, String uri) throws Exception {
    mvc.perform(
            request(HttpMethod.valueOf(method), uri)
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Требуется авторизация: передайте access-токен."));
  }

  @AfterEach
  void servicesAreNotCalled() {
    verifyNoInteractions(deckService, cardService, studyService, reviewService, answerService);
  }
}
