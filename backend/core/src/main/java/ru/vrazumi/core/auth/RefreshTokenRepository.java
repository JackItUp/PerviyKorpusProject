package ru.vrazumi.core.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      """
      update RefreshToken t set t.revokedAt = :now
      where t.user.id = :userId and t.revokedAt is null
      """)
  int revokeAllActive(Long userId, Instant now);
}
