package ru.vrazumi.core.card;

/** Тип карточки. */
public enum CardType {
  /** Обычная карточка «вопрос — ответ» с самооценкой. */
  NORMAL,
  /** Варианты ответа, верных может быть несколько. Ответ проверяет сервер. */
  MULTIPLE_CHOICE
}
