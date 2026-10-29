package ru.vrazumi.core.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vrazumi.core.card.dto.CardResponse;
import ru.vrazumi.core.card.dto.CreateCardRequest;
import ru.vrazumi.core.deck.Deck;
import ru.vrazumi.core.deck.DeckNotFoundException;
import ru.vrazumi.core.deck.DeckService;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

  @Mock private CardRepository cardRepository;
  @Mock private DeckService deckService;
  @Spy private CardMapper cardMapper = Mappers.getMapper(CardMapper.class);
  @InjectMocks private CardService cardService;

  private static final long USER = 5L;

  @Test
  void createSavesNormalCardWithTrimmedTextInDeck() {
    Deck deck = deck(1L);
    when(deckService.getExisting(1L, USER)).thenReturn(deck);
    when(cardRepository.save(any(Card.class)))
        .thenAnswer(
            invocation -> {
              Card card = invocation.getArgument(0);
              card.setId(10L);
              return card;
            });

    CardResponse response =
        cardService.create(1L, new CreateCardRequest("  go ", " went, gone\n"), USER);

    ArgumentCaptor<Card> saved = ArgumentCaptor.forClass(Card.class);
    verify(cardRepository).save(saved.capture());
    assertThat(saved.getValue().getDeck()).isSameAs(deck);
    assertThat(saved.getValue().getType()).isEqualTo(CardType.NORMAL);
    assertThat(saved.getValue().getQuestion()).isEqualTo("go");
    assertThat(saved.getValue().getAnswer()).isEqualTo("went, gone");
    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.deckId()).isEqualTo(1L);
    assertThat(response.type()).isEqualTo(CardType.NORMAL);
  }

  @Test
  void createThrowsNotFoundForMissingDeck() {
    when(deckService.getExisting(99L, USER)).thenThrow(new DeckNotFoundException(99L));

    assertThatThrownBy(() -> cardService.create(99L, new CreateCardRequest("go", "went"), USER))
        .isInstanceOf(DeckNotFoundException.class);
    verifyNoInteractions(cardRepository);
  }

  @Test
  void findByDeckReturnsCardsInRepositoryOrder() {
    Deck deck = deck(1L);
    when(cardRepository.findByDeckIdOrderByIdAsc(1L))
        .thenReturn(List.of(card(10L, deck, "go"), card(11L, deck, "be")));

    assertThat(cardService.findByDeck(1L, USER))
        .extracting(CardResponse::id, CardResponse::question)
        .containsExactly(tuple(10L, "go"), tuple(11L, "be"));
    verify(deckService).ensureExists(1L, USER);
  }

  @Test
  void findByDeckThrowsNotFoundForMissingDeck() {
    doThrow(new DeckNotFoundException(99L)).when(deckService).ensureExists(99L, USER);

    assertThatThrownBy(() -> cardService.findByDeck(99L, USER))
        .isInstanceOf(DeckNotFoundException.class);
    verifyNoInteractions(cardRepository);
  }

  static Deck deck(Long id) {
    Deck deck = new Deck();
    deck.setId(id);
    deck.setTitle("Колода " + id);
    return deck;
  }

  static Card card(Long id, Deck deck, String question) {
    Card card = new Card();
    card.setId(id);
    card.setDeck(deck);
    card.setQuestion(question);
    card.setAnswer("ответ на " + question);
    return card;
  }
}
