package ru.vrazumi.core.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  void findByIdThrowsNotFoundForMissingDeck() {
    when(deckRepository.findById(42L)).thenReturn(Optional.empty());

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

  private static Deck withId(InvocationOnMock invocation, long id) {
    Deck deck = invocation.getArgument(0);
    deck.setId(id);
    return deck;
  }
}
