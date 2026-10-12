package ru.vrazumi.core.common;

/** Нормализация пользовательского текста перед валидацией и сохранением. */
public final class Texts {

  private Texts() {}

  /** Обрезает пробелы по краям; {@code null} остаётся {@code null}. */
  public static String trim(String value) {
    return value == null ? null : value.strip();
  }

  /** Обрезает пробелы по краям; пустая строка превращается в {@code null}. */
  public static String trimToNull(String value) {
    String trimmed = trim(value);
    return trimmed == null || trimmed.isEmpty() ? null : trimmed;
  }
}
