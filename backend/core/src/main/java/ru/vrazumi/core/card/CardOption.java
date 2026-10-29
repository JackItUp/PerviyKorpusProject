package ru.vrazumi.core.card;

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

/** Вариант ответа карточки {@link CardType#MULTIPLE_CHOICE}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "card_option")
public class CardOption {

  public static final int TEXT_MAX_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "card_id", nullable = false, updatable = false)
  private Card card;

  @Column(name = "position", nullable = false)
  private int position;

  @Column(name = "text", nullable = false, length = TEXT_MAX_LENGTH)
  private String text;

  @Column(name = "correct", nullable = false)
  private boolean correct;
}
