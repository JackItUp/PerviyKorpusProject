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
  public CardResponse create(Long deckId, CreateCardRequest request, Long userId) {
    Deck deck = deckService.getExisting(deckId, userId);
    Card card = cardMapper.toEntity(request, deck);
    request.options().forEach(option -> card.addOption(option.text(), option.correct()));
    return cardMapper.toResponse(cardRepository.save(card));
  }

  @Transactional(readOnly = true)
  public List<CardResponse> findByDeck(Long deckId, Long userId) {
    deckService.ensureExists(deckId, userId);
    return cardRepository.findByDeckIdOrderByIdAsc(deckId).stream()
        .map(cardMapper::toResponse)
        .toList();
  }

  /** Карточка пользователя для других фич (оценки). Чужая или несуществующая — 404. */
  @Transactional(readOnly = true)
  public Card getExisting(Long cardId, Long userId) {
    return cardRepository
        .findByIdAndDeckOwnerId(cardId, userId)
        .orElseThrow(() -> new CardNotFoundException(cardId));
  }

  /**
   * Карточки колоды в порядке добавления для других фич. Владельца не проверяет: вызывающий уже
   * получил колоду через {@link DeckService}.
   */
  @Transactional(readOnly = true)
  public List<Card> getCardsInOrder(Long deckId) {
    return cardRepository.findByDeckIdOrderByIdAsc(deckId);
  }
}
