package ru.vrazumi.core.deck;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.vrazumi.core.auth.CurrentUserId;
import ru.vrazumi.core.deck.dto.CreateDeckRequest;
import ru.vrazumi.core.deck.dto.DeckResponse;
import ru.vrazumi.core.deck.dto.MoveDeckRequest;

@Tag(name = "Колоды")
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

  private final DeckService deckService;

  @Operation(summary = "Создать колоду")
  @ApiResponse(responseCode = "201", description = "Колода создана")
  @ApiResponse(
      responseCode = "400",
      description = "Ошибка валидации",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping
  public ResponseEntity<DeckResponse> create(
      @Valid @RequestBody CreateDeckRequest request, @CurrentUserId Long userId) {
    DeckResponse deck = deckService.create(request, userId);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{deckId}")
            .buildAndExpand(deck.id())
            .toUri();
    return ResponseEntity.created(location).body(deck);
  }

  @Operation(summary = "Список колод", description = "Новые колоды первыми.")
  @GetMapping
  public List<DeckResponse> findAll(@CurrentUserId Long userId) {
    return deckService.findAll(userId);
  }

  @Operation(summary = "Колода по id")
  @ApiResponse(responseCode = "200", description = "Колода найдена")
  @ApiResponse(
      responseCode = "404",
      description = "Колоды нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/{deckId}")
  public DeckResponse findById(@PathVariable Long deckId, @CurrentUserId Long userId) {
    return deckService.findById(deckId, userId);
  }

  @Operation(
      summary = "Перенести колоду в группу",
      description = "groupId = null убирает колоду из группы.")
  @ApiResponse(responseCode = "200", description = "Колода перенесена")
  @ApiResponse(
      responseCode = "404",
      description = "Колоды или группы нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping("/{deckId}/group")
  public DeckResponse moveToGroup(
      @PathVariable Long deckId, @RequestBody MoveDeckRequest request, @CurrentUserId Long userId) {
    return deckService.moveToGroup(deckId, request.groupId(), userId);
  }
}
