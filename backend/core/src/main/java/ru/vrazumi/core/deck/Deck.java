package ru.vrazumi.core.deck;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deck")
public class Deck extends AuditableEntity {

  public static final int TITLE_MAX_LENGTH = 100;
  public static final int DESCRIPTION_MAX_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
  private String title;

  @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false, updatable = false)
  private User owner;
}
