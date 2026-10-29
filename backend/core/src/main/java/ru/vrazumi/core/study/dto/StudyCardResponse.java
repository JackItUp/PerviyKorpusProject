package ru.vrazumi.core.study.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import ru.vrazumi.core.card.CardType;

/**
 * Карточка в режиме изучения. Для карточек, которые проверяет сервер, правильный ответ не отдаётся:
 * у MULTIPLE_CHOICE нет отметок верных вариантов, его ответ придёт из POST /api/cards/{id}/answer.
 */
@Schema(description = "Карточка в режиме изучения")
public record StudyCardResponse(
    @Schema(example = "10") Long id,
    @Schema(example = "NORMAL") CardType type,
    @Schema(example = "go") String question,
    @Schema(description = "Только у NORMAL", example = "went, gone")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String answer,
    @Schema(description = "Варианты без отметки верных; пустой список у NORMAL")
        List<StudyOptionResponse> options) {}
