package ru.vrazumi.core.review;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.review.dto.CreateReviewRequest;
import ru.vrazumi.core.review.dto.ReviewResponse;
import ru.vrazumi.core.user.UserService;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final CardService cardService;
  private final UserService userService;
  private final Clock clock;

  /**
   * Сохраняет оценку как новую запись. Повторная оценка той же карточки не перезаписывает старую.
   */
  @Transactional
  public ReviewResponse create(Long cardId, CreateReviewRequest request, Long userId) {
    Card card = cardService.getExisting(cardId, userId);
    Instant reviewedAt = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    Review review = reviewMapper.toEntity(request, card, reviewedAt);
    review.setUser(userService.getReference(userId));
    return reviewMapper.toResponse(reviewRepository.save(review));
  }
}
