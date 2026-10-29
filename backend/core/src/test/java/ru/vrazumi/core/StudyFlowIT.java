package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.review.Rating;

/** Сквозные сценарии спринта 1 через HTTP API на настоящем PostgreSQL. */
class StudyFlowIT extends AbstractIntegrationTest {

  @BeforeEach
  void signIn() throws Exception {
    actAs("alice");
  }

  @Test
  void createDeckAddCardsStudyAndReviewTwice() throws Exception {
    long deckId =
        idOf(
            postJson(
                    "/api/decks",
                    "{\"title\": \"  Английские глаголы \", \"description\": \"   \"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/decks/1")))
                .andExpect(jsonPath("$.title").value("Английские глаголы"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.cardCount").value(0)));

    long goId = createCard(deckId, "go", "went, gone");
    long beId = createCard(deckId, "be", "was/were, been");

    mvc.perform(get("/api/decks/{deckId}/study", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deckId").value(deckId))
        .andExpect(jsonPath("$.deckTitle").value("Английские глаголы"))
        .andExpect(jsonPath("$.cards.length()").value(2))
        .andExpect(jsonPath("$.cards[0].id").value(goId))
        .andExpect(jsonPath("$.cards[0].type").value("NORMAL"))
        .andExpect(jsonPath("$.cards[0].question").value("go"))
        .andExpect(jsonPath("$.cards[0].answer").value("went, gone"))
        .andExpect(jsonPath("$.cards[1].id").value(beId));

    long firstReview = review(goId, "HARD");
    long secondReview = review(goId, "EASY");

    assertThat(firstReview).isNotEqualTo(secondReview);
    assertThat(
            jdbc.queryForList(
                "SELECT rating FROM review WHERE card_id = ? ORDER BY reviewed_at, id",
                String.class,
                goId))
        .containsExactly("HARD", "EASY");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM review", Long.class)).isEqualTo(2);

    mvc.perform(get("/api/decks/{deckId}", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardCount").value(2));
  }

  @Test
  void emptyDeckListReturnsEmptyArray() throws Exception {
    mvc.perform(get("/api/decks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void deckListIsNewestFirstWithCardCounts() throws Exception {
    long first = createDeck("Первая");
    long second = createDeck("Вторая");
    long third = createDeck("Третья");
    createCard(first, "q1", "a1");
    createCard(first, "q2", "a2");
    createCard(third, "q3", "a3");

    mvc.perform(get("/api/decks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").value(third))
        .andExpect(jsonPath("$[0].cardCount").value(1))
        .andExpect(jsonPath("$[1].id").value(second))
        .andExpect(jsonPath("$[1].cardCount").value(0))
        .andExpect(jsonPath("$[2].id").value(first))
        .andExpect(jsonPath("$[2].cardCount").value(2));
  }

  @Test
  void emptyDeckStudyReturnsEmptyCards() throws Exception {
    long deckId = createDeck("Пустая колода");

    mvc.perform(get("/api/decks/{deckId}/study", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deckTitle").value("Пустая колода"))
        .andExpect(jsonPath("$.cards").isEmpty());
    mvc.perform(get("/api/decks/{deckId}/cards", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void missingDeckAndCardReturn404() throws Exception {
    mvc.perform(get("/api/decks/999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/decks/999/cards")).andExpect(status().isNotFound());
    mvc.perform(get("/api/decks/999/study"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Колода с id=999 не найдена"));
    postJson("/api/decks/999/cards", "{\"question\": \"go\", \"answer\": \"went\"}")
        .andExpect(status().isNotFound());
    postJson("/api/cards/999/review", "{\"rating\": \"KNOW\"}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Карточка с id=999 не найдена"));

    assertThat(jdbc.queryForObject("SELECT count(*) FROM card", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM review", Long.class)).isZero();
  }

  @Test
  void longTextIsRejectedAndMaxLengthIsStored() throws Exception {
    postJson("/api/decks", "{\"title\": \"" + "x".repeat(101) + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("title"));

    long deckId = createDeck("x".repeat(100));
    postJson(
            "/api/decks/" + deckId + "/cards",
            "{\"question\": \"" + "q".repeat(2001) + "\", \"answer\": \"a\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("question"));

    long cardId = createCard(deckId, "q".repeat(2000), "a".repeat(2000));
    assertThat(
            jdbc.queryForObject(
                "SELECT length(question) + length(answer) FROM card WHERE id = ?",
                Integer.class,
                cardId))
        .isEqualTo(4000);
  }

  @ParameterizedTest
  @EnumSource(Rating.class)
  void everyRatingIsStored(Rating rating) throws Exception {
    long cardId = createCard(createDeck("Колода"), "go", "went");

    review(cardId, rating.name());

    assertThat(
            jdbc.queryForObject(
                "SELECT rating FROM review WHERE card_id = ?", String.class, cardId))
        .isEqualTo(rating.name());
  }

  @Test
  void invalidRatingIsRejectedAndNotStored() throws Exception {
    long cardId = createCard(createDeck("Колода"), "go", "went");

    for (String body : List.of("{\"rating\": \"SUPER\"}", "{\"rating\": null}", "{}")) {
      postJson("/api/cards/" + cardId + "/review", body)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("rating"));
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM review", Long.class)).isZero();
  }

  private long createDeck(String title) throws Exception {
    return idOf(
        postJson("/api/decks", "{\"title\": \"%s\"}".formatted(title))
            .andExpect(status().isCreated()));
  }

  private long createCard(long deckId, String question, String answer) throws Exception {
    ResultActions result =
        postJson(
                "/api/decks/" + deckId + "/cards",
                "{\"question\": \"%s\", \"answer\": \"%s\"}".formatted(question, answer))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.deckId").value(deckId))
            .andExpect(jsonPath("$.type").value("NORMAL"));
    long cardId = idOf(result);
    result.andExpect(header().string("Location", endsWith("/api/cards/" + cardId)));
    return cardId;
  }

  private long review(long cardId, String rating) throws Exception {
    return idOf(
        postJson("/api/cards/" + cardId + "/review", "{\"rating\": \"%s\"}".formatted(rating))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cardId").value(cardId))
            .andExpect(jsonPath("$.rating").value(rating))
            .andExpect(jsonPath("$.reviewedAt").exists()));
  }

  private ResultActions postJson(String uri, String json) throws Exception {
    return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static long idOf(ResultActions result) throws Exception {
    Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    return id.longValue();
  }
}
