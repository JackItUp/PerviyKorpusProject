package ru.vrazumi.core.card;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.card.dto.CardResponse;
import ru.vrazumi.core.card.dto.CreateCardRequest;
import ru.vrazumi.core.deck.Deck;

@Mapper
public interface CardMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "deck", source = "deck")
  @Mapping(target = "type", constant = "NORMAL")
  @Mapping(target = "question", source = "request.question")
  @Mapping(target = "answer", source = "request.answer")
  Card toEntity(CreateCardRequest request, Deck deck);

  @Mapping(target = "deckId", source = "deck.id")
  CardResponse toResponse(Card card);
}
