package ru.vrazumi.core.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки токенов.
 *
 * @param secret ключ подписи HS256, не короче 32 байт (переменная окружения {@code JWT_SECRET})
 * @param accessTtl срок жизни access-токена
 * @param refreshTtl срок жизни refresh-токена
 * @param issuer значение claim {@code iss}
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl, String issuer) {

  static final int MIN_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "app.jwt.secret (JWT_SECRET) должен быть не короче %d байт".formatted(MIN_SECRET_BYTES));
    }
    accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
    refreshTtl = refreshTtl == null ? Duration.ofDays(30) : refreshTtl;
    issuer = issuer == null ? "vrazumi-core" : issuer;
  }

  /** Секрет не попадает в логи и toString. */
  @Override
  public String toString() {
    return "JwtProperties[accessTtl=%s, refreshTtl=%s, issuer=%s]"
        .formatted(accessTtl, refreshTtl, issuer);
  }
}
