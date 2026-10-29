package ru.vrazumi.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ru.vrazumi.core.common.Texts;

@Schema(description = "Вход по имени пользователя или email")
public record LoginRequest(
    @Schema(
            description = "Имя пользователя или email",
            example = "ivan_petrov",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Укажите имя пользователя или email")
        String login,
    @Schema(example = "s3cret-pass", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Пароль обязателен")
        String password) {

  public LoginRequest {
    login = Texts.trim(login);
  }

  /** Пароль не попадает в логи и toString. */
  @Override
  public String toString() {
    return "LoginRequest[login=%s]".formatted(login);
  }
}
