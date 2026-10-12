package ru.vrazumi.core.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import ru.vrazumi.core.review.Rating;

@Schema(description = "Сохранённая оценка")
public record ReviewResponse(
    @Schema(example = "100") Long id,
    @Schema(example = "10") Long cardId,
    @Schema(example = "KNOW") Rating rating,
    @Schema(example = "2026-10-01T09:40:00Z") Instant reviewedAt) {}
