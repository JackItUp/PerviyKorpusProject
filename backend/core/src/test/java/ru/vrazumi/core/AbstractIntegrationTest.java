package ru.vrazumi.core;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Общая база интеграционных тестов: полный контекст и PostgreSQL в Testcontainers. Контекст (и
 * контейнер) переиспользуется всеми наследниками, данные очищаются перед каждым тестом.
 *
 * <p>Запросы через {@link #mvc} по умолчанию анонимные. {@link #actAs(String)} регистрирует
 * пользователя, входит настоящим логином и дальше подставляет его access-токен во все запросы
 * {@link #mvc}; {@link #anonymous} остаётся без токена.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

  protected static final String PASSWORD = "s3cret-pass";

  @Autowired protected MockMvc mvc;
  @Autowired protected MockMvc anonymous;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired private WebApplicationContext context;

  @BeforeEach
  void cleanDatabase() {
    jdbc.execute(
        "TRUNCATE TABLE review, card, deck, refresh_token, app_user RESTART IDENTITY CASCADE");
  }

  /** Регистрирует пользователя и возвращает значение заголовка {@code Authorization}. */
  protected String bearerOf(String username) throws Exception {
    anonymous
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\": \"%s\", \"email\": \"%s@example.com\", \"password\": \"%s\"}"
                        .formatted(username, username, PASSWORD)))
        .andExpect(status().isCreated());
    String tokens =
        anonymous
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"login\": \"%s\", \"password\": \"%s\"}".formatted(username, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(tokens, "$.accessToken");
  }

  /** Дальше все запросы {@link #mvc} идут от этого пользователя. */
  protected String actAs(String username) throws Exception {
    String bearer = bearerOf(username);
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, bearer))
            .build();
    return bearer;
  }
}
