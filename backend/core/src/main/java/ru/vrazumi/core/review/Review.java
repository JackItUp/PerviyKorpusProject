package ru.vrazumi.core.review;

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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.vrazumi.core.card.Card;
import ru.vrazumi.core.user.User;

/** Одна оценка ответа. Записи только добавляются: из них строится история и статистика. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review")
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Кто поставил оценку. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "card_id", nullable = false, updatable = false)
  private Card card;

  @Enumerated(EnumType.STRING)
  @Column(name = "rating", nullable = false, length = 20, updatable = false)
  private Rating rating;

  @Column(name = "reviewed_at", nullable = false, updatable = false)
  private Instant reviewedAt;
}
