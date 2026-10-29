package ru.vrazumi.core.deck;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.deck.dto.CreateDeckRequest;
import ru.vrazumi.core.deck.dto.DeckResponse;
import ru.vrazumi.core.user.UserService;

/**
 * Колоды текущего пользователя. Чужая колода неотличима от несуществующей (404): так не раскрываем,
 * что она есть.
 */
@Service
@RequiredArgsConstructor
public class DeckService {

  private final DeckRepository deckRepository;
  private final DeckMapper deckMapper;
  private final UserService userService;

  @Transactional
  public DeckResponse create(CreateDeckRequest request, Long userId) {
    Deck deck = deckMapper.toEntity(request);
    deck.setOwner(userService.getReference(userId));
    return deckMapper.toResponse(deckRepository.save(deck), 0);
  }

  @Transactional(readOnly = true)
  public List<DeckResponse> findAll(Long userId) {
    return deckRepository.findAllWithCardCount(userId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public DeckResponse findById(Long deckId, Long userId) {
    return deckRepository
        .findWithCardCountById(deckId, userId)
        .map(this::toResponse)
        .orElseThrow(() -> new DeckNotFoundException(deckId));
  }

  /** Колода пользователя для других фич (карточки, изучение). Бросает 404, если её нет. */
  @Transactional(readOnly = true)
  public Deck getExisting(Long deckId, Long userId) {
    return deckRepository
        .findByIdAndOwnerId(deckId, userId)
        .orElseThrow(() -> new DeckNotFoundException(deckId));
  }

  /** Проверка, что колода есть и принадлежит пользователю, без загрузки. Иначе 404. */
  @Transactional(readOnly = true)
  public void ensureExists(Long deckId, Long userId) {
    if (!deckRepository.existsByIdAndOwnerId(deckId, userId)) {
      throw new DeckNotFoundException(deckId);
    }
  }

  private DeckResponse toResponse(DeckWithCardCount row) {
    return deckMapper.toResponse(row.deck(), row.cardCount());
  }
}
