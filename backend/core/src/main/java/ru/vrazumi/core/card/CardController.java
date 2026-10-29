package ru.vrazumi.core.card;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.vrazumi.core.auth.CurrentUserId;
import ru.vrazumi.core.card.dto.CardResponse;
import ru.vrazumi.core.card.dto.CreateCardRequest;

@Tag(name = "Карточки")
@RestController
@RequestMapping("/api/decks/{deckId}/cards")
@RequiredArgsConstructor
public class CardController {

  private final CardService cardService;

  @Operation(
      summary = "Добавить карточку в колоду",
      description = "Тип карточки всегда NORMAL. Location указывает на /api/cards/{cardId}.")
  @ApiResponse(responseCode = "201", description = "Карточка создана")
  @ApiResponse(
      responseCode = "400",
      description = "Ошибка валидации",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Колоды нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping
  public ResponseEntity<CardResponse> create(
      @PathVariable Long deckId,
      @Valid @RequestBody CreateCardRequest request,
      @CurrentUserId Long userId) {
    CardResponse card = cardService.create(deckId, request, userId);
    URI location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/cards/{cardId}")
            .buildAndExpand(card.id())
            .toUri();
    return ResponseEntity.created(location).body(card);
  }

  @Operation(summary = "Карточки колоды", description = "В порядке добавления.")
  @ApiResponse(responseCode = "200", description = "Список карточек, возможно пустой")
  @ApiResponse(
      responseCode = "404",
      description = "Колоды нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping
  public List<CardResponse> findByDeck(@PathVariable Long deckId, @CurrentUserId Long userId) {
    return cardService.findByDeck(deckId, userId);
  }
}
