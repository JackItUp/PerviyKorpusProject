package ru.vrazumi.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import ru.vrazumi.core.auth.SecurityConfig;

/**
 * {@link WebMvcTest} с настоящей конфигурацией безопасности приложения. Без неё срез поднял бы
 * Spring Security по умолчанию (Basic-авторизация) и ответы 401/403 отличались бы от боевых.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import(SecurityConfig.class)
public @interface WebLayerTest {

  @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
  Class<?>[] value() default {};
}
