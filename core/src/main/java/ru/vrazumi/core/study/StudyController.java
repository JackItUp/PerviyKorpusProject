package ru.vrazumi.core.study;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.vrazumi.core.study.dto.StudySessionResponse;

@Tag(name = "Изучение")
@RestController
@RequiredArgsConstructor
public class StudyController {

  private final StudyService studyService;

  @Operation(
      summary = "Карточки колоды для изучения",
      description = "Карточки в порядке добавления. Пустая колода — 200 с пустым списком cards.")
  @ApiResponse(responseCode = "200", description = "Сессия изучения")
  @ApiResponse(
      responseCode = "404",
      description = "Колоды нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/api/decks/{deckId}/study")
  public StudySessionResponse getStudySession(@PathVariable Long deckId) {
    return studyService.getStudySession(deckId);
  }
}
