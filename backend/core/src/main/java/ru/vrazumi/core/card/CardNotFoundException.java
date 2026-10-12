package ru.vrazumi.core.card;

import ru.vrazumi.core.common.NotFoundException;

public class CardNotFoundException extends NotFoundException {

  public CardNotFoundException(Long cardId) {
    super("Карточка с id=%d не найдена".formatted(cardId));
  }
}
