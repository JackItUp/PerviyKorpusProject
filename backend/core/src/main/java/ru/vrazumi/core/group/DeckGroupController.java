package ru.vrazumi.core.group;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.vrazumi.core.auth.CurrentUserId;
import ru.vrazumi.core.group.dto.DeckGroupRequest;
import ru.vrazumi.core.group.dto.DeckGroupResponse;

@Tag(name = "Группы колод")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class DeckGroupController {

  private final DeckGroupService groupService;

  @Operation(summary = "Группы пользователя", description = "По названию, с количеством колод.")
  @GetMapping
  public List<DeckGroupResponse> findAll(@CurrentUserId Long userId) {
    return groupService.findAll(userId);
  }

  @Operation(summary = "Создать группу")
  @ApiResponse(responseCode = "201", description = "Группа создана")
  @ApiResponse(
      responseCode = "400",
      description = "Ошибка валидации",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Группа с таким названием уже есть",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping
  public ResponseEntity<DeckGroupResponse> create(
      @Valid @RequestBody DeckGroupRequest request, @CurrentUserId Long userId) {
    DeckGroupResponse group = groupService.create(request, userId);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{groupId}")
            .buildAndExpand(group.id())
            .toUri();
    return ResponseEntity.created(location).body(group);
  }

  @Operation(summary = "Группа по id")
  @ApiResponse(responseCode = "200", description = "Группа найдена")
  @ApiResponse(
      responseCode = "404",
      description = "Группы нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/{groupId}")
  public DeckGroupResponse findById(@PathVariable Long groupId, @CurrentUserId Long userId) {
    return groupService.findById(groupId, userId);
  }

  @Operation(summary = "Переименовать группу")
  @ApiResponse(responseCode = "200", description = "Группа переименована")
  @ApiResponse(
      responseCode = "404",
      description = "Группы нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Группа с таким названием уже есть",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @PutMapping("/{groupId}")
  public DeckGroupResponse rename(
      @PathVariable Long groupId,
      @Valid @RequestBody DeckGroupRequest request,
      @CurrentUserId Long userId) {
    return groupService.rename(groupId, request, userId);
  }

  @Operation(
      summary = "Удалить группу",
      description = "Колоды группы не удаляются: они остаются без группы (groupId = null).")
  @ApiResponse(responseCode = "204", description = "Группа удалена")
  @ApiResponse(
      responseCode = "404",
      description = "Группы нет",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @DeleteMapping("/{groupId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long groupId, @CurrentUserId Long userId) {
    groupService.delete(groupId, userId);
  }
}
