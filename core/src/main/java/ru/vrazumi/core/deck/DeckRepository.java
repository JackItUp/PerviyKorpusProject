package ru.vrazumi.core.deck;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, Long> {

  List<Deck> findAllByOrderByCreatedAtDescIdDesc();
}
