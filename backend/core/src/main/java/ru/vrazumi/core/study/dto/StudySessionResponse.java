package ru.vrazumi.core.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Карточки колоды для прохождения. Пустая колода — пустой список cards.")
public record StudySessionResponse(
    @Schema(example = "1") Long deckId,
    @Schema(example = "Английские глаголы") String deckTitle,
    List<StudyCardResponse> cards) {}
