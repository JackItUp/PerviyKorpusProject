package ru.vrazumi.core.deck;

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
}
