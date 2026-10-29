package ru.vrazumi.core.study;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vrazumi.core.WebLayerTest;
import ru.vrazumi.core.card.CardType;
import ru.vrazumi.core.deck.DeckNotFoundException;
import ru.vrazumi.core.study.dto.StudyCardResponse;
import ru.vrazumi.core.study.dto.StudySessionResponse;

@WebLayerTest(StudyController.class)
class StudyControllerTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private StudyService studyService;

  @Test
  void returnsStudySession() throws Exception {
    when(studyService.getStudySession(1L))
        .thenReturn(
            new StudySessionResponse(
                1L,
                "Английские глаголы",
                List.of(new StudyCardResponse(10L, CardType.NORMAL, "go", "went, gone"))));

    mvc.perform(get("/api/decks/1/study"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deckId").value(1))
        .andExpect(jsonPath("$.deckTitle").value("Английские глаголы"))
        .andExpect(jsonPath("$.cards[0].id").value(10))
        .andExpect(jsonPath("$.cards[0].type").value("NORMAL"))
        .andExpect(jsonPath("$.cards[0].question").value("go"))
        .andExpect(jsonPath("$.cards[0].answer").value("went, gone"));
  }

  @Test
  void emptyDeckReturns200WithEmptyCards() throws Exception {
    when(studyService.getStudySession(1L))
        .thenReturn(new StudySessionResponse(1L, "Пустая", List.of()));

    mvc.perform(get("/api/decks/1/study"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards").isArray())
        .andExpect(jsonPath("$.cards").isEmpty());
  }

  @Test
  void missingDeckReturns404() throws Exception {
    when(studyService.getStudySession(99L)).thenThrow(new DeckNotFoundException(99L));

    mvc.perform(get("/api/decks/99/study"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Колода с id=99 не найдена"));
  }

  @Test
  void nonNumericDeckIdReturns400() throws Exception {
    mvc.perform(get("/api/decks/abc/study"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    verifyNoInteractions(studyService);
  }
}
