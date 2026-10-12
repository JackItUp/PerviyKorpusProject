package ru.vrazumi.core.common;

/** Запрошенный ресурс не существует. Обработчик ошибок превращает его в 404. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
