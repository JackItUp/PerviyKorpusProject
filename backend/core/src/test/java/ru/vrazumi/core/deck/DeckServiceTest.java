package ru.vrazumi.core.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import ru.vrazumi.core.group.DeckGroup;
import ru.vrazumi.core.group.DeckGroupNotFoundException;
import ru.vrazumi.core.group.DeckGroupService;
import ru.vrazumi.core.user.User;
import ru.vrazumi.core.user.UserService;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

  @Mock private DeckRepository deckRepository;
  @Mock private UserService userService;
  @Mock private DeckGroupService deckGroupService;
  @Spy private DeckMapper deckMapper = Mappers.getMapper(DeckMapper.class);
  @InjectMocks private DeckService deckService;

  private static final long USER = 5L;
  private final User owner = new User();

  @Test
  void createSavesDeckWithTrimmedText() {
    when(userService.getReference(USER)).thenReturn(owner);
    when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> withId(invocation, 1L));

    DeckResponse response =
        deckService.create(
            new CreateDeckRequest("  Английские глаголы \n", "\tНеправильные глаголы  "), USER);

    ArgumentCaptor<Deck> saved = ArgumentCaptor.forClass(Deck.class);
    verify(deckRepository).save(saved.capture());
    assertThat(saved.getValue().getTitle()).isEqualTo("Английские глаголы");
    assertThat(saved.getValue().getDescription()).isEqualTo("Неправильные глаголы");
    assertThat(saved.getValue().getOwner()).isSameAs(owner);
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.title()).isEqualTo("Английские глаголы");
    assertThat(response.cardCount()).isZero();
  }

  @Test
  void createStoresBlankDescriptionAsNull() {
    when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> withId(invocation, 2L));

    DeckResponse response = deckService.create(new CreateDeckRequest("Колода", "   "), USER);

    ArgumentCaptor<Deck> saved = ArgumentCaptor.forClass(Deck.class);
    verify(deckRepository).save(saved.capture());
    assertThat(saved.getValue().getDescription()).isNull();
    assertThat(response.description()).isNull();
  }

  @Test
  void findAllMapsCardCounts() {
    when(deckRepository.findAllWithCardCount(USER))
        .thenReturn(
            List.of(
                new DeckWithCardCount(deck(2L, "Вторая"), 3L),
                new DeckWithCardCount(deck(1L, "Первая"), 0L)));

    assertThat(deckService.findAll(USER))
        .extracting(DeckResponse::id, DeckResponse::cardCount)
        .containsExactly(tuple(2L, 3L), tuple(1L, 0L));
  }

  @Test
  void findByIdReturnsDeckWithCardCount() {
    when(deckRepository.findWithCardCountById(1L, USER))
        .thenReturn(Optional.of(new DeckWithCardCount(deck(1L, "Колода"), 5L)));

    DeckResponse response = deckService.findById(1L, USER);

    assertThat(response.title()).isEqualTo("Колода");
    assertThat(response.cardCount()).isEqualTo(5L);
  }

  @Test
  void ensureExistsThrowsNotFoundForMissingDeck() {
    when(deckRepository.existsByIdAndOwnerId(42L, USER)).thenReturn(false);

    assertThatThrownBy(() -> deckService.ensureExists(42L, USER))
        .isInstanceOf(DeckNotFoundException.class);
  }

  @Test
  void findByIdThrowsNotFoundForMissingDeck() {
    when(deckRepository.findWithCardCountById(42L, USER)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> deckService.findById(42L, USER))
        .isInstanceOf(DeckNotFoundException.class)
        .hasMessageContaining("42");
  }

  @Test
  void getExistingReturnsDeck() {
    Deck deck = new Deck();
    deck.setId(7L);
    deck.setTitle("Колода");
    when(deckRepository.findByIdAndOwnerId(7L, USER)).thenReturn(Optional.of(deck));

    assertThat(deckService.getExisting(7L, USER)).isSameAs(deck);
  }

  @Test
  void moveToGroupSetsGroupOfSameUser() {
    Deck deck = deck(7L, "Колода");
    DeckGroup group = new DeckGroup();
    group.setId(3L);
    when(deckRepository.findByIdAndOwnerId(7L, USER)).thenReturn(Optional.of(deck));
    when(deckGroupService.getExisting(3L, USER)).thenReturn(group);
    when(deckRepository.findWithCardCountById(7L, USER))
        .thenReturn(Optional.of(new DeckWithCardCount(deck, 2L)));

    DeckResponse response = deckService.moveToGroup(7L, 3L, USER);

    assertThat(deck.getGroup()).isSameAs(group);
    assertThat(response.groupId()).isEqualTo(3L);
  }

  @Test
  void moveToNullGroupRemovesDeckFromGroup() {
    Deck deck = deck(7L, "Колода");
    deck.setGroup(new DeckGroup());
    when(deckRepository.findByIdAndOwnerId(7L, USER)).thenReturn(Optional.of(deck));
    when(deckRepository.findWithCardCountById(7L, USER))
        .thenReturn(Optional.of(new DeckWithCardCount(deck, 0L)));

    assertThat(deckService.moveToGroup(7L, null, USER).groupId()).isNull();
    assertThat(deck.getGroup()).isNull();
    verifyNoInteractions(deckGroupService);
  }

  @Test
  void moveToForeignGroupThrowsNotFound() {
    when(deckRepository.findByIdAndOwnerId(7L, USER)).thenReturn(Optional.of(deck(7L, "Колода")));
    when(deckGroupService.getExisting(99L, USER)).thenThrow(new DeckGroupNotFoundException(99L));

    assertThatThrownBy(() -> deckService.moveToGroup(7L, 99L, USER))
        .isInstanceOf(DeckGroupNotFoundException.class);
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
