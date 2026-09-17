package com.example.skeleton.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed at {}: {}", request.getRequestURI(), message);

        return buildErrorResponse(
                message,
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                request
        );
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRequestException ex,
            HttpServletRequest request) {

        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed request at {}", request.getRequestURI(), ex);

        return buildErrorResponse(
                "Malformed JSON request or invalid value. Check enum values and JSON format.",
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                request
        );
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(
            TicketNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Ticket not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                "Not Found",
                request
        );
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(
            ProjectNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Project not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                "Not Found",
                request
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        log.warn("User not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                "Not Found",
                request
        );
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(
            CommentNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Comment not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                "Not Found",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Data integrity violation at {}", request.getRequestURI(), ex);

        return buildErrorResponse(
                "Data integrity violation. Check foreign keys and unique constraints.",
                HttpStatus.CONFLICT,
                "Conflict",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}", request.getRequestURI(), ex);

        return buildErrorResponse(
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            String message,
            HttpStatus status,
            String error,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse();
        response.setTimestamp(OffsetDateTime.now());
        response.setStatus(status.value());
        response.setError(error);
        response.setMessage(message);
        response.setPath(request.getRequestURI());

        return ResponseEntity.status(status).body(response);
    }
}