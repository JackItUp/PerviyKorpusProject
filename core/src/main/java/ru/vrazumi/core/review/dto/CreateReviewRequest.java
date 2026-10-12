package ru.vrazumi.core.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.vrazumi.core.review.Rating;

@Schema(description = "Оценка ответа на карточку")
public record CreateReviewRequest(
    @Schema(
            description = "Оценка: Не знаю, Трудно, Знаю, Легко",
            example = "KNOW",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Оценка обязательна. Допустимые значения: DONT_KNOW, HARD, KNOW, EASY")
        Rating rating) {}
