package ru.vrazumi.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh-токен, полученный при входе или прошлом обновлении")
public record RefreshRequest(
    @Schema(
            example = "c2VjcmV0LXJlZnJlc2gtdG9rZW4tdmFsdWUtMzItYnl0ZXM",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Refresh-токен обязателен")
        String refreshToken) {

  @Override
  public String toString() {
    return "RefreshRequest[***]";
  }
}
