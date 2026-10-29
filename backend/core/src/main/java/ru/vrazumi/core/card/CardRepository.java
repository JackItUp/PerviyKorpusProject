package ru.vrazumi.core.card;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Варианты ответа загружаются вместе с карточками одним запросом, без N+1. */
public interface CardRepository extends JpaRepository<Card, Long> {

  /** Карточки колоды в порядке добавления. */
  @EntityGraph(attributePaths = "options")
  List<Card> findByDeckIdOrderByIdAsc(Long deckId);

  /** Карточка, если её колода принадлежит пользователю. */
  @EntityGraph(attributePaths = "options")
  Optional<Card> findByIdAndDeckOwnerId(Long cardId, Long ownerId);
}
