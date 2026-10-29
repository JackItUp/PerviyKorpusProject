package ru.vrazumi.core.review;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.card.CardType;
import ru.vrazumi.core.common.BadRequestException;
import ru.vrazumi.core.review.dto.CreateReviewRequest;
import ru.vrazumi.core.review.dto.ReviewResponse;
import ru.vrazumi.core.user.UserService;

/** История ответов: каждая оценка — новая запись, ничего не перезаписывается. */
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final CardService cardService;
  private final UserService userService;
  private final Clock clock;

  /** Самооценка ответа. Только для NORMAL: остальные типы проверяет сервер через /answer. */
  @Transactional
  public ReviewResponse create(Long cardId, CreateReviewRequest request, Long userId) {
    Card card = cardService.getExisting(cardId, userId);
    if (card.getType() != CardType.NORMAL) {
      throw new BadRequestException(
          null,
          "Карточку типа %s проверяет сервер: отправьте ответ в POST /api/cards/%d/answer"
              .formatted(card.getType(), cardId));
    }
    return record(card, request.rating(), userId);
  }

  /** Записывает оценку, которую определил сервер (например, по результату проверки ответа). */
  @Transactional
  public ReviewResponse record(Card card, Rating rating, Long userId) {
    Instant reviewedAt = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    Review review = reviewMapper.toEntity(rating, card, reviewedAt);
    review.setUser(userService.getReference(userId));
    return reviewMapper.toResponse(reviewRepository.save(review));
  }
}
