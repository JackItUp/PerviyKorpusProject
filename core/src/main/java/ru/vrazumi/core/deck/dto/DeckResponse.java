package ru.vrazumi.core.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Колода")
public record DeckResponse(
    @Schema(example = "1") Long id,
    @Schema(example = "Английские глаголы") String title,
    @Schema(example = "Неправильные глаголы", nullable = true) String description,
    @Schema(description = "Количество карточек в колоде", example = "12") long cardCount,
    @Schema(example = "2026-10-01T09:30:00Z") Instant createdAt,
    @Schema(example = "2026-10-01T09:30:00Z") Instant updatedAt) {}
