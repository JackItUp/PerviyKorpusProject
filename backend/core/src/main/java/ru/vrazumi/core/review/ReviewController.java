package ru.vrazumi.core.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.vrazumi.core.auth.CurrentUserId;
import ru.vrazumi.core.review.dto.CreateReviewRequest;
import ru.vrazumi.core.review.dto.ReviewResponse;

@Tag(name = "Оценки")
@RestController
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(
      summary = "Оценить ответ на карточку",
      description = "Каждый вызов создаёт новую запись в истории ответов.")
  @ApiResponse(responseCode = "201", description = "Оценка сохранена")
  @ApiResponse(
      responseCode = "400",
      description = "Оценка отсутствует или имеет недопустимое значение",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Карточки нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/api/cards/{cardId}/review")
  @ResponseStatus(HttpStatus.CREATED)
  public ReviewResponse create(
      @PathVariable Long cardId,
      @Valid @RequestBody CreateReviewRequest request,
      @CurrentUserId Long userId) {
    return reviewService.create(cardId, request, userId);
  }
}
