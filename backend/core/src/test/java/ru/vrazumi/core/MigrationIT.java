package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Схема создаётся только миграциями Liquibase, а Hibernate в режиме validate сверяет с ней
 * сущности. Если бы схема не совпала, контекст не поднялся бы и тесты упали бы ещё на старте.
 */
class MigrationIT extends AbstractIntegrationTest {

  @Value("${spring.jpa.hibernate.ddl-auto}")
  private String ddlAuto;

  @Test
  void hibernateOnlyValidatesSchema() {
    assertThat(ddlAuto).isEqualTo("validate");
  }

  @Test
  void schemaChangeSetsAppliedWithoutDemoData() {
    List<String> ids =
        jdbc.queryForList("SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class);

    assertThat(ids)
        .containsExactly(
            "001-create-deck",
            "002-create-card",
            "003-create-review",
            "005-create-app-user",
            "006-create-refresh-token");
  }

  @Test
  void tablesExist() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String.class);

    assertThat(tables).contains("deck", "card", "review");
  }

  @Test
  void cardTypeDefaultsToNormal() {
    Long deckId = insertDeck();
    jdbc.update(
        "INSERT INTO card (deck_id, question, answer, created_at, updated_at)"
            + " VALUES (?, 'q', 'a', now(), now())",
        deckId);

    assertThat(jdbc.queryForObject("SELECT type FROM card", String.class)).isEqualTo("NORMAL");
  }

  @Test
  void reviewRatingCheckConstraintRejectsUnknownValue() {
    Long deckId = insertDeck();
    Long cardId =
        jdbc.queryForObject(
            "INSERT INTO card (deck_id, question, answer, created_at, updated_at)"
                + " VALUES (?, 'q', 'a', now(), now()) RETURNING id",
            Long.class,
            deckId);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO review (card_id, rating, reviewed_at) VALUES (?, 'SUPER', now())",
                    cardId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_review_rating");
  }

  @Test
  void cardRequiresExistingDeck() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO card (deck_id, question, answer, created_at, updated_at)"
                        + " VALUES (999, 'q', 'a', now(), now())"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fk_card_deck");
  }

  private Long insertDeck() {
    return jdbc.queryForObject(
        "INSERT INTO deck (title, created_at, updated_at) VALUES ('Колода', now(), now())"
            + " RETURNING id",
        Long.class);
  }
}
