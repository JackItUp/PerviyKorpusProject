package ru.vrazumi.core.auth;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** id текущего пользователя из access-токена (claim {@code sub}) для параметра контроллера. */
@Target(PARAMETER)
@Retention(RUNTIME)
@Parameter(hidden = true)
public @interface CurrentUserId {}
