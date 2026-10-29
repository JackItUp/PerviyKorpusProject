package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Группы колод: пустая группа, перенос колод, удаление группы с колодами, чужие группы. */
class GroupsIT extends AbstractIntegrationTest {

  @BeforeEach
  void signIn() throws Exception {
    actAs("alice");
  }

  @Test
  void emptyGroupHasZeroDecks() throws Exception {
    long groupId = createGroup("История");

    mvc.perform(get("/api/groups/{id}", groupId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("История"))
        .andExpect(jsonPath("$.deckCount").value(0));
  }

  @Test
  void decksMoveBetweenGroupsAndCountsUpdate() throws Exception {
    long english = createGroup("Английский");
    long history = createGroup("История");
    long verbs = createDeck("Глаголы");
    long nouns = createDeck("Существительные");

    moveDeck(verbs, english)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupId").value(english));
    moveDeck(nouns, english).andExpect(status().isOk());
    moveDeck(nouns, history).andExpect(status().isOk());

    mvc.perform(get("/api/groups"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Английский"))
        .andExpect(jsonPath("$[0].deckCount").value(1))
        .andExpect(jsonPath("$[1].name").value("История"))
        .andExpect(jsonPath("$[1].deckCount").value(1));

    moveDeck(verbs, null).andExpect(status().isOk()).andExpect(jsonPath("$.groupId").isEmpty());
  }

  @Test
  void deletingGroupKeepsItsDecksWithoutGroup() throws Exception {
    long groupId = createGroup("Английский");
    long deckId = createDeck("Глаголы");
    createCard(deckId);
    moveDeck(deckId, groupId).andExpect(status().isOk());

    mvc.perform(delete("/api/groups/{id}", groupId)).andExpect(status().isNoContent());

    mvc.perform(get("/api/decks/{id}", deckId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupId").isEmpty())
        .andExpect(jsonPath("$.cardCount").value(1));
    mvc.perform(get("/api/groups/{id}", groupId)).andExpect(status().isNotFound());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM deck", Long.class)).isEqualTo(1);
  }

  @Test
  void duplicateNameIgnoringCaseIsConflict() throws Exception {
    createGroup("Английский");

    postJson("/api/groups", "{\"name\": \"АНГЛИЙСКИЙ\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("name"));

    long history = createGroup("История");
    mvc.perform(
            put("/api/groups/{id}", history)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"английский\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void sameNameIsAllowedForDifferentUsers() throws Exception {
    createGroup("Английский");
    String bob = bearerOf("bob");

    mvc.perform(
            post("/api/groups")
                .header(HttpHeaders.AUTHORIZATION, bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Английский\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void foreignGroupsAreInvisible() throws Exception {
    long aliceGroup = createGroup("Английский");
    String bob = bearerOf("bob");
    long bobDeck =
        ((Number)
                JsonPath.read(
                    mvc.perform(
                            post("/api/decks")
                                .header(HttpHeaders.AUTHORIZATION, bob)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\": \"Колода Боба\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                    "$.id"))
            .longValue();

    mvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bob))
        .andExpect(jsonPath("$").isEmpty());
    mvc.perform(delete("/api/groups/{id}", aliceGroup).header(HttpHeaders.AUTHORIZATION, bob))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/decks/{id}/group", bobDeck)
                .header(HttpHeaders.AUTHORIZATION, bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\": %d}".formatted(aliceGroup)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Группа с id=%d не найдена".formatted(aliceGroup)));
  }

  private long createGroup(String name) throws Exception {
    return id(postJson("/api/groups", "{\"name\": \"%s\"}".formatted(name)));
  }

  private long createDeck(String title) throws Exception {
    return id(postJson("/api/decks", "{\"title\": \"%s\"}".formatted(title)));
  }

  private void createCard(long deckId) throws Exception {
    postJson("/api/decks/" + deckId + "/cards", "{\"question\": \"go\", \"answer\": \"went\"}")
        .andExpect(status().isCreated());
  }

  private ResultActions moveDeck(long deckId, Long groupId) throws Exception {
    return mvc.perform(
        put("/api/decks/{id}/group", deckId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"groupId\": %s}".formatted(groupId)));
  }

  private ResultActions postJson(String uri, String json) throws Exception {
    return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static long id(ResultActions result) throws Exception {
    String body =
        result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }
}
