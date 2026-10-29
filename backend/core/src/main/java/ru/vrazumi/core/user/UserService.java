package ru.vrazumi.core.user;

import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.common.UnauthorizedException;
import ru.vrazumi.core.user.dto.RegisterRequest;
import ru.vrazumi.core.user.dto.UserResponse;

@Service
@RequiredArgsConstructor
public class UserService {

  static final String USERNAME_TAKEN = "Имя пользователя уже занято";
  static final String EMAIL_TAKEN = "Пользователь с таким email уже зарегистрирован";

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResponse register(RegisterRequest request) {
    if (userRepository.existsByUsernameIgnoreCase(request.username())) {
      throw new ConflictException("username", USERNAME_TAKEN);
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("email", EMAIL_TAKEN);
    }

    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    try {
      return userMapper.toResponse(userRepository.saveAndFlush(user));
    } catch (DataIntegrityViolationException ex) {
      // Гонка двух одновременных регистраций: проверки выше прошли, но сработал уникальный индекс
      throw usernameIndexViolated(ex)
          ? new ConflictException("username", USERNAME_TAKEN)
          : new ConflictException("email", EMAIL_TAKEN);
    }
  }

  /** Пользователь по имени (без учёта регистра) или по email, если в логине есть {@code @}. */
  @Transactional(readOnly = true)
  public Optional<User> findByLogin(String login) {
    return login.contains("@")
        ? userRepository.findByEmail(login.toLowerCase(Locale.ROOT))
        : userRepository.findByUsernameIgnoreCase(login);
  }

  /**
   * Пользователь по id из токена. Если его уже нет (аккаунт удалён), токен считается
   * недействительным: 401.
   */
  @Transactional(readOnly = true)
  public User getExisting(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));
  }

  @Transactional(readOnly = true)
  public UserResponse getProfile(Long userId) {
    return userMapper.toResponse(getExisting(userId));
  }

  private static boolean usernameIndexViolated(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause().getMessage();
    return message != null && message.contains("uq_app_user_username_lower");
  }
}
