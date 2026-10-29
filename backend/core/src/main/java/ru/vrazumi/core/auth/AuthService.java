package ru.vrazumi.core.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.auth.dto.LoginRequest;
import ru.vrazumi.core.auth.dto.TokenResponse;
import ru.vrazumi.core.common.UnauthorizedException;
import ru.vrazumi.core.user.User;
import ru.vrazumi.core.user.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  static final String BAD_CREDENTIALS = "Неверный логин или пароль";
  static final String BAD_REFRESH_TOKEN = "Refresh-токен недействителен или истёк. Войдите заново.";

  private final UserService userService;
  private final TokenService tokenService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProperties properties;
  private final Clock clock;

  /**
   * Хеш для проверки пароля, когда пользователь не найден: время ответа не выдаёт, существует ли
   * логин.
   */
  private String dummyHash;

  @Transactional
  public TokenResponse login(LoginRequest request) {
    Optional<User> user = userService.findByLogin(request.login());
    String hash = user.map(User::getPasswordHash).orElseGet(this::dummyHash);
    boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
    if (user.isEmpty() || !passwordMatches) {
      throw new UnauthorizedException(BAD_CREDENTIALS);
    }
    return issueTokens(user.get(), now());
  }

  /** Обменивает refresh-токен на новую пару. Старый токен отзывается (ротация). */
  @Transactional(noRollbackFor = UnauthorizedException.class)
  public TokenResponse refresh(String refreshToken) {
    Instant now = now();
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(tokenService.hash(refreshToken))
            .orElseThrow(() -> new UnauthorizedException(BAD_REFRESH_TOKEN));

    if (stored.getRevokedAt() != null) {
      // Отозванный токен предъявили повторно: возможно, его украли. Отзываем все сессии.
      log.warn("Повторное использование отозванного refresh-токена, userId={}", userId(stored));
      refreshTokenRepository.revokeAllActive(userId(stored), now);
      throw new UnauthorizedException(BAD_REFRESH_TOKEN);
    }
    if (!stored.isActive(now)) {
      throw new UnauthorizedException(BAD_REFRESH_TOKEN);
    }

    stored.setRevokedAt(now);
    return issueTokens(stored.getUser(), now);
  }

  /** Отзывает refresh-токен. Неизвестный или уже отозванный токен не считается ошибкой. */
  @Transactional
  public void logout(String refreshToken) {
    refreshTokenRepository
        .findByTokenHash(tokenService.hash(refreshToken))
        .filter(token -> token.getRevokedAt() == null)
        .ifPresent(token -> token.setRevokedAt(now()));
  }

  private TokenResponse issueTokens(User user, Instant now) {
    String refreshValue = tokenService.generateRefreshToken();
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setTokenHash(tokenService.hash(refreshValue));
    refreshToken.setCreatedAt(now);
    refreshToken.setExpiresAt(now.plus(properties.refreshTtl()));
    refreshTokenRepository.save(refreshToken);

    return new TokenResponse(
        tokenService.issueAccessToken(user, now),
        "Bearer",
        properties.accessTtl().toSeconds(),
        refreshValue,
        properties.refreshTtl().toSeconds());
  }

  private Instant now() {
    return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
  }

  private static Long userId(RefreshToken token) {
    return token.getUser().getId();
  }

  private String dummyHash() {
    if (dummyHash == null) {
      dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }
    return dummyHash;
  }
}
