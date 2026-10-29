package ru.vrazumi.core.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Группа колод")
public record DeckGroupResponse(
    @Schema(example = "3") Long id,
    @Schema(example = "Английский язык") String name,
    @Schema(description = "Количество колод в группе", example = "4") long deckCount,
    @Schema(example = "2026-10-20T09:30:00Z") Instant createdAt,
    @Schema(example = "2026-10-20T09:30:00Z") Instant updatedAt) {}
