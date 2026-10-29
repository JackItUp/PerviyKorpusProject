package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Карточка с вариантами: создание, изучение без подсказок, проверка ответа и запись оценки. */
class MultipleChoiceIT extends AbstractIntegrationTest {

  private long deckId;
  private long cardId;
  private List<Integer> optionIds;

  @BeforeEach
  void createChoiceCard() throws Exception {
    actAs("alice");
    deckId = id(postJson("/api/decks", "{\"title\": \"Столицы\"}"));
    String body =
        postJson(
                "/api/decks/" + deckId + "/cards",
                """
                {"type": "MULTIPLE_CHOICE", "question": "Какие города — столицы?",
                 "options": [
                   {"text": "Париж", "correct": true},
                   {"text": "Сидней", "correct": false},
                   {"text": "Оттава", "correct": true}
                 ]}
                """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"))
            .andExpect(jsonPath("$.answer").doesNotExist())
            .andExpect(jsonPath("$.options.length()").value(3))
            .andExpect(jsonPath("$.options[0].text").value("Париж"))
            .andExpect(jsonPath("$.options[0].correct").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    cardId = ((Number) JsonPath.read(body, "$.id")).longValue();
    optionIds = JsonPath.read(body, "$.options[*].id");
  }

  @Test
  void studyHidesCorrectFlags() throws Exception {
    mvc.perform(get("/api/decks/{deckId}/study", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[0].type").value("MULTIPLE_CHOICE"))
        .andExpect(jsonPath("$.cards[0].answer").doesNotExist())
        .andExpect(
            jsonPath("$.cards[0].options[*].text").value(contains("Париж", "Сидней", "Оттава")))
        .andExpect(jsonPath("$.cards[0].options[0].correct").doesNotExist());
  }

  @Test
  void answersAreCheckedAndRecorded() throws Exception {
    answer(optionIds.get(2), optionIds.get(0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("CORRECT"))
        .andExpect(jsonPath("$.correctOptionIds.length()").value(2))
        .andExpect(jsonPath("$.review.rating").value("KNOW"));
    answer(optionIds.get(0))
        .andExpect(jsonPath("$.result").value("PARTIAL"))
        .andExpect(jsonPath("$.review.rating").value("HARD"));
    answer(optionIds.get(1))
        .andExpect(jsonPath("$.result").value("INCORRECT"))
        .andExpect(jsonPath("$.review.rating").value("DONT_KNOW"));

    assertThat(
            jdbc.queryForList(
                "SELECT rating FROM review WHERE card_id = ? ORDER BY id", String.class, cardId))
        .containsExactly("KNOW", "HARD", "DONT_KNOW");
  }

  @Test
  void emptySelectionAndForeignOptionAreRejected() throws Exception {
    postJson("/api/cards/" + cardId + "/answer", "{\"selectedOptionIds\": []}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("selectedOptionIds"));
    answer(999_999)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("selectedOptionIds"));

    assertThat(jdbc.queryForObject("SELECT count(*) FROM review", Long.class)).isZero();
  }

  @Test
  void selfReviewIsOnlyForNormalCards() throws Exception {
    postJson("/api/cards/" + cardId + "/review", "{\"rating\": \"KNOW\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(containsString("/answer")));
  }

  @Test
  void invalidChoiceCardsAreRejected() throws Exception {
    String uri = "/api/decks/" + deckId + "/cards";
    postJson(
            uri,
            "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"q\","
                + " \"options\": [{\"text\": \"a\", \"correct\": true}]}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("options"));
    postJson(
            uri,
            "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"q\","
                + " \"options\": [{\"text\": \"a\"}, {\"text\": \"b\"}]}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("Отметьте хотя бы один верный вариант"));
  }

  private ResultActions answer(Object... optionIds) throws Exception {
    return postJson(
        "/api/cards/" + cardId + "/answer",
        "{\"selectedOptionIds\": %s}".formatted(List.of(optionIds)));
  }

  private ResultActions postJson(String uri, String json) throws Exception {
    return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static long id(ResultActions result) throws Exception {
    return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"))
        .longValue();
  }
}
