package ru.vrazumi.core.card;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

  /** Карточки колоды в порядке добавления. */
  List<Card> findByDeckIdOrderByIdAsc(Long deckId);
}
