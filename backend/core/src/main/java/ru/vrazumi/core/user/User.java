package ru.vrazumi.core.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.vrazumi.core.common.AuditableEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User extends AuditableEntity {

  public static final int USERNAME_MIN_LENGTH = 3;
  public static final int USERNAME_MAX_LENGTH = 32;
  public static final int EMAIL_MAX_LENGTH = 254;
  public static final int PASSWORD_MIN_LENGTH = 8;

  /** Предел BCrypt: учитываются только первые 72 байта пароля. */
  public static final int PASSWORD_MAX_BYTES = 72;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, length = USERNAME_MAX_LENGTH)
  private String username;

  @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;
}
