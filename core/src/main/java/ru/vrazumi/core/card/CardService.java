package ru.vrazumi.core.card;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.card.dto.CardResponse;
import ru.vrazumi.core.card.dto.CreateCardRequest;
import ru.vrazumi.core.deck.Deck;
import ru.vrazumi.core.deck.DeckService;

@Service
@RequiredArgsConstructor
public class CardService {

  private final CardRepository cardRepository;
  private final CardMapper cardMapper;
  private final DeckService deckService;

  @Transactional
  public CardResponse create(Long deckId, CreateCardRequest request) {
    Deck deck = deckService.getExisting(deckId);
    Card card = cardRepository.save(cardMapper.toEntity(request, deck));
    return cardMapper.toResponse(card);
  }

  @Transactional(readOnly = true)
  public List<CardResponse> findByDeck(Long deckId) {
    deckService.ensureExists(deckId);
    return cardRepository.findByDeckIdOrderByIdAsc(deckId).stream()
        .map(cardMapper::toResponse)
        .toList();
  }

  /** Карточки колоды в порядке добавления для других фич. Существование колоды не проверяет. */
  @Transactional(readOnly = true)
  public List<Card> getCardsInOrder(Long deckId) {
    return cardRepository.findByDeckIdOrderByIdAsc(deckId);
  }
}
