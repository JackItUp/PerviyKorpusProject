package ru.vrazumi.core.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Ответ пользователя. Какое поле нужно, зависит от типа карточки. */
@Schema(description = "Ответ на карточку, которую проверяет сервер")
public record AnswerRequest(
    @Schema(description = "Для MULTIPLE_CHOICE: id выбранных вариантов", example = "[31, 33]")
        List<Long> selectedOptionIds,
    @Schema(description = "Для TEXT_INPUT: введённый ответ", example = "went") String text) {}
