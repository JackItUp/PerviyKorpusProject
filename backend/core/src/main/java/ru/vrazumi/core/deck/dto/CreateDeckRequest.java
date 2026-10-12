package ru.vrazumi.core.deck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.vrazumi.core.common.Texts;
import ru.vrazumi.core.deck.Deck;

/**
 * Запрос на создание колоды. Строки обрезаются по краям ещё до валидации, поэтому ограничения длины
 * проверяются для уже обрезанного текста.
 */
@Schema(description = "Новая колода")
public record CreateDeckRequest(
    @Schema(
            description = "Название колоды",
            example = "Английские глаголы",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = Deck.TITLE_MAX_LENGTH)
        @NotBlank(message = "Название колоды обязательно")
        @Size(
            max = Deck.TITLE_MAX_LENGTH,
            message = "Название колоды должно быть не длиннее {max} символов")
        String title,
    @Schema(
            description = "Описание колоды",
            example = "Неправильные глаголы",
            nullable = true,
            maxLength = Deck.DESCRIPTION_MAX_LENGTH)
        @Size(
            max = Deck.DESCRIPTION_MAX_LENGTH,
            message = "Описание колоды должно быть не длиннее {max} символов")
        String description) {

  public CreateDeckRequest {
    title = Texts.trim(title);
    description = Texts.trimToNull(description);
  }
}
