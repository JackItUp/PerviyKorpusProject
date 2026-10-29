package ru.vrazumi.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.vrazumi.core.auth.dto.LoginRequest;
import ru.vrazumi.core.auth.dto.TokenResponse;
import ru.vrazumi.core.common.UnauthorizedException;
import ru.vrazumi.core.user.User;
import ru.vrazumi.core.user.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
  private static final PasswordEncoder ENCODER =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();
  private static final String HASH = ENCODER.encode("s3cret-pass");

  @Mock private UserService userService;
  @Mock private TokenService tokenService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    JwtProperties properties =
        new JwtProperties(
            "test-only-secret-key-for-jwt-signing-0123456789",
            Duration.ofMinutes(15),
            Duration.ofDays(30),
            "vrazumi-core");
    authService =
        new AuthService(
            userService,
            tokenService,
            refreshTokenRepository,
            ENCODER,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void loginIssuesAccessAndRefreshTokens() {
    User user = user();
    when(userService.findByLogin("ivan")).thenReturn(Optional.of(user));
    when(tokenService.issueAccessToken(user, NOW)).thenReturn("access-jwt");
    when(tokenService.generateRefreshToken()).thenReturn("refresh-value");
    when(tokenService.hash("refresh-value")).thenReturn("refresh-hash");

    TokenResponse tokens = authService.login(new LoginRequest("ivan", "s3cret-pass"));

    assertThat(tokens.accessToken()).isEqualTo("access-jwt");
    assertThat(tokens.tokenType()).isEqualTo("Bearer");
    assertThat(tokens.expiresIn()).isEqualTo(900);
    assertThat(tokens.refreshToken()).isEqualTo("refresh-value");
    ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(saved.capture());
    assertThat(saved.getValue().getTokenHash()).isEqualTo("refresh-hash");
    assertThat(saved.getValue().getUser()).isSameAs(user);
    assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
  }

  @Test
  void loginWithWrongPasswordFails() {
    when(userService.findByLogin("ivan")).thenReturn(Optional.of(user()));

    assertThatThrownBy(() -> authService.login(new LoginRequest("ivan", "wrong-pass")))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage(AuthService.BAD_CREDENTIALS);
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void loginWithUnknownUserFailsWithSameMessage() {
    when(userService.findByLogin("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "s3cret-pass")))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage(AuthService.BAD_CREDENTIALS);
  }

  @Test
  void refreshRotatesToken() {
    RefreshToken stored = refreshToken(NOW.plusSeconds(60), null);
    when(tokenService.hash("old")).thenReturn("old-hash");
    when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(stored));
    when(tokenService.generateRefreshToken()).thenReturn("new");
    when(tokenService.hash("new")).thenReturn("new-hash");
    when(tokenService.issueAccessToken(stored.getUser(), NOW)).thenReturn("access-jwt");

    TokenResponse tokens = authService.refresh("old");

    assertThat(stored.getRevokedAt()).isEqualTo(NOW);
    assertThat(tokens.refreshToken()).isEqualTo("new");
    assertThat(tokens.accessToken()).isEqualTo("access-jwt");
  }

  @Test
  void refreshWithExpiredTokenFails() {
    when(tokenService.hash("old")).thenReturn("old-hash");
    when(refreshTokenRepository.findByTokenHash("old-hash"))
        .thenReturn(Optional.of(refreshToken(NOW.minusSeconds(1), null)));

    assertThatThrownBy(() -> authService.refresh("old")).isInstanceOf(UnauthorizedException.class);
    verify(tokenService, never()).issueAccessToken(any(), any());
  }

  @Test
  void refreshWithUnknownTokenFails() {
    when(tokenService.hash(anyString())).thenReturn("unknown-hash");
    when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("unknown"))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void reusedRevokedTokenRevokesAllSessions() {
    when(tokenService.hash("old")).thenReturn("old-hash");
    when(refreshTokenRepository.findByTokenHash("old-hash"))
        .thenReturn(Optional.of(refreshToken(NOW.plusSeconds(60), NOW.minusSeconds(5))));

    assertThatThrownBy(() -> authService.refresh("old")).isInstanceOf(UnauthorizedException.class);
    verify(refreshTokenRepository).revokeAllActive(eq(1L), eq(NOW));
  }

  @Test
  void logoutRevokesToken() {
    RefreshToken stored = refreshToken(NOW.plusSeconds(60), null);
    when(tokenService.hash("value")).thenReturn("hash");
    when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

    authService.logout("value");

    assertThat(stored.getRevokedAt()).isEqualTo(NOW);
  }

  private static User user() {
    User user = new User();
    user.setId(1L);
    user.setUsername("ivan");
    user.setEmail("ivan@example.com");
    user.setPasswordHash(HASH);
    return user;
  }

  private static RefreshToken refreshToken(Instant expiresAt, Instant revokedAt) {
    RefreshToken token = new RefreshToken();
    token.setUser(user());
    token.setTokenHash("old-hash");
    token.setCreatedAt(NOW.minusSeconds(3600));
    token.setExpiresAt(expiresAt);
    token.setRevokedAt(revokedAt);
    return token;
  }
}
