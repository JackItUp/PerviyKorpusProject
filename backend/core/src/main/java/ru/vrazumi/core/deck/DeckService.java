package ru.vrazumi.core.deck;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.deck.dto.CreateDeckRequest;
import ru.vrazumi.core.deck.dto.DeckResponse;

@Service
@RequiredArgsConstructor
public class DeckService {

  private final DeckRepository deckRepository;
  private final DeckMapper deckMapper;

  @Transactional
  public DeckResponse create(CreateDeckRequest request) {
    Deck deck = deckRepository.save(deckMapper.toEntity(request));
    return deckMapper.toResponse(deck, 0);
  }

  @Transactional(readOnly = true)
  public List<DeckResponse> findAll() {
    return deckRepository.findAllWithCardCount().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public DeckResponse findById(Long deckId) {
    return deckRepository
        .findWithCardCountById(deckId)
        .map(this::toResponse)
        .orElseThrow(() -> new DeckNotFoundException(deckId));
  }

  /** Колода для других фич (карточки, изучение). Бросает 404, если колоды нет. */
  @Transactional(readOnly = true)
  public Deck getExisting(Long deckId) {
    return deckRepository.findById(deckId).orElseThrow(() -> new DeckNotFoundException(deckId));
  }

  /** Проверка существования колоды без загрузки. Бросает 404, если колоды нет. */
  @Transactional(readOnly = true)
  public void ensureExists(Long deckId) {
    if (!deckRepository.existsById(deckId)) {
      throw new DeckNotFoundException(deckId);
    }
  }

  private DeckResponse toResponse(DeckWithCardCount row) {
    return deckMapper.toResponse(row.deck(), row.cardCount());
  }
}
