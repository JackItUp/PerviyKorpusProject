package ru.vrazumi.core.review;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.DefaultTestUserConfig;
import ru.vrazumi.core.WebLayerTest;
import ru.vrazumi.core.card.CardNotFoundException;
import ru.vrazumi.core.review.dto.CreateReviewRequest;
import ru.vrazumi.core.review.dto.ReviewResponse;

@WebLayerTest(ReviewController.class)
class ReviewControllerTest {

  private static final long USER = DefaultTestUserConfig.USER_ID;

  private static final Instant NOW = Instant.parse("2026-10-01T09:40:00Z");

  @Autowired private MockMvc mvc;
  @MockitoBean private ReviewService reviewService;

  @ParameterizedTest
  @EnumSource(Rating.class)
  void acceptsEveryRating(Rating rating) throws Exception {
    when(reviewService.create(10L, new CreateReviewRequest(rating), USER))
        .thenReturn(new ReviewResponse(100L, 10L, rating, NOW));

    postReview(10, "{\"rating\": \"%s\"}".formatted(rating))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.cardId").value(10))
        .andExpect(jsonPath("$.rating").value(rating.name()))
        .andExpect(jsonPath("$.reviewedAt").value("2026-10-01T09:40:00Z"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"SUPER\"", "\"know\"", "\"\"", "1", "true"})
  void rejectsUnknownRating(String value) throws Exception {
    postReview(10, "{\"rating\": %s}".formatted(value))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors[0].field").value("rating"));
    verifyNoInteractions(reviewService);
  }

  @Test
  void unknownRatingMessageListsAllowedValues() throws Exception {
    postReview(10, "{\"rating\": \"SUPER\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Недопустимое значение поля 'rating'."))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value(
                    "Недопустимое значение 'SUPER'. Допустимые значения: DONT_KNOW, HARD, KNOW,"
                        + " EASY"));
  }

  @Test
  void rejectsNullRating() throws Exception {
    postReview(10, "{\"rating\": null}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("rating"))
        .andExpect(jsonPath("$.errors[0].message").value(containsString("DONT_KNOW")));
    verifyNoInteractions(reviewService);
  }

  @Test
  void rejectsMissingRating() throws Exception {
    postReview(10, "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("rating"));
    verifyNoInteractions(reviewService);
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    postReview(10, "{\"rating\": KNOW")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    verifyNoInteractions(reviewService);
  }

  @Test
  void rejectsMissingBody() throws Exception {
    mvc.perform(post("/api/cards/10/review").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(reviewService);
  }

  @Test
  void returns404ForMissingCard() throws Exception {
    when(reviewService.create(eq(99L), any(), eq(USER))).thenThrow(new CardNotFoundException(99L));

    postReview(99, "{\"rating\": \"KNOW\"}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Карточка с id=99 не найдена"));
  }

  @Test
  void rejectsNonNumericCardId() throws Exception {
    mvc.perform(
            post("/api/cards/abc/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\": \"KNOW\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    verifyNoInteractions(reviewService);
  }

  private ResultActions postReview(long cardId, String json) throws Exception {
    return mvc.perform(
        post("/api/cards/{cardId}/review", cardId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }
}
