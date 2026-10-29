package ru.vrazumi.core.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import ru.vrazumi.core.card.AnswerResult;

@Schema(description = "Результат проверки ответа и записанная оценка")
public record AnswerResultResponse(
    @Schema(example = "PARTIAL") AnswerResult result,
    @Schema(description = "Для MULTIPLE_CHOICE: id верных вариантов", example = "[31, 32]")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<Long> correctOptionIds,
    @Schema(description = "Для TEXT_INPUT: правильный ответ", example = "went")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String correctAnswer,
    @Schema(
            description =
                "Оценка, записанная в историю: CORRECT → KNOW, PARTIAL → HARD,"
                    + " INCORRECT → DONT_KNOW")
        ReviewResponse review) {}
