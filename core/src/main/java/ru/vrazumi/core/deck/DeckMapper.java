package ru.vrazumi.core.deck;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.deck.dto.CreateDeckRequest;
import ru.vrazumi.core.deck.dto.DeckResponse;

@Mapper
public interface DeckMapper {

  @Mapping(target = "id", ignore = true)
  Deck toEntity(CreateDeckRequest request);

  @Mapping(target = "cardCount", source = "cardCount")
  DeckResponse toResponse(Deck deck, long cardCount);
}
