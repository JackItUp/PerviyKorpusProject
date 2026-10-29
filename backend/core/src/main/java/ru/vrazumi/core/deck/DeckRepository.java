package ru.vrazumi.core.deck;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Все запросы ограничены владельцем: чужие колоды для пользователя не существуют. */
public interface DeckRepository extends JpaRepository<Deck, Long> {

  @Query(
      """
      select new ru.vrazumi.core.deck.DeckWithCardCount(d, count(c))
      from Deck d
      left join Card c on c.deck = d
      where d.owner.id = :ownerId
      group by d
      order by d.createdAt desc, d.id desc
      """)
  List<DeckWithCardCount> findAllWithCardCount(Long ownerId);

  @Query(
      """
      select new ru.vrazumi.core.deck.DeckWithCardCount(d, count(c))
      from Deck d
      left join Card c on c.deck = d
      where d.id = :deckId and d.owner.id = :ownerId
      group by d
      """)
  Optional<DeckWithCardCount> findWithCardCountById(Long deckId, Long ownerId);

  Optional<Deck> findByIdAndOwnerId(Long deckId, Long ownerId);

  boolean existsByIdAndOwnerId(Long deckId, Long ownerId);
}
