package ru.vrazumi.core.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Вариант ответа без отметки, верный ли он")
public record StudyOptionResponse(
    @Schema(example = "31") Long id, @Schema(example = "went") String text) {}
