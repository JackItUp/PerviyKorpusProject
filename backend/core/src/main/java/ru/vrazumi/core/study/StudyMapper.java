package ru.vrazumi.core.study;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardOption;
import ru.vrazumi.core.card.CardType;
import ru.vrazumi.core.deck.Deck;
import ru.vrazumi.core.study.dto.StudyCardResponse;
import ru.vrazumi.core.study.dto.StudyOptionResponse;
import ru.vrazumi.core.study.dto.StudySessionResponse;

@Mapper(imports = CardType.class)
public interface StudyMapper {

  @Mapping(target = "deckId", source = "deck.id")
  @Mapping(target = "deckTitle", source = "deck.title")
  @Mapping(target = "cards", source = "cards")
  StudySessionResponse toResponse(Deck deck, List<Card> cards);

  /** Правильный ответ показываем заранее только у NORMAL: остальные проверяет сервер. */
  @Mapping(
      target = "answer",
      expression = "java(card.getType() == CardType.NORMAL ? card.getAnswer() : null)")
  StudyCardResponse toCard(Card card);

  StudyOptionResponse toOption(CardOption option);
}
