package ru.vrazumi.core.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.vrazumi.core.card.CardType;

@Schema(description = "Карточка в режиме изучения")
public record StudyCardResponse(
    @Schema(example = "10") Long id,
    @Schema(example = "NORMAL") CardType type,
    @Schema(example = "go") String question,
    @Schema(example = "went, gone") String answer) {}
