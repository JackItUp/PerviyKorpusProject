package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Колоды, карточки и оценки видны только владельцу. Чужое неотличимо от несуществующего: 404. */
class OwnershipIT extends AbstractIntegrationTest {

  private long aliceDeck;
  private long aliceCard;
  private String bob;

  @BeforeEach
  void aliceCreatesDeck() throws Exception {
    actAs("alice");
    aliceDeck = id(post("/api/decks"), "{\"title\": \"Колода Алисы\"}");
    aliceCard =
        id(
            post("/api/decks/" + aliceDeck + "/cards"),
            "{\"question\": \"go\", \"answer\": \"went\"}");
    bob = bearerOf("bob");
  }

  @Test
  void otherUserDoesNotSeeDeckInList() throws Exception {
    mvc.perform(get("/api/decks").header(HttpHeaders.AUTHORIZATION, bob))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mvc.perform(get("/api/decks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void otherUserGets404ForEveryDeckEndpoint() throws Exception {
    for (String uri :
        new String[] {
          "/api/decks/" + aliceDeck,
          "/api/decks/" + aliceDeck + "/cards",
          "/api/decks/" + aliceDeck + "/study"
        }) {
      mvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bob))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Колода с id=%d не найдена".formatted(aliceDeck)));
    }
  }

  @Test
  void otherUserCannotAddCardOrReview() throws Exception {
    mvc.perform(
            post("/api/decks/" + aliceDeck + "/cards")
                .header(HttpHeaders.AUTHORIZATION, bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"чужая\", \"answer\": \"карточка\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/cards/" + aliceCard + "/review")
                .header(HttpHeaders.AUTHORIZATION, bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\": \"KNOW\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Карточка с id=%d не найдена".formatted(aliceCard)));

    assertThat(jdbc.queryForObject("SELECT count(*) FROM card", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM review", Long.class)).isZero();
  }

  @Test
  void deckAndReviewAreStoredWithOwner() throws Exception {
    mvc.perform(
            post("/api/cards/" + aliceCard + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\": \"KNOW\"}"))
        .andExpect(status().isCreated());

    Long alice =
        jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'alice'", Long.class);
    assertThat(jdbc.queryForObject("SELECT owner_id FROM deck", Long.class)).isEqualTo(alice);
    assertThat(jdbc.queryForObject("SELECT user_id FROM review", Long.class)).isEqualTo(alice);
  }

  @Test
  void requestWithoutTokenIsUnauthorized() throws Exception {
    anonymous
        .perform(get("/api/decks"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  private long id(MockHttpServletRequestBuilder request, String json) throws Exception {
    String body =
        mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }
}
