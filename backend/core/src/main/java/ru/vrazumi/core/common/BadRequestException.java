package ru.vrazumi.core.common;

import lombok.Getter;

/** Запрос корректен по формату, но не подходит по смыслу. Превращается в 400. */
@Getter
public class BadRequestException extends RuntimeException {

  /** Поле запроса, к которому относится ошибка, или {@code null}, если ошибка общая. */
  private final String field;

  public BadRequestException(String field, String message) {
    super(message);
    this.field = field;
  }
}
