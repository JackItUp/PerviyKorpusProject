package ru.vrazumi.core.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  /** Имя схемы безопасности: кнопка Authorize в Swagger UI. */
  public static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI vrazumiOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Вразуми API")
                .version("v1")
                .description(
                    "Ядро «Вразуми»: аккаунты, колоды, карточки, режим изучения и оценки"
                        + " ответов. Доступ по access-токену из POST /api/auth/login."
                        + " Ошибки возвращаются в формате ProblemDetail (RFC 9457)."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
