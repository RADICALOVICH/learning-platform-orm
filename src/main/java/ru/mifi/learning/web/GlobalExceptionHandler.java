package ru.mifi.learning.web;

import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.InvalidPageRequestException;
import ru.mifi.learning.exception.NotFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    record FieldIssue(String field, String message) {}

    private static ProblemDetail problem(HttpStatusCode status, String code, String message) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, message);
        result.setProperty("code", code);
        if (status.value() == 409 || status.value() == 422) {
            log.warn("Ошибка API: status={}, code={}", status.value(), code);
        }
        return result;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldIssue(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail result = problem(status, "VALIDATION_FAILED", "Проверьте поля запроса");
        result.setProperty("errors", issues);
        return handleExceptionInternal(ex, result, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.isForReturnValue()) {
            return handleExceptionInternal(ex, unexpected(ex), headers, status, request);
        }
        var result = problem(status, "VALIDATION_FAILED", "Проверьте параметры запроса");
        result.setProperty(
                "errors",
                ex.getParameterValidationResults().stream()
                        .flatMap(parameter -> parameter.getResolvableErrors().stream()
                                .map(error -> new FieldIssue(
                                        error instanceof FieldError fieldError
                                                ? fieldError.getField()
                                                : parameter.getMethodParameter().getParameterName(),
                                        error.getDefaultMessage())))
                        .toList());
        return handleExceptionInternal(ex, result, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(
                ex,
                problem(status, "MALFORMED_REQUEST", "Некорректное значение параметра запроса"),
                headers,
                status,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(
                ex, problem(status, "MALFORMED_REQUEST", "Проверьте JSON и типы полей"), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail detail
                && (detail.getProperties() == null || !detail.getProperties().containsKey("code"))) {
            detail.setProperty("code", "HTTP_" + status.value());
            detail.setDetail(
                    switch (status.value()) {
                        case 404 -> "Запрошенный ресурс не найден";
                        case 405 -> "Метод запроса не поддерживается";
                        case 415 -> "Формат содержимого не поддерживается";
                        default -> "Не удалось обработать запрос";
                    });
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail business(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail invalidSort() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Указано неизвестное поле сортировки");
    }

    @ExceptionHandler(InvalidPageRequestException.class)
    public ProblemDetail invalidPageRequest(InvalidPageRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail constraint(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return switch (violation.getConstraintName()) {
                    case "uk_users_email" ->
                        problem(HttpStatus.CONFLICT, "USER_EMAIL_TAKEN", "Этот email уже используется");
                    case "uk_categories_name" ->
                        problem(HttpStatus.CONFLICT, "CATEGORY_NAME_TAKEN", "Категория с таким именем уже существует");
                    case "uk_tags_name" ->
                        problem(HttpStatus.CONFLICT, "TAG_NAME_TAKEN", "Тег с таким именем уже существует");
                    default -> constraintConflict();
                };
            }
        }
        return constraintConflict();
    }

    private ProblemDetail constraintConflict() {
        return problem(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", "Операция нарушает ограничение целостности данных");
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail concurrent() {
        return problem(
                HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Данные изменились одновременно. Повторите запрос");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex) {
        // При конфликте SERIALIZABLE PostgreSQL может отклонить сам commit.
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "40001".equals(sql.getSQLState())) {
                return concurrent();
            }
        }
        log.error("Внутренняя ошибка API", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Внутренняя ошибка сервера");
    }
}
