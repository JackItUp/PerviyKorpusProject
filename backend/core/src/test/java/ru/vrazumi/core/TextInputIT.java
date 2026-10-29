package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Карточка с вводом ответа: ответ не виден до проверки, сравнение без регистра и пробелов. */
class TextInputIT extends AbstractIntegrationTest {

  private long deckId;
  private long cardId;

  @BeforeEach
  void createTextCard() throws Exception {
    actAs("alice");
    deckId = id(postJson("/api/decks", "{\"title\": \"Глаголы\"}"));
    cardId =
        id(
            postJson(
                    "/api/decks/" + deckId + "/cards",
                    "{\"type\": \"TEXT_INPUT\", \"question\": \"go (past)\", \"answer\": \"went\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TEXT_INPUT"))
                .andExpect(jsonPath("$.answer").value("went")));
  }

  @Test
  void studyDoesNotRevealAnswer() throws Exception {
    mvc.perform(get("/api/decks/{deckId}/study", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[0].type").value("TEXT_INPUT"))
        .andExpect(jsonPath("$.cards[0].question").value("go (past)"))
        .andExpect(jsonPath("$.cards[0].answer").doesNotExist());
  }

  @Test
  void answerIsComparedIgnoringCaseAndSpaces() throws Exception {
    answer("  WENT  ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("CORRECT"))
        .andExpect(jsonPath("$.correctAnswer").value("went"))
        .andExpect(jsonPath("$.correctOptionIds").doesNotExist())
        .andExpect(jsonPath("$.review.rating").value("KNOW"));
    answer("goed")
        .andExpect(jsonPath("$.result").value("INCORRECT"))
        .andExpect(jsonPath("$.correctAnswer").value("went"))
        .andExpect(jsonPath("$.review.rating").value("DONT_KNOW"));

    assertThat(
            jdbc.queryForList(
                "SELECT rating FROM review WHERE card_id = ? ORDER BY id", String.class, cardId))
        .containsExactly("KNOW", "DONT_KNOW");
  }

  @Test
  void emptyAnswerIsRejected() throws Exception {
    answer("   ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("text"));
    postJson("/api/cards/" + cardId + "/answer", "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("text"));

    assertThat(jdbc.queryForObject("SELECT count(*) FROM review", Long.class)).isZero();
  }

  @Test
  void textCardRequiresAnswer() throws Exception {
    postJson(
            "/api/decks/" + deckId + "/cards",
            "{\"type\": \"TEXT_INPUT\", \"question\": \"be (past)\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("answer"));
  }

  private ResultActions answer(String text) throws Exception {
    return postJson("/api/cards/" + cardId + "/answer", "{\"text\": \"%s\"}".formatted(text));
  }

  private ResultActions postJson(String uri, String json) throws Exception {
    return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static long id(ResultActions result) throws Exception {
    return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"))
        .longValue();
  }
}
