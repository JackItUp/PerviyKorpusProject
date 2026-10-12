package ru.vrazumi.core.study;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.deck.Deck;
import ru.vrazumi.core.deck.DeckService;
import ru.vrazumi.core.study.dto.StudySessionResponse;

/**
 * Подбор карточек для прохождения колоды. Сейчас это все карточки по порядку добавления; позже
 * здесь появится алгоритм интервального повторения.
 */
@Service
@RequiredArgsConstructor
public class StudyService {

  private final DeckService deckService;
  private final CardService cardService;
  private final StudyMapper studyMapper;

  @Transactional(readOnly = true)
  public StudySessionResponse getStudySession(Long deckId) {
    Deck deck = deckService.getExisting(deckId);
    List<Card> cards = cardService.getCardsInOrder(deckId);
    return studyMapper.toResponse(deck, cards);
  }
}
