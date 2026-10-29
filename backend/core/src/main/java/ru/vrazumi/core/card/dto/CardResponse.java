package ru.vrazumi.core.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import ru.vrazumi.core.card.CardType;

@Schema(description = "Карточка")
public record CardResponse(
    @Schema(example = "10") Long id,
    @Schema(example = "1") Long deckId,
    @Schema(example = "NORMAL") CardType type,
    @Schema(example = "go") String question,
    @Schema(description = "Ответ; null у MULTIPLE_CHOICE", example = "went, gone", nullable = true)
        String answer,
    @Schema(description = "Варианты; пустой список у NORMAL") List<CardOptionResponse> options,
    @Schema(example = "2026-10-01T09:35:00Z") Instant createdAt,
    @Schema(example = "2026-10-01T09:35:00Z") Instant updatedAt) {}
