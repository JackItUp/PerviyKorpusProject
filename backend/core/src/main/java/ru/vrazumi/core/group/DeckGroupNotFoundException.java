package ru.vrazumi.core.group;

import ru.vrazumi.core.common.NotFoundException;

public class DeckGroupNotFoundException extends NotFoundException {

  public DeckGroupNotFoundException(Long groupId) {
    super("Группа с id=%d не найдена".formatted(groupId));
  }
}
