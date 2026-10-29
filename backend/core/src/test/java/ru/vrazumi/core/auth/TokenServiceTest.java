package ru.vrazumi.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import ru.vrazumi.core.user.User;

class TokenServiceTest {

  private final JwtProperties properties =
      new JwtProperties(
          "test-only-secret-key-for-jwt-signing-0123456789",
          Duration.ofMinutes(15),
          Duration.ofDays(30),
          "vrazumi-core");
  private final SecurityConfig securityConfig = new SecurityConfig();
  private final TokenService tokenService =
      new TokenService(securityConfig.jwtEncoder(properties), properties);
  private final JwtDecoder decoder = securityConfig.jwtDecoder(properties);

  @Test
  void accessTokenCarriesUserIdAndUsername() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    Jwt jwt = decoder.decode(tokenService.issueAccessToken(user(), now));

    assertThat(jwt.getSubject()).isEqualTo("42");
    assertThat(jwt.getClaimAsString(TokenService.USERNAME_CLAIM)).isEqualTo("ivan");
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("vrazumi-core");
    assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
  }

  @Test
  void expiredAccessTokenIsRejected() {
    String token = tokenService.issueAccessToken(user(), Instant.now().minus(Duration.ofHours(1)));

    assertThatThrownBy(() -> decoder.decode(token))
        .isInstanceOf(JwtValidationException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void tokenSignedWithAnotherSecretIsRejected() {
    JwtProperties other =
        new JwtProperties("another-secret-key-for-jwt-signing-9876543210", null, null, null);
    String token =
        new TokenService(securityConfig.jwtEncoder(other), other)
            .issueAccessToken(user(), Instant.now());

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(Exception.class);
  }

  @Test
  void refreshTokensAreRandomAndHashedDeterministically() {
    String first = tokenService.generateRefreshToken();
    String second = tokenService.generateRefreshToken();

    assertThat(first).isNotEqualTo(second).hasSize(43);
    assertThat(tokenService.hash(first)).hasSize(64).isEqualTo(tokenService.hash(first));
  }

  @Test
  void shortSecretIsRejectedAtStartup() {
    assertThatThrownBy(() -> new JwtProperties("too-short", null, null, null))
        .isInstanceOf(IllegalStateException.class);
  }

  private static User user() {
    User user = new User();
    user.setId(42L);
    user.setUsername("ivan");
    return user;
  }
}
