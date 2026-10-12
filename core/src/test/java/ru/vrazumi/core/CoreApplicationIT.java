package ru.vrazumi.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CoreApplicationIT extends AbstractIntegrationTest {

  @Test
  void healthIsUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void onlyHealthActuatorEndpointIsExposed() throws Exception {
    mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
  }

  @Test
  void openApiSpecListsAllEndpoints() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/decks'].post").exists())
        .andExpect(jsonPath("$.paths['/api/decks'].get").exists())
        .andExpect(jsonPath("$.paths['/api/decks/{deckId}'].get").exists())
        .andExpect(jsonPath("$.paths['/api/decks/{deckId}/cards'].post").exists())
        .andExpect(jsonPath("$.paths['/api/decks/{deckId}/cards'].get").exists())
        .andExpect(jsonPath("$.paths['/api/decks/{deckId}/study'].get").exists())
        .andExpect(jsonPath("$.paths['/api/cards/{cardId}/review'].post").exists());
  }

  @Test
  void swaggerUiIsAvailable() throws Exception {
    mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
  }

  @Test
  void corsAllowsConfiguredFrontendOrigin() throws Exception {
    mvc.perform(
            options("/api/decks")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
  }

  @Test
  void corsRejectsUnknownOrigin() throws Exception {
    mvc.perform(
            options("/api/decks")
                .header(HttpHeaders.ORIGIN, "http://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }
}
