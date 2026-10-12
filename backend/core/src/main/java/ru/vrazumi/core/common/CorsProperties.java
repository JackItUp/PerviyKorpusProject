package ru.vrazumi.core.common;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки CORS для {@code /api/**}.
 *
 * @param allowedOrigins разрешённые origin фронтенда, например {@code http://localhost:5173}
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
  }
}
