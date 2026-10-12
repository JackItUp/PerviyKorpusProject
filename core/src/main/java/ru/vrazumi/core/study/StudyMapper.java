package ru.vrazumi.core.study;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.deck.Deck;
import ru.vrazumi.core.study.dto.StudyCardResponse;
import ru.vrazumi.core.study.dto.StudySessionResponse;

@Mapper
public interface StudyMapper {

  @Mapping(target = "deckId", source = "deck.id")
  @Mapping(target = "deckTitle", source = "deck.title")
  @Mapping(target = "cards", source = "cards")
  StudySessionResponse toResponse(Deck deck, List<Card> cards);

  StudyCardResponse toCard(Card card);
}
