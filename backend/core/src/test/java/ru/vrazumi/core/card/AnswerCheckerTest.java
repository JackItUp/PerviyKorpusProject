package ru.vrazumi.core.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.vrazumi.core.common.BadRequestException;

class AnswerCheckerTest {

  private final AnswerChecker checker = new AnswerChecker();

  /** Варианты 1 и 2 верные, 3 и 4 — нет. */
  private final Card twoCorrect = choiceCard(true, true, false, false);

  /** Верный только вариант 2. */
  private final Card oneCorrect = choiceCard(false, true, false, false);

  @Test
  void singleCorrectOptionSelected() {
    assertThat(checker.checkChoice(oneCorrect, List.of(2L))).isEqualTo(AnswerResult.CORRECT);
  }

  @Test
  void allCorrectOptionsSelectedInAnyOrder() {
    assertThat(checker.checkChoice(twoCorrect, List.of(2L, 1L))).isEqualTo(AnswerResult.CORRECT);
  }

  @Test
  void duplicateIdsDoNotMatter() {
    assertThat(checker.checkChoice(oneCorrect, List.of(2L, 2L))).isEqualTo(AnswerResult.CORRECT);
  }

  @Test
  void onlySomeCorrectOptionsSelectedIsPartial() {
    assertThat(checker.checkChoice(twoCorrect, List.of(1L))).isEqualTo(AnswerResult.PARTIAL);
  }

  @Test
  void correctPlusWrongOptionIsPartial() {
    assertThat(checker.checkChoice(oneCorrect, List.of(2L, 3L))).isEqualTo(AnswerResult.PARTIAL);
  }

  @Test
  void onlyWrongOptionsIsIncorrect() {
    assertThat(checker.checkChoice(twoCorrect, List.of(3L, 4L))).isEqualTo(AnswerResult.INCORRECT);
  }

  @Test
  void optionFromAnotherCardIsRejected() {
    assertThatThrownBy(() -> checker.checkChoice(oneCorrect, List.of(2L, 99L)))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            ex -> assertThat(ex.getField()).isEqualTo("selectedOptionIds"));
  }

  @Test
  void correctOptionIds() {
    assertThat(checker.correctOptionIds(twoCorrect)).containsExactlyInAnyOrder(1L, 2L);
  }

  /** Карточка с вариантами id 1..n. */
  static Card choiceCard(boolean... correct) {
    Card card = new Card();
    card.setType(CardType.MULTIPLE_CHOICE);
    for (int i = 0; i < correct.length; i++) {
      card.addOption("вариант " + (i + 1), correct[i]);
      card.getOptions().get(i).setId((long) i + 1);
    }
    return card;
  }
}
