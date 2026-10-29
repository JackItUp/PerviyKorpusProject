package ru.vrazumi.core.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Пара токенов")
public record TokenResponse(
    @Schema(description = "JWT для заголовка Authorization: Bearer …") String accessToken,
    @Schema(example = "Bearer") String tokenType,
    @Schema(description = "Срок жизни access-токена, секунды", example = "900") long expiresIn,
    @Schema(description = "Одноразовый токен для POST /api/auth/refresh") String refreshToken,
    @Schema(description = "Срок жизни refresh-токена, секунды", example = "2592000")
        long refreshExpiresIn) {

  @Override
  public String toString() {
    return "TokenResponse[tokenType=%s, expiresIn=%d]".formatted(tokenType, expiresIn);
  }
}
