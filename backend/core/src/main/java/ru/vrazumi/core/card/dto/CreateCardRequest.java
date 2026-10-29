package ru.vrazumi.core.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardType;
import ru.vrazumi.core.common.Texts;

/**
 * Запрос на создание карточки. Строки обрезаются по краям до валидации. Какие поля нужны, зависит
 * от типа: это проверяет {@link ValidCardContent}.
 */
@Schema(description = "Новая карточка")
@ValidCardContent
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
            description = "Ответ. Обязателен для NORMAL, у MULTIPLE_CHOICE отсутствует",
            example = "went, gone",
            maxLength = Card.ANSWER_MAX_LENGTH)
        @Size(max = Card.ANSWER_MAX_LENGTH, message = "Ответ должен быть не длиннее {max} символов")
        String answer,
    @Schema(description = "Тип карточки, по умолчанию NORMAL", example = "NORMAL") CardType type,
    @Schema(description = "Варианты ответа, только для MULTIPLE_CHOICE")
        List<@NotNull(message = "Вариант не может быть null") @Valid OptionRequest> options) {

  public CreateCardRequest {
    question = Texts.trim(question);
    answer = Texts.trimToNull(answer);
    type = type == null ? CardType.NORMAL : type;
    options = options == null ? List.of() : options;
  }

  /** Обычная карточка «вопрос — ответ». */
  public CreateCardRequest(String question, String answer) {
    this(question, answer, CardType.NORMAL, List.of());
  }
}
