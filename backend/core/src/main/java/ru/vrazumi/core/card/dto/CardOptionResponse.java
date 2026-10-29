package ru.vrazumi.core.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Вариант ответа (для автора карточки, с отметкой верного)")
public record CardOptionResponse(
    @Schema(example = "31") Long id,
    @Schema(example = "went") String text,
    @Schema(example = "true") boolean correct) {}
