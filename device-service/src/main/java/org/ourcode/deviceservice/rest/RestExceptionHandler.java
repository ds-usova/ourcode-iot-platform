package org.ourcode.deviceservice.rest;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.exception.DuplicateException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String code = UUID.randomUUID().toString();
        log.error("Validation error with code {}: {}", code, ex.getMessage());

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
        log.error("Duplicate exception with code {}: {}", code, ex.getMessage());

        Error error = new Error();
        error.setCode(code);
        error.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

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
