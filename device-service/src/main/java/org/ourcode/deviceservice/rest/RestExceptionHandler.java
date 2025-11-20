package org.ourcode.deviceservice.rest;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.exception.BadRequestException;
import org.ourcode.deviceservice.api.exception.DuplicateException;
import org.ourcode.deviceservice.api.exception.NotFoundException;
import org.ourcode.deviceservice.api.exception.PersistenceException;
import org.ourcode.rest.model.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    // Client errors (4xx)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String code = UUID.randomUUID().toString();
        log.debug("Validation error with code {}: {}", code, ex.getMessage());

        String invalidFields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        Error error = new Error();
        error.setCode(code);
        error.setMessage("Validation failed: " + invalidFields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<Error> handleDuplicateException(DuplicateException ex) {
        String code = UUID.randomUUID().toString();
        log.debug("Duplicate exception with code {}: {}", code, ex.getMessage());

        Error error = new Error();
        error.setCode(code);
        error.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Error> handleNotFoundException(NotFoundException ex) {
        String code = UUID.randomUUID().toString();
        log.debug("Resource not found {}: {}", code, ex.getMessage(), ex);

        Error error = new Error();
        error.setCode(code);
        error.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Error> handleBadRequestException(BadRequestException ex) {
        String code = UUID.randomUUID().toString();
        log.debug("Bad Request {}: {}", code, ex.getMessage(), ex);

        Error error = new Error();
        error.setCode(code);
        error.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Error> handleConstraintViolationException(ConstraintViolationException ex) {
        String code = UUID.randomUUID().toString();
        log.debug("Validation error {}: {}", code, ex.getMessage(), ex);

        String invalidFields = ex.getConstraintViolations().stream()
                .map(it -> {
                    String path = "";
                    for (var node : it.getPropertyPath()) {
                        path = node.getName();
                    }
                    return "%s %s".formatted(path, it.getMessage());
                })
                .collect(Collectors.joining(", "));

        Error error = new Error();
        error.setCode(code);
        error.setMessage("Validation failed: " + invalidFields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Server errors (5xx)
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Error> handleInternalExceptions(PersistenceException ex) {
        String code = UUID.randomUUID().toString();
        log.error("Internal exception has occurred {}: {}", code, ex.getMessage(), ex);

        Error error = new Error();
        error.setCode(code);
        error.setMessage("An internal error occurred. Please contact support if the issue persists");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleAll(Exception ex) {
        String code = UUID.randomUUID().toString();
        log.error("Unhandled exception type ({}) {}: {}", ex.getClass().getName(), code, ex.getMessage(), ex);

        Error error = new Error();
        error.setCode(code);
        error.setMessage("An internal error occurred. Please contact support if the issue persists");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
