package ru.vrazumi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RegistrationIT extends AbstractIntegrationTest {

  @Test
  void registerStoresUserWithHashedPassword() throws Exception {
    register("Ivan_Petrov", "Ivan@Example.com", "s3cret-pass")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("Ivan_Petrov"))
        .andExpect(jsonPath("$.email").value("ivan@example.com"))
        .andExpect(jsonPath("$.createdAt").exists());

    String hash = jdbc.queryForObject("SELECT password_hash FROM app_user", String.class);
    assertThat(hash).startsWith("{bcrypt}$2").doesNotContain("s3cret-pass");
  }

  @Test
  void usernameIsUniqueIgnoringCase() throws Exception {
    register("ivan", "ivan@example.com", "s3cret-pass").andExpect(status().isCreated());

    register("IVAN", "other@example.com", "s3cret-pass")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("username"));
  }

  @Test
  void emailIsUniqueIgnoringCase() throws Exception {
    register("ivan", "ivan@example.com", "s3cret-pass").andExpect(status().isCreated());

    register("petr", "IVAN@example.COM", "s3cret-pass")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].field").value("email"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Long.class)).isEqualTo(1);
  }

  private ResultActions register(String username, String email, String password) throws Exception {
    return mvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}"
                    .formatted(username, email, password)));
  }
}
