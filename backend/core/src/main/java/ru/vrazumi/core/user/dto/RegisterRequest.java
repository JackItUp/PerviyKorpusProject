package ru.vrazumi.core.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import ru.vrazumi.core.common.MaxUtf8Bytes;
import ru.vrazumi.core.common.Texts;
import ru.vrazumi.core.user.User;

/**
 * Запрос на регистрацию. Имя и email обрезаются по краям, email приводится к нижнему регистру.
 * Пароль не изменяется.
 */
@Schema(description = "Регистрация пользователя")
public record RegisterRequest(
    @Schema(
            example = "ivan_petrov",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = User.USERNAME_MIN_LENGTH,
            maxLength = User.USERNAME_MAX_LENGTH)
        @NotBlank(message = "Имя пользователя обязательно")
        @Size(
            min = User.USERNAME_MIN_LENGTH,
            max = User.USERNAME_MAX_LENGTH,
            message = "Имя пользователя должно быть от {min} до {max} символов")
        @Pattern(
            regexp = "^[A-Za-z0-9_.-]*$",
            message = "Имя пользователя может содержать только латинские буквы, цифры и _ . -")
        String username,
    @Schema(
            example = "ivan@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = User.EMAIL_MAX_LENGTH)
        @NotBlank(message = "Email обязателен")
        @Size(max = User.EMAIL_MAX_LENGTH, message = "Email должен быть не длиннее {max} символов")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Некорректный email")
        String email,
    @Schema(
            example = "s3cret-pass",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = User.PASSWORD_MIN_LENGTH)
        @NotBlank(message = "Пароль обязателен")
        @Size(
            min = User.PASSWORD_MIN_LENGTH,
            message = "Пароль должен быть не короче {min} символов")
        @MaxUtf8Bytes(
            value = User.PASSWORD_MAX_BYTES,
            message =
                "Пароль слишком длинный: не больше 72 латинских или 36 кириллических символов")
        String password) {

  public RegisterRequest {
    username = Texts.trim(username);
    email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
  }

  /** Пароль не попадает в логи и toString. */
  @Override
  public String toString() {
    return "RegisterRequest[username=%s, email=%s]".formatted(username, email);
  }
}
