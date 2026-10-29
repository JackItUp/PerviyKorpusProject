package ru.vrazumi.core.card;

/** Результат автоматической проверки ответа. */
public enum AnswerResult {
  /** Ответ полностью верный. */
  CORRECT,
  /** Выбран хотя бы один верный вариант, но набор не совпал с верным. */
  PARTIAL,
  /** Ответ неверный. */
  INCORRECT
}
