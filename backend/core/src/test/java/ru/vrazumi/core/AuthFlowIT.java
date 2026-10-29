package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.vrazumi.core.auth.TokenService;
import ru.vrazumi.core.user.User;

class AuthFlowIT extends AbstractIntegrationTest {

  @Autowired private TokenService tokenService;

  @Test
  void loginByUsernameOrEmailThenMe() throws Exception {
    register("Ivan", "ivan@example.com");

    String byUsername = accessToken(login("ivan", "s3cret-pass"));
    String byEmail = accessToken(login("IVAN@example.com", "s3cret-pass"));

    for (String token : new String[] {byUsername, byEmail}) {
      mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("Ivan"))
          .andExpect(jsonPath("$.email").value("ivan@example.com"));
    }
  }

  @Test
  void wrongPasswordAndUnknownUserGiveSameAnswer() throws Exception {
    register("ivan", "ivan@example.com");

    login("ivan", "wrong-pass")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Неверный логин или пароль"));
    login("ghost", "s3cret-pass")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Неверный логин или пароль"));
  }

  @Test
  void refreshRotatesAndLogoutRevokes() throws Exception {
    register("ivan", "ivan@example.com");
    String firstRefresh = refreshToken(login("ivan", "s3cret-pass").andExpect(status().isOk()));

    ResultActions refreshed = refresh(firstRefresh).andExpect(status().isOk());
    String secondRefresh = refreshToken(refreshed);
    assertThat(secondRefresh).isNotEqualTo(firstRefresh);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken(refreshed)))
        .andExpect(status().isOk());

    // Старый refresh-токен одноразовый. Повторное предъявление отзывает и новый
    refresh(firstRefresh).andExpect(status().isUnauthorized());
    refresh(secondRefresh).andExpect(status().isUnauthorized());

    String thirdRefresh = refreshToken(login("ivan", "s3cret-pass"));
    postJson("/api/auth/logout", "{\"refreshToken\": \"%s\"}".formatted(thirdRefresh))
        .andExpect(status().isNoContent());
    refresh(thirdRefresh).andExpect(status().isUnauthorized());

    assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_token", Long.class)).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM refresh_token WHERE revoked_at IS NULL", Long.class))
        .isZero();
  }

  @Test
  void expiredAccessTokenIsRejectedWithClearMessage() throws Exception {
    register("ivan", "ivan@example.com");
    User user = new User();
    user.setId(jdbc.queryForObject("SELECT id FROM app_user", Long.class));
    user.setUsername("ivan");
    String expired = tokenService.issueAccessToken(user, Instant.now().minus(Duration.ofHours(1)));

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Срок действия access-токена истёк. Обновите его."));
  }

  @Test
  void meWithoutTokenIsUnauthorized() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  private void register(String username, String email) throws Exception {
    postJson(
            "/api/auth/register",
            "{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"s3cret-pass\"}"
                .formatted(username, email))
        .andExpect(status().isCreated());
  }

  private ResultActions login(String login, String password) throws Exception {
    return postJson(
        "/api/auth/login", "{\"login\": \"%s\", \"password\": \"%s\"}".formatted(login, password));
  }

  private ResultActions refresh(String refreshToken) throws Exception {
    return postJson("/api/auth/refresh", "{\"refreshToken\": \"%s\"}".formatted(refreshToken));
  }

  private ResultActions postJson(String uri, String json) throws Exception {
    return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static String accessToken(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
  }

  private static String refreshToken(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.refreshToken");
  }
}
