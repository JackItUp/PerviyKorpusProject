package ru.vrazumi.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ответы 401 и 403 от фильтров безопасности в формате ProblemDetail. Они возникают до контроллеров,
 * поэтому {@code GlobalExceptionHandler} их не видит.
 */
@RequiredArgsConstructor
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  static final String TOKEN_REQUIRED = "Требуется авторизация: передайте access-токен.";
  static final String TOKEN_EXPIRED = "Срок действия access-токена истёк. Обновите его.";
  static final String TOKEN_INVALID = "Access-токен недействителен.";

  private final JsonMapper jsonMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    write(request, response, HttpStatus.UNAUTHORIZED, "Не авторизован", detail(authException));
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(request, response, HttpStatus.FORBIDDEN, "Доступ запрещён", "Недостаточно прав.");
  }

  private static String detail(AuthenticationException ex) {
    if (ex instanceof InvalidBearerTokenException) {
      String message = ex.getMessage();
      return message != null && message.toLowerCase(Locale.ROOT).contains("expired")
          ? TOKEN_EXPIRED
          : TOKEN_INVALID;
    }
    return TOKEN_REQUIRED;
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String title,
      String detail)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", title);
    body.put("status", status.value());
    body.put("detail", detail);
    body.put("instance", request.getRequestURI());
    response.setStatus(status.value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    jsonMapper.writeValue(response.getOutputStream(), body);
  }
}
