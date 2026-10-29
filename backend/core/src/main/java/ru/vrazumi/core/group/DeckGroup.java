package ru.vrazumi.core.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.vrazumi.core.common.AuditableEntity;
import ru.vrazumi.core.user.User;

/** Группа колод пользователя: предмет или тема. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deck_group")
public class DeckGroup extends AuditableEntity {

  public static final int NAME_MAX_LENGTH = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false, updatable = false)
  private User owner;

  @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
  private String name;
}
