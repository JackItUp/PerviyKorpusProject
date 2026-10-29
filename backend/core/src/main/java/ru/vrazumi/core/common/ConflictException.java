package ru.vrazumi.core.common;

import lombok.Getter;

/** Запрос конфликтует с уже существующими данными (например, занятое имя). Превращается в 409. */
@Getter
public class ConflictException extends RuntimeException {

  /** Поле запроса, к которому относится конфликт. */
  private final String field;

  public ConflictException(String field, String message) {
    super(message);
    this.field = field;
  }
}
