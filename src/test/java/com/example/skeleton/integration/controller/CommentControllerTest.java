package com.example.skeleton.integration.controller;

import com.example.skeleton.config.SecurityProperties;
import com.example.skeleton.controller.CommentController;
import com.example.skeleton.exception.CommentNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.service.CommentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.openapitools.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("test@example.com")
                .claim("userId", 1L)
                .claim("role", "PROJECT_MANAGER")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER")),
                "test@example.com"
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void whenGetComments_thenReturns200AndList() throws Exception {
        Long ticketId = 1L;
        CommentDto dto = new CommentDto();
        dto.setId(1L);
        dto.setBody("Test comment");
        dto.setTicketId(ticketId);

        when(commentService.getCommentsByTicketId(ticketId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Test comment"))
                .andExpect(jsonPath("$[0].ticketId").value(1));

        verify(commentService).getCommentsByTicketId(ticketId);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenNoComments_thenReturns200AndEmptyList() throws Exception {
        Long ticketId = 1L;
        when(commentService.getCommentsByTicketId(ticketId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commentService).getCommentsByTicketId(ticketId);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenGetCommentsForNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        when(commentService.getCommentsByTicketId(ticketId))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(commentService).getCommentsByTicketId(ticketId);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenAddComment_thenReturns201AndComment() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "body": "New comment"
                }
                """;

        CommentDto expectedResponse = new CommentDto();
        expectedResponse.setId(1L);
        expectedResponse.setBody("New comment");
        expectedResponse.setTicketId(ticketId);

        ArgumentCaptor<CommentCreateRequest> captor = ArgumentCaptor.forClass(CommentCreateRequest.class);
        when(commentService.addComment(eq(ticketId), captor.capture(), eq(1L)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.body").value("New comment"))
                .andExpect(jsonPath("$.ticketId").value(1));

        verify(commentService).addComment(eq(ticketId), captor.capture(), eq(1L));

        CommentCreateRequest captured = captor.getValue();
        assertThat(captured.getBody()).isEqualTo("New comment");

        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenAddCommentWithBlankBody_thenReturns400() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "body": ""
                }
                """;

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("body")));

        verifyNoInteractions(commentService);
    }

    @Test
    void whenAddCommentWithMissingBody_thenReturns400() throws Exception {
        Long ticketId = 1L;
        String requestBody = "{}";

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("body")));

        verifyNoInteractions(commentService);
    }

    @Test
    void whenAddCommentToNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        String requestBody = """
                {
                    "body": "New comment"
                }
                """;

        ArgumentCaptor<CommentCreateRequest> captor = ArgumentCaptor.forClass(CommentCreateRequest.class);
        when(commentService.addComment(eq(ticketId), captor.capture(), eq(1L)))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(commentService).addComment(eq(ticketId), captor.capture(), eq(1L));

        CommentCreateRequest captured = captor.getValue();
        assertThat(captured.getBody()).isEqualTo("New comment");

        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenDeleteComment_thenReturns204() throws Exception {
        Long ticketId = 1L;
        Long commentId = 1L;
        doNothing().when(commentService).deleteComment(ticketId, commentId);

        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}", ticketId, commentId))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(ticketId, commentId);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenDeleteNonExistentComment_thenReturns404() throws Exception {
        Long ticketId = 1L;
        Long commentId = 99999L;
        doThrow(new CommentNotFoundException(commentId))
                .when(commentService).deleteComment(ticketId, commentId);

        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}", ticketId, commentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Comment with id 99999 not found"));

        verify(commentService).deleteComment(ticketId, commentId);
        verifyNoMoreInteractions(commentService);
    }
}