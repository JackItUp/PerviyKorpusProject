package ru.vrazumi.core.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.vrazumi.core.card.CardOption;
import ru.vrazumi.core.common.Texts;

@Schema(description = "Вариант ответа")
public record OptionRequest(
    @Schema(example = "went", maxLength = CardOption.TEXT_MAX_LENGTH)
        @NotBlank(message = "Текст варианта обязателен")
        @Size(
            max = CardOption.TEXT_MAX_LENGTH,
            message = "Вариант должен быть не длиннее {max} символов")
        String text,
    @Schema(description = "Верный ли вариант, по умолчанию false", example = "true")
        Boolean correct) {

  public OptionRequest {
    text = Texts.trim(text);
    correct = Boolean.TRUE.equals(correct);
  }
}
