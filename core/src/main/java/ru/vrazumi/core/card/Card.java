package ru.vrazumi.core.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import ru.vrazumi.core.deck.Deck;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "card")
public class Card extends AuditableEntity {

  public static final int QUESTION_MAX_LENGTH = 2000;
  public static final int ANSWER_MAX_LENGTH = 2000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "deck_id", nullable = false)
  private Deck deck;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private CardType type = CardType.NORMAL;

  @Column(name = "question", nullable = false, length = QUESTION_MAX_LENGTH)
  private String question;

  @Column(name = "answer", nullable = false, length = ANSWER_MAX_LENGTH)
  private String answer;
}
