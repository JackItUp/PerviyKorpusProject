package ru.vrazumi.core;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * В тестах веб-слоя каждый запрос по умолчанию идёт от пользователя с id {@link #USER_ID}. Запрос
 * без токена: {@code .with(SecurityMockMvcRequestPostProcessors.anonymous())}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DefaultTestUserConfig {

  public static final long USER_ID = 1L;

  @Bean
  MockMvcBuilderCustomizer defaultTestUser() {
    return builder ->
        builder.defaultRequest(
            get("/").with(jwt().jwt(token -> token.subject(String.valueOf(USER_ID)))));
  }
}
