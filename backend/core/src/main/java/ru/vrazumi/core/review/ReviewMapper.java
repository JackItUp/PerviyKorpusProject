package ru.vrazumi.core.review;

import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.review.dto.CreateReviewRequest;
import ru.vrazumi.core.review.dto.ReviewResponse;

@Mapper
public interface ReviewMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "card", source = "card")
  @Mapping(target = "rating", source = "request.rating")
  @Mapping(target = "reviewedAt", source = "reviewedAt")
  Review toEntity(CreateReviewRequest request, Card card, Instant reviewedAt);

  @Mapping(target = "cardId", source = "card.id")
  ReviewResponse toResponse(Review review);
}
