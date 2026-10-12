package ru.vrazumi.core.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI vrazumiOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Вразуми API")
                .version("v1")
                .description(
                    "Ядро «Вразуми»: колоды, карточки, режим изучения и оценки ответов. "
                        + "Ошибки возвращаются в формате ProblemDetail (RFC 9457)."));
  }
}
