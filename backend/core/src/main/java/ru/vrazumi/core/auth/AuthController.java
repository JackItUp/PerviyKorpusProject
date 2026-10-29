package ru.vrazumi.core.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.vrazumi.core.auth.dto.LoginRequest;
import ru.vrazumi.core.auth.dto.RefreshRequest;
import ru.vrazumi.core.auth.dto.TokenResponse;
import ru.vrazumi.core.user.UserService;
import ru.vrazumi.core.user.dto.RegisterRequest;
import ru.vrazumi.core.user.dto.UserResponse;

@Tag(name = "Авторизация")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final AuthService authService;

  @Operation(summary = "Регистрация")
  @SecurityRequirements
  @ApiResponse(responseCode = "201", description = "Пользователь создан")
  @ApiResponse(
      responseCode = "400",
      description = "Ошибка валидации",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Имя пользователя или email уже заняты",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return userService.register(request);
  }

  @Operation(summary = "Вход", description = "Логин — имя пользователя или email.")
  @SecurityRequirements
  @ApiResponse(responseCode = "200", description = "Пара токенов")
  @ApiResponse(
      responseCode = "401",
      description = "Неверный логин или пароль",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @Operation(
      summary = "Обновить токены",
      description = "Refresh-токен одноразовый: в ответе новая пара, старый токен отзывается.")
  @SecurityRequirements
  @ApiResponse(responseCode = "200", description = "Новая пара токенов")
  @ApiResponse(
      responseCode = "401",
      description = "Refresh-токен недействителен, отозван или истёк",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request.refreshToken());
  }

  @Operation(
      summary = "Выход",
      description = "Отзывает refresh-токен. Access-токен клиент просто удаляет.")
  @SecurityRequirements
  @ApiResponse(responseCode = "204", description = "Готово")
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody RefreshRequest request) {
    authService.logout(request.refreshToken());
  }

  @Operation(summary = "Текущий пользователь")
  @ApiResponse(responseCode = "200", description = "Профиль")
  @ApiResponse(
      responseCode = "401",
      description = "Нет токена или он недействителен",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/me")
  public UserResponse me(@CurrentUserId Long userId) {
    return userService.getProfile(userId);
  }
}
