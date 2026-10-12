package ru.vrazumi.core.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.common.Texts;

/**
 * Запрос на создание карточки. Тип карточки клиент не передаёт: пока это всегда NORMAL. Строки
 * обрезаются по краям до валидации.
 */
@Schema(description = "Новая карточка «вопрос — ответ»")
public record CreateCardRequest(
    @Schema(
            description = "Вопрос",
            example = "go",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = Card.QUESTION_MAX_LENGTH)
        @NotBlank(message = "Вопрос обязателен")
        @Size(
            max = Card.QUESTION_MAX_LENGTH,
            message = "Вопрос должен быть не длиннее {max} символов")
        String question,
    @Schema(
            description = "Ответ",
            example = "went, gone",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = Card.ANSWER_MAX_LENGTH)
        @NotBlank(message = "Ответ обязателен")
        @Size(max = Card.ANSWER_MAX_LENGTH, message = "Ответ должен быть не длиннее {max} символов")
        String answer) {

  public CreateCardRequest {
    question = Texts.trim(question);
    answer = Texts.trim(answer);
  }
}
