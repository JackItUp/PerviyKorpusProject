package ru.vrazumi.core.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.vrazumi.core.common.Texts;
import ru.vrazumi.core.group.DeckGroup;

/** Создание или переименование группы. Имя обрезается по краям до валидации. */
@Schema(description = "Группа колод")
public record DeckGroupRequest(
    @Schema(
            example = "Английский язык",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = DeckGroup.NAME_MAX_LENGTH)
        @NotBlank(message = "Название группы обязательно")
        @Size(
            max = DeckGroup.NAME_MAX_LENGTH,
            message = "Название группы должно быть не длиннее {max} символов")
        String name) {

  public DeckGroupRequest {
    name = Texts.trim(name);
  }
}
