package ru.vrazumi.core;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Общая база интеграционных тестов: полный контекст и PostgreSQL в Testcontainers. Контекст (и
 * контейнер) переиспользуется всеми наследниками, данные очищаются перед каждым тестом.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

  @Autowired protected MockMvc mvc;
  @Autowired protected JdbcTemplate jdbc;

  @BeforeEach
  void cleanDatabase() {
    jdbc.execute("TRUNCATE TABLE review, card, deck RESTART IDENTITY CASCADE");
  }
}
