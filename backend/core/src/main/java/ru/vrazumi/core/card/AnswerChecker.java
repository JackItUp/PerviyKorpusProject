package ru.vrazumi.core.card;

import java.text.Normalizer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.vrazumi.core.common.BadRequestException;

/** Проверка ответов на карточки, которые проверяет сервер. */
@Component
public class AnswerChecker {

  private static final Pattern WHITESPACE =
      Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * Сравнивает выбранные варианты с верными: полное совпадение — {@link AnswerResult#CORRECT}, хотя
   * бы один верный среди выбранных — {@link AnswerResult#PARTIAL}, иначе {@link
   * AnswerResult#INCORRECT}.
   *
   * @throws BadRequestException если выбран вариант не из этой карточки
   */
  public AnswerResult checkChoice(Card card, Collection<Long> selectedOptionIds) {
    Set<Long> selected = new HashSet<>(selectedOptionIds);
    Set<Long> all = card.getOptions().stream().map(CardOption::getId).collect(Collectors.toSet());
    if (!all.containsAll(selected)) {
      throw new BadRequestException(
          "selectedOptionIds", "Выбран вариант, которого нет у этой карточки");
    }

    Set<Long> correct = correctOptionIds(card);
    if (selected.equals(correct)) {
      return AnswerResult.CORRECT;
    }
    return selected.stream().anyMatch(correct::contains)
        ? AnswerResult.PARTIAL
        : AnswerResult.INCORRECT;
  }

  public Set<Long> correctOptionIds(Card card) {
    return card.getOptions().stream()
        .filter(CardOption::isCorrect)
        .map(CardOption::getId)
        .collect(Collectors.toSet());
  }

  /** Сравнивает введённый ответ с верным без учёта регистра и лишних пробелов. */
  public AnswerResult checkText(Card card, String answer) {
    return normalize(answer).equals(normalize(card.getAnswer()))
        ? AnswerResult.CORRECT
        : AnswerResult.INCORRECT;
  }

  /**
   * Приводит текст к виду для сравнения: Unicode NFC (одинаковые буквы, набранные по-разному),
   * пробелы по краям убраны, внутренние схлопнуты в один, нижний регистр.
   */
  static String normalize(String text) {
    String composed = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC);
    return WHITESPACE.matcher(composed).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
  }
}
