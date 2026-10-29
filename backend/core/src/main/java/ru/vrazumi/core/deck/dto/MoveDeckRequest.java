package ru.vrazumi.core.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Перенос колоды в группу")
public record MoveDeckRequest(
    @Schema(
            description = "id группы; null — убрать колоду из группы",
            example = "3",
            nullable = true)
        Long groupId) {}
