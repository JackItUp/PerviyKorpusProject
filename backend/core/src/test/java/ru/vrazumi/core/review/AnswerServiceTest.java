package ru.vrazumi.core.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vrazumi.core.card.AnswerChecker;
import ru.vrazumi.core.card.AnswerResult;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.card.CardType;
import ru.vrazumi.core.common.BadRequestException;
import ru.vrazumi.core.review.dto.AnswerRequest;
import ru.vrazumi.core.review.dto.AnswerResultResponse;
import ru.vrazumi.core.review.dto.ReviewResponse;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

  private static final long USER = 5L;

  @Mock private CardService cardService;
  @Mock private ReviewService reviewService;
  private AnswerService answerService;

  /** Верные варианты 1 и 2, неверные 3 и 4. */
  private Card card;

  @BeforeEach
  void setUp() {
    answerService = new AnswerService(cardService, new AnswerChecker(), reviewService);
    card = new Card();
    card.setId(10L);
    card.setType(CardType.MULTIPLE_CHOICE);
    card.addOption("a", true);
    card.addOption("b", true);
    card.addOption("c", false);
    card.addOption("d", false);
    for (int i = 0; i < 4; i++) {
      card.getOptions().get(i).setId((long) i + 1);
    }
  }

  @ParameterizedTest
  @CsvSource({"'1,2', CORRECT, KNOW", "'1', PARTIAL, HARD", "'3,4', INCORRECT, DONT_KNOW"})
  void choiceAnswerIsCheckedAndRecordedAsRating(
      String selected, AnswerResult expected, Rating rating) {
    when(cardService.getExisting(10L, USER)).thenReturn(card);
    when(reviewService.record(card, rating, USER))
        .thenReturn(new ReviewResponse(100L, 10L, rating, Instant.now()));
    List<Long> ids = List.of(selected.split(",")).stream().map(Long::valueOf).toList();

    AnswerResultResponse response = answerService.answer(10L, new AnswerRequest(ids, null), USER);

    assertThat(response.result()).isEqualTo(expected);
    assertThat(response.correctOptionIds()).containsExactly(1L, 2L);
    assertThat(response.review().rating()).isEqualTo(rating);
    verify(reviewService).record(card, rating, USER);
  }

  @Test
  void emptySelectionIsRejected() {
    when(cardService.getExisting(10L, USER)).thenReturn(card);

    assertThatThrownBy(() -> answerService.answer(10L, new AnswerRequest(List.of(), null), USER))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            ex -> assertThat(ex.getField()).isEqualTo("selectedOptionIds"));
    verifyNoInteractions(reviewService);
  }

  @Test
  void missingSelectionIsRejected() {
    when(cardService.getExisting(10L, USER)).thenReturn(card);

    assertThatThrownBy(() -> answerService.answer(10L, new AnswerRequest(null, null), USER))
        .isInstanceOf(BadRequestException.class);
  }

  @ParameterizedTest
  @CsvSource({"'  WENT ', CORRECT, KNOW", "gone, INCORRECT, DONT_KNOW"})
  void textAnswerIsCheckedAndReturnsCorrectAnswer(
      String text, AnswerResult expected, Rating rating) {
    Card textCard = textCard();
    when(cardService.getExisting(12L, USER)).thenReturn(textCard);
    when(reviewService.record(textCard, rating, USER))
        .thenReturn(new ReviewResponse(101L, 12L, rating, Instant.now()));

    AnswerResultResponse response = answerService.answer(12L, new AnswerRequest(null, text), USER);

    assertThat(response.result()).isEqualTo(expected);
    assertThat(response.correctAnswer()).isEqualTo("went");
    assertThat(response.correctOptionIds()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void blankTextAnswerIsRejected(String text) {
    when(cardService.getExisting(12L, USER)).thenReturn(textCard());

    assertThatThrownBy(() -> answerService.answer(12L, new AnswerRequest(null, text), USER))
        .isInstanceOfSatisfying(
            BadRequestException.class, ex -> assertThat(ex.getField()).isEqualTo("text"));
    verifyNoInteractions(reviewService);
  }

  private static Card textCard() {
    Card card = new Card();
    card.setId(12L);
    card.setType(CardType.TEXT_INPUT);
    card.setAnswer("went");
    return card;
  }

  @Test
  void normalCardCannotBeAnswered() {
    Card normal = new Card();
    normal.setId(11L);
    when(cardService.getExisting(11L, USER)).thenReturn(normal);

    assertThatThrownBy(() -> answerService.answer(11L, new AnswerRequest(List.of(1L), null), USER))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("/review");
    verify(reviewService, never()).record(any(), any(), any());
  }
}
