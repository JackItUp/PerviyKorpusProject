package ru.vrazumi.core.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByEmail(String email);

  Optional<User> findByUsernameIgnoreCase(String username);

  Optional<User> findByEmail(String email);
}
