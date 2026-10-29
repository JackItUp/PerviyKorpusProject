package ru.vrazumi.core.review;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.card.AnswerChecker;
import ru.vrazumi.core.card.AnswerResult;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.common.BadRequestException;
import ru.vrazumi.core.review.dto.AnswerRequest;
import ru.vrazumi.core.review.dto.AnswerResultResponse;
import ru.vrazumi.core.review.dto.ReviewResponse;

/**
 * Проверка ответа на карточку без самооценки. Результат сразу записывается в историю как обычная
 * оценка, поэтому статистика считается одинаково для всех типов карточек.
 */
@Service
@RequiredArgsConstructor
public class AnswerService {

  private final CardService cardService;
  private final AnswerChecker answerChecker;
  private final ReviewService reviewService;

  @Transactional
  public AnswerResultResponse answer(Long cardId, AnswerRequest request, Long userId) {
    Card card = cardService.getExisting(cardId, userId);
    return switch (card.getType()) {
      case NORMAL ->
          throw new BadRequestException(
              null,
              "Обычную карточку оценивают самостоятельно: POST /api/cards/%d/review"
                  .formatted(cardId));
      case MULTIPLE_CHOICE -> answerChoice(card, request, userId);
      case TEXT_INPUT -> answerText(card, request, userId);
    };
  }

  private AnswerResultResponse answerChoice(Card card, AnswerRequest request, Long userId) {
    List<Long> selected = request.selectedOptionIds();
    if (selected == null || selected.isEmpty()) {
      throw new BadRequestException("selectedOptionIds", "Выберите хотя бы один вариант");
    }
    AnswerResult result = answerChecker.checkChoice(card, selected);
    ReviewResponse review = reviewService.record(card, toRating(result), userId);
    List<Long> correct = answerChecker.correctOptionIds(card).stream().sorted().toList();
    return new AnswerResultResponse(result, correct, null, review);
  }

  private AnswerResultResponse answerText(Card card, AnswerRequest request, Long userId) {
    if (request.text() == null || request.text().isBlank()) {
      throw new BadRequestException("text", "Введите ответ");
    }
    AnswerResult result = answerChecker.checkText(card, request.text());
    ReviewResponse review = reviewService.record(card, toRating(result), userId);
    return new AnswerResultResponse(result, null, card.getAnswer(), review);
  }

  static Rating toRating(AnswerResult result) {
    return switch (result) {
      case CORRECT -> Rating.KNOW;
      case PARTIAL -> Rating.HARD;
      case INCORRECT -> Rating.DONT_KNOW;
    };
  }
}
