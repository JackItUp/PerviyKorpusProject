package ru.vrazumi.core.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Единая обработка ошибок API. Все ответы с ошибкой имеют формат {@link ProblemDetail} (RFC 9457).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  static final String ERRORS_PROPERTY = "errors";

  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail handleNotFound(NotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Не найдено");
    return problem;
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
    return unauthorized(ex.getMessage());
  }

  /** Например, контроллер запросил текущего пользователя, а токена нет. */
  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthentication(AuthenticationException ex) {
    return unauthorized("Требуется авторизация: передайте access-токен.");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Недостаточно прав.");
    problem.setTitle("Доступ запрещён");
    return problem;
  }

  @ExceptionHandler(BadRequestException.class)
  public ProblemDetail handleBadRequest(BadRequestException ex) {
    ProblemDetail problem = badRequest(ex.getMessage());
    if (ex.getField() != null) {
      problem.setProperty(
          ERRORS_PROPERTY, List.of(new FieldViolation(ex.getField(), ex.getMessage())));
    }
    return problem;
  }

  @ExceptionHandler(ConflictException.class)
  public ProblemDetail handleConflict(ConflictException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Конфликт");
    problem.setProperty(
        ERRORS_PROPERTY, List.of(new FieldViolation(ex.getField(), ex.getMessage())));
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    log.error("Непредвиденная ошибка при обработке запроса", ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера. Попробуйте позже.");
    problem.setTitle("Внутренняя ошибка");
    return problem;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldViolation> errors =
        Stream.concat(
                ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage())),
                ex.getBindingResult().getGlobalErrors().stream()
                    .map(
                        error ->
                            new FieldViolation(error.getObjectName(), error.getDefaultMessage())))
            .toList();
    ProblemDetail problem = badRequest("Запрос содержит ошибки валидации.");
    problem.setProperty(ERRORS_PROPERTY, errors);
    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem;
    if (ex.getMostSpecificCause() instanceof InvalidFormatException invalid
        && invalid.getTargetType() != null
        && invalid.getTargetType().isEnum()) {
      String field = fieldPath(invalid);
      String allowed =
          Arrays.stream(invalid.getTargetType().getEnumConstants())
              .map(Object::toString)
              .collect(Collectors.joining(", "));
      String message =
          "Недопустимое значение '%s'. Допустимые значения: %s"
              .formatted(invalid.getValue(), allowed);
      problem = badRequest("Недопустимое значение поля '%s'.".formatted(field));
      problem.setProperty(ERRORS_PROPERTY, List.of(new FieldViolation(field, message)));
    } else if (ex.getMostSpecificCause() instanceof MismatchedInputException mismatched
        && !mismatched.getPath().isEmpty()) {
      String field = fieldPath(mismatched);
      problem = badRequest("Поле '%s' имеет неверный тип.".formatted(field));
      problem.setProperty(
          ERRORS_PROPERTY, List.of(new FieldViolation(field, "Неверный тип значения")));
    } else {
      problem = badRequest("Тело запроса отсутствует или не является корректным JSON.");
    }
    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String name =
        ex instanceof MethodArgumentTypeMismatchException mismatch
            ? mismatch.getName()
            : ex.getPropertyName();
    ProblemDetail problem =
        badRequest(
            "Параметр '%s' имеет недопустимое значение '%s'.".formatted(name, ex.getValue()));
    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  private static ProblemDetail unauthorized(String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    problem.setTitle("Не авторизован");
    return problem;
  }

  private static ProblemDetail badRequest(String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Некорректный запрос");
    return problem;
  }

  /** Путь к полю в том же виде, что у Bean Validation: {@code options[1].text}. */
  private static String fieldPath(JacksonException ex) {
    StringBuilder path = new StringBuilder();
    for (JacksonException.Reference ref : ex.getPath()) {
      if (ref.getPropertyName() != null) {
        path.append(path.isEmpty() ? "" : ".").append(ref.getPropertyName());
      } else {
        path.append('[').append(ref.getIndex()).append(']');
      }
    }
    return path.toString();
  }
}
