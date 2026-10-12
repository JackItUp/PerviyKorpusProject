package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Демо-данные загружаются только с контекстом Liquibase demo. Отдельный контекст и контейнер. */
@SpringBootTest(properties = "spring.liquibase.contexts=demo")
@Import(TestcontainersConfiguration.class)
class DemoDataIT {

  @Autowired private JdbcTemplate jdbc;

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
}
