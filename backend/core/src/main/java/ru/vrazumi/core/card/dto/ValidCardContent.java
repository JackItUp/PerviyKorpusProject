package ru.vrazumi.core.card.dto;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Содержимое карточки соответствует её типу. Ошибки привязываются к полям ({@code answer}, {@code
 * options}), поэтому формат ответа 400 тот же, что у обычной валидации.
 */
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidCardContent.Validator.class)
public @interface ValidCardContent {

  int MIN_OPTIONS = 2;
  int MAX_OPTIONS = 10;

  String message() default "Содержимое карточки не соответствует её типу";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<ValidCardContent, CreateCardRequest> {

    @Override
    public boolean isValid(CreateCardRequest request, ConstraintValidatorContext context) {
      if (request == null) {
        return true;
      }
      context.disableDefaultConstraintViolation();
      return switch (request.type()) {
        case NORMAL -> validateNormal(request, context);
        case MULTIPLE_CHOICE -> validateChoice(request, context);
      };
    }

    private static boolean validateNormal(
        CreateCardRequest request, ConstraintValidatorContext context) {
      boolean valid = true;
      if (request.answer() == null) {
        valid = violation(context, "answer", "Ответ обязателен");
      }
      if (!request.options().isEmpty()) {
        valid = violation(context, "options", "У обычной карточки нет вариантов ответа");
      }
      return valid;
    }

    private static boolean validateChoice(
        CreateCardRequest request, ConstraintValidatorContext context) {
      boolean valid = true;
      if (request.answer() != null) {
        valid =
            violation(
                context,
                "answer",
                "У карточки с вариантами ответ задаётся вариантами, а не answer");
      }
      List<OptionRequest> options = request.options().stream().filter(Objects::nonNull).toList();
      if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
        return violation(
            context,
            "options",
            "Нужно от %d до %d вариантов ответа".formatted(MIN_OPTIONS, MAX_OPTIONS));
      }
      if (options.stream().noneMatch(OptionRequest::correct)) {
        valid = violation(context, "options", "Отметьте хотя бы один верный вариант");
      }
      Set<String> seen = new HashSet<>();
      boolean duplicates =
          options.stream()
              .map(OptionRequest::text)
              .filter(Objects::nonNull)
              .anyMatch(text -> !seen.add(text.toLowerCase(Locale.ROOT)));
      if (duplicates) {
        valid = violation(context, "options", "Варианты ответа не должны повторяться");
      }
      return valid;
    }

    private static boolean violation(
        ConstraintValidatorContext context, String field, String message) {
      context
          .buildConstraintViolationWithTemplate(message)
          .addPropertyNode(field)
          .addConstraintViolation();
      return false;
    }
  }
}
