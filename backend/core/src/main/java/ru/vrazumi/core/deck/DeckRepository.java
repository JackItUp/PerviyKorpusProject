package ru.vrazumi.core.deck;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeckRepository extends JpaRepository<Deck, Long> {

  @Query(
      """
      select new ru.vrazumi.core.deck.DeckWithCardCount(d, count(c))
      from Deck d
      left join Card c on c.deck = d
      group by d
      order by d.createdAt desc, d.id desc
      """)
  List<DeckWithCardCount> findAllWithCardCount();

  @Query(
      """
      select new ru.vrazumi.core.deck.DeckWithCardCount(d, count(c))
      from Deck d
      left join Card c on c.deck = d
      where d.id = :deckId
      group by d
      """)
  Optional<DeckWithCardCount> findWithCardCountById(Long deckId);
}
