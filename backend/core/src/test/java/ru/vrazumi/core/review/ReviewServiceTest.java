package ru.vrazumi.core.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardNotFoundException;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.review.dto.CreateReviewRequest;
import ru.vrazumi.core.review.dto.ReviewResponse;
import ru.vrazumi.core.user.User;
import ru.vrazumi.core.user.UserService;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-01T09:40:00.123456789Z");

  @Mock private ReviewRepository reviewRepository;
  @Mock private CardService cardService;
  @Mock private UserService userService;
  private ReviewService reviewService;

  private final AtomicLong ids = new AtomicLong(100);
  private static final long USER = 5L;
  private final User user = new User();

  @BeforeEach
  void setUp() {
    reviewService =
        new ReviewService(
            reviewRepository,
            Mappers.getMapper(ReviewMapper.class),
            cardService,
            userService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @ParameterizedTest
  @EnumSource(Rating.class)
  void createSavesReviewWithEveryRating(Rating rating) {
    Card card = card(10L);
    when(cardService.getExisting(10L, USER)).thenReturn(card);
    stubSave();

    ReviewResponse response = reviewService.create(10L, new CreateReviewRequest(rating), USER);

    ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
    verify(reviewRepository).save(saved.capture());
    assertThat(saved.getValue().getCard()).isSameAs(card);
    assertThat(saved.getValue().getUser()).isSameAs(user);
    assertThat(saved.getValue().getRating()).isEqualTo(rating);
    assertThat(saved.getValue().getReviewedAt())
        .isEqualTo(Instant.parse("2026-10-01T09:40:00.123456Z"));
    assertThat(response.cardId()).isEqualTo(10L);
    assertThat(response.rating()).isEqualTo(rating);
  }

  @Test
  void repeatedReviewCreatesNewRecord() {
    when(cardService.getExisting(10L, USER)).thenReturn(card(10L));
    stubSave();

    ReviewResponse first = reviewService.create(10L, new CreateReviewRequest(Rating.HARD), USER);
    ReviewResponse second = reviewService.create(10L, new CreateReviewRequest(Rating.EASY), USER);

    ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
    verify(reviewRepository, times(2)).save(saved.capture());
    assertThat(saved.getAllValues()).doesNotHaveDuplicates();
    assertThat(saved.getAllValues()).allSatisfy(review -> assertThat(review.getId()).isNotNull());
    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void createThrowsNotFoundForMissingCard() {
    when(cardService.getExisting(99L, USER)).thenThrow(new CardNotFoundException(99L));

    assertThatThrownBy(() -> reviewService.create(99L, new CreateReviewRequest(Rating.KNOW), USER))
        .isInstanceOf(CardNotFoundException.class)
        .hasMessageContaining("99");
    verifyNoInteractions(reviewRepository);
  }

  private void stubSave() {
    when(userService.getReference(USER)).thenReturn(user);
    when(reviewRepository.save(any(Review.class)))
        .thenAnswer(
            invocation -> {
              Review review = invocation.getArgument(0);
              review.setId(ids.getAndIncrement());
              return review;
            });
  }

  private static Card card(Long id) {
    Card card = new Card();
    card.setId(id);
    return card;
  }
}
