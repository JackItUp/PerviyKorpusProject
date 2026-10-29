package ru.vrazumi.core.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Пользователь")
public record UserResponse(
    @Schema(example = "1") Long id,
    @Schema(example = "ivan_petrov") String username,
    @Schema(example = "ivan@example.com") String email,
    @Schema(example = "2026-10-13T09:30:00Z") Instant createdAt) {}
