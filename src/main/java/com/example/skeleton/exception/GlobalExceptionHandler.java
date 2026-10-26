package com.example.skeleton.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

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

        log.warn("[VALIDATION ERROR] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                message);

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

        log.warn("[BAD REQUEST] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

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

        log.warn("[MALFORMED REQUEST] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

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

        log.warn("[NOT FOUND] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

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

        log.warn("[NOT FOUND] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

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

        log.warn("[NOT FOUND] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

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

        log.warn("[NOT FOUND] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

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

        log.error("[DATA INTEGRITY] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),
                ex);

        return buildErrorResponse(
                "Data integrity violation. Check foreign keys and unique constraints.",
                HttpStatus.CONFLICT,
                "Conflict",
                request
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("[CONFLICT] {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                "Conflict",
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("[UNAUTHORIZED] {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        log.warn("[METHOD NOT ALLOWED] {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                "Method " + request.getMethod() + " is not supported for this endpoint",
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                request
        );
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[ACCESS DENIED] {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                "Access denied",
                HttpStatus.FORBIDDEN,
                "Forbidden",
                request
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        log.warn("[INVALID TOKEN] {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("[UNEXPECTED ERROR] {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),
                ex);

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