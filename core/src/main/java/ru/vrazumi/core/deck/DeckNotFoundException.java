package ru.vrazumi.core.deck;

import ru.vrazumi.core.common.NotFoundException;

public class DeckNotFoundException extends NotFoundException {

  public DeckNotFoundException(Long deckId) {
    super("Колода с id=%d не найдена".formatted(deckId));
  }
}
