package ru.vrazumi.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.user.dto.RegisterRequest;
import ru.vrazumi.core.user.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  private final PasswordEncoder passwordEncoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();
  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(userRepository, Mappers.getMapper(UserMapper.class), passwordEncoder);
  }

  @Test
  void registerStoresBcryptHashAndNormalizedEmail() {
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(1L);
              return user;
            });

    UserResponse response =
        userService.register(
            new RegisterRequest("  Ivan_Petrov ", " Ivan@Example.COM ", "s3cret-pass"));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getUsername()).isEqualTo("Ivan_Petrov");
    assertThat(saved.getValue().getEmail()).isEqualTo("ivan@example.com");
    assertThat(saved.getValue().getPasswordHash())
        .startsWith("{bcrypt}")
        .doesNotContain("s3cret-pass");
    assertThat(passwordEncoder.matches("s3cret-pass", saved.getValue().getPasswordHash())).isTrue();
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("ivan@example.com");
  }

  @Test
  void registerRejectsTakenUsername() {
    when(userRepository.existsByUsernameIgnoreCase("ivan")).thenReturn(true);

    assertThatThrownBy(
            () -> userService.register(new RegisterRequest("ivan", "a@b.ru", "password1")))
        .isInstanceOfSatisfying(
            ConflictException.class, ex -> assertThat(ex.getField()).isEqualTo("username"));
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void registerRejectsTakenEmail() {
    when(userRepository.existsByEmail("a@b.ru")).thenReturn(true);

    assertThatThrownBy(
            () -> userService.register(new RegisterRequest("ivan", "A@B.ru", "password1")))
        .isInstanceOfSatisfying(
            ConflictException.class, ex -> assertThat(ex.getField()).isEqualTo("email"));
  }

  @Test
  void registerTranslatesUniqueIndexRaceIntoConflict() {
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(
            new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_app_user_username_lower\""));

    assertThatThrownBy(
            () -> userService.register(new RegisterRequest("ivan", "a@b.ru", "password1")))
        .isInstanceOfSatisfying(
            ConflictException.class, ex -> assertThat(ex.getField()).isEqualTo("username"));
  }
}
