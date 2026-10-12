package ru.vrazumi.core.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vrazumi.core.deck.dto.CreateDeckRequest;
import ru.vrazumi.core.deck.dto.DeckResponse;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

  @Mock private DeckRepository deckRepository;
  @Spy private DeckMapper deckMapper = Mappers.getMapper(DeckMapper.class);
  @InjectMocks private DeckService deckService;

  @Test
  void createSavesDeckWithTrimmedText() {
    when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> withId(invocation, 1L));

    DeckResponse response =
        deckService.create(
            new CreateDeckRequest("  Английские глаголы \n", "\tНеправильные глаголы  "));

    ArgumentCaptor<Deck> saved = ArgumentCaptor.forClass(Deck.class);
    verify(deckRepository).save(saved.capture());
    assertThat(saved.getValue().getTitle()).isEqualTo("Английские глаголы");
    assertThat(saved.getValue().getDescription()).isEqualTo("Неправильные глаголы");
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.title()).isEqualTo("Английские глаголы");
    assertThat(response.cardCount()).isZero();
  }

  @Test
  void createStoresBlankDescriptionAsNull() {
    when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> withId(invocation, 2L));

    DeckResponse response = deckService.create(new CreateDeckRequest("Колода", "   "));

    ArgumentCaptor<Deck> saved = ArgumentCaptor.forClass(Deck.class);
    verify(deckRepository).save(saved.capture());
    assertThat(saved.getValue().getDescription()).isNull();
    assertThat(response.description()).isNull();
  }

  @Test
  void findAllMapsCardCounts() {
    when(deckRepository.findAllWithCardCount())
        .thenReturn(
            List.of(
                new DeckWithCardCount(deck(2L, "Вторая"), 3L),
                new DeckWithCardCount(deck(1L, "Первая"), 0L)));

    assertThat(deckService.findAll())
        .extracting(DeckResponse::id, DeckResponse::cardCount)
        .containsExactly(tuple(2L, 3L), tuple(1L, 0L));
  }

  @Test
  void findByIdReturnsDeckWithCardCount() {
    when(deckRepository.findWithCardCountById(1L))
        .thenReturn(Optional.of(new DeckWithCardCount(deck(1L, "Колода"), 5L)));

    DeckResponse response = deckService.findById(1L);

    assertThat(response.title()).isEqualTo("Колода");
    assertThat(response.cardCount()).isEqualTo(5L);
  }

  @Test
  void ensureExistsThrowsNotFoundForMissingDeck() {
    when(deckRepository.existsById(42L)).thenReturn(false);

    assertThatThrownBy(() -> deckService.ensureExists(42L))
        .isInstanceOf(DeckNotFoundException.class);
  }

  @Test
  void findByIdThrowsNotFoundForMissingDeck() {
    when(deckRepository.findWithCardCountById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> deckService.findById(42L))
        .isInstanceOf(DeckNotFoundException.class)
        .hasMessageContaining("42");
  }

  @Test
  void getExistingReturnsDeck() {
    Deck deck = new Deck();
    deck.setId(7L);
    deck.setTitle("Колода");
    when(deckRepository.findById(7L)).thenReturn(Optional.of(deck));

    assertThat(deckService.getExisting(7L)).isSameAs(deck);
  }

  private static Deck deck(Long id, String title) {
    Deck deck = new Deck();
    deck.setId(id);
    deck.setTitle(title);
    return deck;
  }

  private static Deck withId(InvocationOnMock invocation, long id) {
    Deck deck = invocation.getArgument(0);
    deck.setId(id);
    return deck;
  }
}
