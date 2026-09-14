package com.example.skeleton.unit.exception;

import com.example.skeleton.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/tickets");
    }


    @Test
    void whenTicketNotFound_thenReturns404() {
        TicketNotFoundException ex = new TicketNotFoundException(99999L);

        ResponseEntity<ErrorResponse> response = handler.handleTicketNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();

        ErrorResponse body = response.getBody();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("Not Found");
        assertThat(body.getMessage()).isEqualTo("Ticket with id 99999 not found");
        assertThat(body.getPath()).isEqualTo("/api/v1/tickets");
        assertThat(body.getTimestamp()).isNotNull();

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }


    @Test
    void whenProjectNotFound_thenReturns404() {
        ProjectNotFoundException ex = new ProjectNotFoundException(99999L);

        ResponseEntity<ErrorResponse> response = handler.handleProjectNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Project with id 99999 not found");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }

    @Test
    void whenUserNotFound_thenReturns404() {
        UserNotFoundException ex = new UserNotFoundException(99999L);

        ResponseEntity<ErrorResponse> response = handler.handleUserNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("User with id 99999 not found");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }


    @Test
    void whenCommentNotFound_thenReturns404() {
        CommentNotFoundException ex = new CommentNotFoundException(99999L);

        ResponseEntity<ErrorResponse> response = handler.handleCommentNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Comment with id 99999 not found");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }


    @Test
    void whenIllegalArgument_thenReturns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }

    @Test
    void whenValidationErrors_thenReturns400WithFieldDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError titleError = new FieldError("ticketCreateRequest", "title", "must not be blank");
        FieldError priorityError = new FieldError("ticketCreateRequest", "priority", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(titleError, priorityError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getMessage())
                .contains("title: must not be blank")
                .contains("priority: must not be null");

        verify(ex).getBindingResult();
        verify(bindingResult).getFieldErrors();
        verify(request).getRequestURI();
        verifyNoMoreInteractions(request, ex, bindingResult);
    }

    @Test
    void whenMalformedJson_thenReturns400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Malformed JSON", null, null);

        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Malformed Request");
        assertThat(response.getBody().getMessage())
                .contains("Malformed JSON request or invalid value");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }

    @Test
    void whenDataIntegrityViolation_thenReturns409() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("FK violation");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage())
                .contains("Data integrity violation");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }

    @Test
    void whenUnexpectedException_thenReturns500() {
        RuntimeException ex = new RuntimeException("Something broke");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");

        verify(request).getRequestURI();
        verifyNoMoreInteractions(request);
    }
}