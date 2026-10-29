package ru.vrazumi.core.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.vrazumi.core.auth.CurrentUserId;
import ru.vrazumi.core.review.dto.AnswerRequest;
import ru.vrazumi.core.review.dto.AnswerResultResponse;

@Tag(name = "Оценки")
@RestController
@RequiredArgsConstructor
public class AnswerController {

  private final AnswerService answerService;

  @Operation(
      summary = "Ответить на карточку, которую проверяет сервер",
      description =
          "Для MULTIPLE_CHOICE передайте selectedOptionIds. Результат записывается в историю"
              + " как оценка: CORRECT → KNOW, PARTIAL → HARD, INCORRECT → DONT_KNOW.")
  @ApiResponse(responseCode = "200", description = "Ответ проверен, оценка записана")
  @ApiResponse(
      responseCode = "400",
      description = "Нет ответа, вариант не из этой карточки или карточка типа NORMAL",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Карточки нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/api/cards/{cardId}/answer")
  public AnswerResultResponse answer(
      @PathVariable Long cardId, @RequestBody AnswerRequest request, @CurrentUserId Long userId) {
    return answerService.answer(cardId, request, userId);
  }
}
