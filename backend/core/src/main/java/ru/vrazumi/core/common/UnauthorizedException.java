package ru.vrazumi.core.common;

/** Неверные учётные данные или недействительный токен. Обработчик ошибок превращает его в 401. */
public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException(String message) {
    super(message);
  }
}
