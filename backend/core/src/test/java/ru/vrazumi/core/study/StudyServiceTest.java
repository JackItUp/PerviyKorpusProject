package ru.vrazumi.core.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.card.CardService;
import ru.vrazumi.core.card.CardType;
import ru.vrazumi.core.deck.Deck;
import ru.vrazumi.core.deck.DeckNotFoundException;
import ru.vrazumi.core.deck.DeckService;
import ru.vrazumi.core.study.dto.StudyCardResponse;
import ru.vrazumi.core.study.dto.StudySessionResponse;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

  @Mock private DeckService deckService;
  @Mock private CardService cardService;
  @Spy private StudyMapper studyMapper = Mappers.getMapper(StudyMapper.class);
  @InjectMocks private StudyService studyService;

  private static final long USER = 5L;

  @Test
  void returnsDeckTitleAndCardsInOrder() {
    Deck deck = deck();
    when(deckService.getExisting(1L, USER)).thenReturn(deck);
    when(cardService.getCardsInOrder(1L))
        .thenReturn(List.of(card(10L, deck, "go", "went"), card(11L, deck, "be", "was")));

    StudySessionResponse session = studyService.getStudySession(1L, USER);

    assertThat(session.deckId()).isEqualTo(1L);
    assertThat(session.deckTitle()).isEqualTo("Английские глаголы");
    assertThat(session.cards())
        .containsExactly(
            new StudyCardResponse(10L, CardType.NORMAL, "go", "went"),
            new StudyCardResponse(11L, CardType.NORMAL, "be", "was"));
  }

  @Test
  void emptyDeckGivesEmptyCardList() {
    when(deckService.getExisting(1L, USER)).thenReturn(deck());
    when(cardService.getCardsInOrder(1L)).thenReturn(List.of());

    assertThat(studyService.getStudySession(1L, USER).cards()).isEmpty();
  }

  @Test
  void missingDeckThrowsNotFound() {
    when(deckService.getExisting(99L, USER)).thenThrow(new DeckNotFoundException(99L));

    assertThatThrownBy(() -> studyService.getStudySession(99L, USER))
        .isInstanceOf(DeckNotFoundException.class);
    verifyNoInteractions(cardService);
  }

  private static Deck deck() {
    Deck deck = new Deck();
    deck.setId(1L);
    deck.setTitle("Английские глаголы");
    return deck;
  }

  private static Card card(Long id, Deck deck, String question, String answer) {
    Card card = new Card();
    card.setId(id);
    card.setDeck(deck);
    card.setQuestion(question);
    card.setAnswer(answer);
    return card;
  }
}
