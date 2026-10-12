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
    // Количество карточек появится вместе с карточками
    return deckRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
        .map(deck -> deckMapper.toResponse(deck, 0))
        .toList();
  }

  @Transactional(readOnly = true)
  public DeckResponse findById(Long deckId) {
    return deckMapper.toResponse(getExisting(deckId), 0);
  }

  /** Колода для других фич (карточки, изучение). Бросает 404, если колоды нет. */
  @Transactional(readOnly = true)
  public Deck getExisting(Long deckId) {
    return deckRepository.findById(deckId).orElseThrow(() -> new DeckNotFoundException(deckId));
  }
}
