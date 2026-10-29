package ru.vrazumi.core.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.WebLayerTest;
import ru.vrazumi.core.auth.dto.LoginRequest;
import ru.vrazumi.core.auth.dto.TokenResponse;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.common.UnauthorizedException;
import ru.vrazumi.core.user.UserService;
import ru.vrazumi.core.user.dto.UserResponse;

@WebLayerTest(AuthController.class)
class AuthControllerTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private UserService userService;
  @MockitoBean private AuthService authService;

  @Test
  void loginReturnsTokens() throws Exception {
    when(authService.login(new LoginRequest("ivan", "s3cret-pass")))
        .thenReturn(new TokenResponse("access", "Bearer", 900, "refresh", 2_592_000));

    postJson("/api/auth/login", "{\"login\": \" ivan \", \"password\": \"s3cret-pass\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900))
        .andExpect(jsonPath("$.refreshToken").value("refresh"));
  }

  @Test
  void loginWithBadCredentialsReturns401() throws Exception {
    when(authService.login(any()))
        .thenThrow(new UnauthorizedException(AuthService.BAD_CREDENTIALS));

    postJson("/api/auth/login", "{\"login\": \"ivan\", \"password\": \"wrong-pass\"}")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Неверный логин или пароль"));
  }

  @Test
  void loginRequiresLoginAndPassword() throws Exception {
    postJson("/api/auth/login", "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2));
    verifyNoInteractions(authService);
  }

  @Test
  void refreshWithInvalidTokenReturns401() throws Exception {
    when(authService.refresh("stale"))
        .thenThrow(new UnauthorizedException(AuthService.BAD_REFRESH_TOKEN));

    postJson("/api/auth/refresh", "{\"refreshToken\": \"stale\"}")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void logoutReturns204WithoutAccessToken() throws Exception {
    postJson("/api/auth/logout", "{\"refreshToken\": \"refresh\"}")
        .andExpect(status().isNoContent());
    verify(authService).logout("refresh");
  }

  @Test
  void meWithoutTokenReturns401ProblemDetail() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.instance").value("/api/auth/me"));
    verifyNoInteractions(userService);
  }

  @Test
  void meWithInvalidTokenReturns401() throws Exception {
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(ProblemDetailSecurityHandler.TOKEN_INVALID));
  }

  @Test
  void meReturnsCurrentUserFromToken() throws Exception {
    when(userService.getProfile(7L))
        .thenReturn(new UserResponse(7L, "ivan", "ivan@example.com", Instant.now()));

    mvc.perform(get("/api/auth/me").with(jwt().jwt(token -> token.subject("7"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.username").value("ivan"));
  }

  @Test
  void registerReturns201WithoutPassword() throws Exception {
    when(userService.register(any()))
        .thenReturn(
            new UserResponse(
                1L, "ivan", "ivan@example.com", Instant.parse("2026-10-13T09:30:00Z")));

    register("ivan", "ivan@example.com", "s3cret-pass")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.username").value("ivan"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "ab", "иван", "ivan petrov", "ivan@x"})
  void registerRejectsInvalidUsername(String username) throws Exception {
    expectFieldError(register(username, "ivan@example.com", "s3cret-pass"), "username");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "ivan", "ivan@", "ivan@example", "@example.com", "iv an@ex.com"})
  void registerRejectsInvalidEmail(String email) throws Exception {
    expectFieldError(register("ivan", email, "s3cret-pass"), "email");
  }

  @Test
  void registerRejectsShortPassword() throws Exception {
    expectFieldError(register("ivan", "ivan@example.com", "1234567"), "password");
  }

  @Test
  void registerRejectsPasswordLongerThan72Bytes() throws Exception {
    // 37 кириллических символов = 74 байта
    expectFieldError(register("ivan", "ivan@example.com", "ж".repeat(37)), "password");
  }

  @Test
  void registerAccepts72BytePassword() throws Exception {
    when(userService.register(any()))
        .thenReturn(new UserResponse(1L, "ivan", "ivan@example.com", Instant.now()));

    register("ivan", "ivan@example.com", "ж".repeat(36)).andExpect(status().isCreated());
  }

  @Test
  void registerRejectsEmptyBody() throws Exception {
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(3));
    verifyNoInteractions(userService);
  }

  @Test
  void registerReturns409ForTakenUsername() throws Exception {
    when(userService.register(any()))
        .thenThrow(new ConflictException("username", "Имя пользователя уже занято"));

    register("ivan", "ivan@example.com", "s3cret-pass")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.errors[0].field").value("username"))
        .andExpect(jsonPath("$.errors[0].message").value("Имя пользователя уже занято"));
  }

  private ResultActions register(String username, String email, String password) throws Exception {
    return mvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}"
                    .formatted(username, email, password)));
  }

  private ResultActions postJson(String uri, String json) throws Exception {
    return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private void expectFieldError(ResultActions result, String field) throws Exception {
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == '%s')]".formatted(field)).exists());
    verifyNoInteractions(userService);
  }
}
