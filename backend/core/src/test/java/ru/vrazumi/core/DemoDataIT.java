package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Демо-данные загружаются только с контекстом Liquibase demo и принадлежат пользователю {@code
 * demo}. Отдельный контекст и контейнер.
 */
@SpringBootTest(properties = "spring.liquibase.contexts=demo")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DemoDataIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mvc;

  @Test
  void demoContextLoadsTwoDecksWithFiveCardsEach() {
    List<Integer> cardsPerDeck =
        jdbc.queryForList(
            "SELECT count(c.id) FROM deck d LEFT JOIN card c ON c.deck_id = d.id"
                + " GROUP BY d.id ORDER BY d.id",
            Integer.class);

    assertThat(cardsPerDeck).containsExactly(5, 5);
  }

  @Test
  void demoCardsKeepInsertionOrder() {
    List<String> questions =
        jdbc.queryForList(
            "SELECT c.question FROM card c JOIN deck d ON d.id = c.deck_id"
                + " WHERE d.title = 'Столицы мира' ORDER BY c.id",
            String.class);

    assertThat(questions).containsExactly("Франция", "Япония", "Канада", "Австралия", "Бразилия");
  }

  @Test
  void demoUserCanLogInAndSeesDemoDecks() throws Exception {
    String tokens =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"login\": \"demo\", \"password\": \"demo-password\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonPath.read(tokens, "$.accessToken");

    mvc.perform(get("/api/decks").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].cardCount").value(everyItem(is(5))));
  }
}
