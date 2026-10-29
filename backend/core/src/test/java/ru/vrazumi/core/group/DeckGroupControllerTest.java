package ru.vrazumi.core.group;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vrazumi.core.DefaultTestUserConfig;
import ru.vrazumi.core.WebLayerTest;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.group.dto.DeckGroupRequest;
import ru.vrazumi.core.group.dto.DeckGroupResponse;

@WebLayerTest(DeckGroupController.class)
class DeckGroupControllerTest {

  private static final long USER = DefaultTestUserConfig.USER_ID;
  private static final Instant NOW = Instant.parse("2026-10-20T09:30:00Z");

  @Autowired private MockMvc mvc;
  @MockitoBean private DeckGroupService groupService;

  @Test
  void createReturns201WithLocation() throws Exception {
    when(groupService.create(new DeckGroupRequest("Английский"), USER))
        .thenReturn(group(3L, "Английский", 0));

    mvc.perform(
            post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"  Английский \"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/groups/3")))
        .andExpect(jsonPath("$.deckCount").value(0));
  }

  @Test
  void createRejectsBlankAndLongName() throws Exception {
    for (String name : List.of("   ", "x".repeat(101))) {
      mvc.perform(
              post("/api/groups")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\": \"%s\"}".formatted(name)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("name"));
    }
    verifyNoInteractions(groupService);
  }

  @Test
  void createReturns409ForDuplicateName() throws Exception {
    when(groupService.create(any(), eq(USER)))
        .thenThrow(new ConflictException("name", "Группа с таким названием уже есть"));

    mvc.perform(
            post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Английский\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void findAllReturnsGroups() throws Exception {
    when(groupService.findAll(USER)).thenReturn(List.of(group(3L, "Английский", 2)));

    mvc.perform(get("/api/groups"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Английский"))
        .andExpect(jsonPath("$[0].deckCount").value(2));
  }

  @Test
  void renameReturnsGroup() throws Exception {
    when(groupService.rename(3L, new DeckGroupRequest("История"), USER))
        .thenReturn(group(3L, "История", 0));

    mvc.perform(
            put("/api/groups/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"История\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("История"));
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/api/groups/3")).andExpect(status().isNoContent());
    verify(groupService).delete(3L, USER);
  }

  @Test
  void missingGroupReturns404() throws Exception {
    doThrow(new DeckGroupNotFoundException(9L)).when(groupService).delete(9L, USER);

    mvc.perform(delete("/api/groups/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Группа с id=9 не найдена"));
  }

  private static DeckGroupResponse group(Long id, String name, long deckCount) {
    return new DeckGroupResponse(id, name, deckCount, NOW, NOW);
  }
}
