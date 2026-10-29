package ru.vrazumi.core.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.user.UserService;
import ru.vrazumi.core.user.dto.UserResponse;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private UserService userService;

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

  private void expectFieldError(ResultActions result, String field) throws Exception {
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == '%s')]".formatted(field)).exists());
    verifyNoInteractions(userService);
  }
}
