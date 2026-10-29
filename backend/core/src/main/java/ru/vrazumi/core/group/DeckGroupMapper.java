package ru.vrazumi.core.group;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.group.dto.DeckGroupResponse;

@Mapper
public interface DeckGroupMapper {

  @Mapping(target = "deckCount", source = "deckCount")
  DeckGroupResponse toResponse(DeckGroup group, long deckCount);
}
