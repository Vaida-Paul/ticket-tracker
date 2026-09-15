package com.example.skeleton.integration.controller;

import com.example.skeleton.exception.CommentNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.openapitools.model.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;


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

        when(commentService.addComment(eq(ticketId), any(CommentCreateRequest.class), any()))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.body").value("New comment"))
                .andExpect(jsonPath("$.ticketId").value(1));

        verify(commentService).addComment(eq(ticketId), any(CommentCreateRequest.class), any());
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

        when(commentService.addComment(eq(ticketId), any(CommentCreateRequest.class), any()))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(commentService).addComment(eq(ticketId), any(CommentCreateRequest.class), any());
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenDeleteComment_thenReturns204() throws Exception {
        Long ticketId = 1L;
        Long commentId = 1L;
        doNothing().when(commentService).deleteComment(commentId);

        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}", ticketId, commentId))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(commentId);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void whenDeleteNonExistentComment_thenReturns404() throws Exception {
        Long ticketId = 1L;
        Long commentId = 99999L;
        doThrow(new CommentNotFoundException(commentId))
                .when(commentService).deleteComment(commentId);

        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}", ticketId, commentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Comment with id 99999 not found"));

        verify(commentService).deleteComment(commentId);
        verifyNoMoreInteractions(commentService);
    }
}