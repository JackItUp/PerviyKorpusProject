package ru.vrazumi.core.card;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.card.dto.CardOptionResponse;
import ru.vrazumi.core.card.dto.CardResponse;
import ru.vrazumi.core.card.dto.CreateCardRequest;
import ru.vrazumi.core.deck.Deck;

@Mapper
public interface CardMapper {

  /** Варианты ответа добавляет сервис через {@link Card#addOption}: им нужны позиции. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "deck", source = "deck")
  @Mapping(target = "type", source = "request.type")
  @Mapping(target = "question", source = "request.question")
  @Mapping(target = "answer", source = "request.answer")
  @Mapping(target = "options", ignore = true)
  Card toEntity(CreateCardRequest request, Deck deck);

  @Mapping(target = "deckId", source = "deck.id")
  CardResponse toResponse(Card card);

  CardOptionResponse toOptionResponse(CardOption option);
}
