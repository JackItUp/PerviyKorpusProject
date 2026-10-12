package ru.vrazumi.core.common;

/** Ошибка валидации одного поля в теле ответа 400. */
public record FieldViolation(String field, String message) {}
