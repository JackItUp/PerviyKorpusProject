package ru.vrazumi.core.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.vrazumi.core.user.User;

/** Выпуск access-токенов (JWT) и генерация непрозрачных refresh-токенов. */
@Service
@RequiredArgsConstructor
public class TokenService {

  public static final String USERNAME_CLAIM = "username";

  private static final int REFRESH_TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  public String issueAccessToken(User user, Instant issuedAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(String.valueOf(user.getId()))
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(properties.accessTtl()))
            .claim(USERNAME_CLAIM, user.getUsername())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  /** Случайные 256 бит в Base64URL. */
  public String generateRefreshToken() {
    byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** SHA-256 в hex: так refresh-токен хранится в БД. */
  public String hash(String refreshToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 недоступен", ex);
    }
  }
}
