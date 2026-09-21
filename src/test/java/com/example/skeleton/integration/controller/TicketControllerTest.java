package com.example.skeleton.integration.controller;

import com.example.skeleton.controller.TicketController;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.service.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

@WebMvcTest(TicketController.class)
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;


    @Test
    void whenTicketExists_thenReturns200AndTicket() throws Exception {
        Long ticketId = 1L;
        TicketDto response = new TicketDto();
        response.setId(ticketId);
        response.setTitle("Fix login");
        response.setStatus(TicketStatus.OPEN);
        response.setPriority(Priority.HIGH);
        response.setProjectId(1L);

        when(ticketService.getTicketById(ticketId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix login"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        verify(ticketService).getTicketById(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenTicketDoesNotExist_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        when(ticketService.getTicketById(ticketId))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(ticketService).getTicketById(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenGetTickets_thenReturns200AndPage() throws Exception {
        TicketDto dto = new TicketDto();
        dto.setId(1L);
        dto.setTitle("Test Ticket");
        dto.setStatus(TicketStatus.OPEN);
        dto.setPriority(Priority.HIGH);
        dto.setProjectId(1L);

        org.springframework.data.domain.Page<TicketDto> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(dto));

        when(ticketService.searchTickets(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(10), eq("priority"), eq("desc")
        )).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/tickets")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Ticket"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(ticketService).searchTickets(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(10), eq("priority"), eq("desc")
        );
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenNoTickets_thenReturns200AndEmptyPage() throws Exception {
        org.springframework.data.domain.Page<TicketDto> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());

        when(ticketService.searchTickets(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("priority"), eq("desc")
        )).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/tickets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(ticketService).searchTickets(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("priority"), eq("desc")
        );
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenCreateTicket_thenReturns201AndTicket() throws Exception {
        String requestBody = """
                {
                    "title": "New Ticket",
                    "description": "New Description",
                    "priority": "HIGH",
                    "projectId": 1
                }
                """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(1L);
        expectedResponse.setTitle("New Ticket");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketCreateRequest> captor = ArgumentCaptor.forClass(TicketCreateRequest.class);
        when(ticketService.createTicket(captor.capture())).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Ticket"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        verify(ticketService).createTicket(captor.capture());

        TicketCreateRequest captured = captor.getValue();
        assertThat(captured.getTitle()).isEqualTo("New Ticket");
        assertThat(captured.getDescription()).isEqualTo("New Description");
        assertThat(captured.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(captured.getProjectId()).isEqualTo(1L);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenCreateTicketWithoutTitle_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "priority": "HIGH",
                    "projectId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenCreateTicketWithBlankTitle_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "title": "",
                    "priority": "HIGH",
                    "projectId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenCreateTicketWithMissingPriority_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "title": "New Ticket",
                    "projectId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("priority")));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenCreateTicketWithNonExistentProject_thenReturns404() throws Exception {
        String requestBody = """
                {
                    "title": "New Ticket",
                    "priority": "HIGH",
                    "projectId": 99999
                }
                """;

        ArgumentCaptor<TicketCreateRequest> captor = ArgumentCaptor.forClass(TicketCreateRequest.class);
        when(ticketService.createTicket(captor.capture()))
                .thenThrow(new ProjectNotFoundException(99999L));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"));

        verify(ticketService).createTicket(captor.capture());

        TicketCreateRequest captured = captor.getValue();
        assertThat(captured.getProjectId()).isEqualTo(99999L);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPutTicket_thenReturns200AndTicket() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "title": "Updated Ticket",
                    "status": "IN_PROGRESS",
                    "priority": "URGENT"
                }
                """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Updated Ticket");
        expectedResponse.setStatus(TicketStatus.IN_PROGRESS);
        expectedResponse.setPriority(Priority.URGENT);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPutRequest> captor = ArgumentCaptor.forClass(TicketPutRequest.class);
        when(ticketService.updateTicket(eq(ticketId), captor.capture()))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Ticket"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("URGENT"));

        verify(ticketService).updateTicket(eq(ticketId), captor.capture());

        TicketPutRequest captured = captor.getValue();
        assertThat(captured.getTitle()).isEqualTo("Updated Ticket");
        assertThat(captured.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(captured.getPriority()).isEqualTo(Priority.URGENT);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPutTicketWithoutRequiredFields_thenReturns400() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "title": "Updated Ticket"
                }
                """;

        mockMvc.perform(put("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenPutNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        String requestBody = """
                {
                    "title": "Updated Ticket",
                    "status": "IN_PROGRESS",
                    "priority": "HIGH"
                }
                """;

        ArgumentCaptor<TicketPutRequest> captor = ArgumentCaptor.forClass(TicketPutRequest.class);
        when(ticketService.updateTicket(eq(ticketId), captor.capture()))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(put("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(ticketService).updateTicket(eq(ticketId), captor.capture());

        TicketPutRequest captured = captor.getValue();
        assertThat(captured.getTitle()).isEqualTo("Updated Ticket");
        assertThat(captured.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicket_thenReturns200AndTicket() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "status": "DONE"
                }
                """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Test Ticket");
        expectedResponse.setStatus(TicketStatus.DONE);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture()))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.title").value("Test Ticket"));

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo(TicketStatus.DONE);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        String requestBody = """
                {
                    "status": "DONE"
                }
                """;

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture()))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo(TicketStatus.DONE);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketWithDescription_thenDescriptionIsPresentWithValue() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "description": "Updated description"
                }
                """;
        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Test Ticket");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setDescription("Updated description");
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture())).thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();
        assertThat(captured.getDescription().isPresent()).isTrue();
        assertThat(captured.getDescription().get()).isEqualTo("Updated description");

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketWithNullDescription_thenDescriptionIsPresentWithNull() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "description": null
                }
                """;
        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Test Ticket");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture())).thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();
        assertThat(captured.getDescription().isPresent()).isTrue();
        assertThat(captured.getDescription().get()).isNull();

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketWithoutDescription_thenDescriptionIsUndefined() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
            {
                "title": "Only title changed"
            }
            """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Only title changed");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture()))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();
        assertThat(captured.getDescription().isPresent()).isFalse();
        assertThat(captured.getDueDate().isPresent()).isFalse();
        assertThat(captured.getAssigneeId().isPresent()).isFalse();

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketWithAllJsonNullableFields_thenAllCapturedWithValues() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
            {
                "description": "New description",
                "dueDate": "2026-12-31T23:59:59Z",
                "assigneeId": 5
            }
            """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Test Ticket");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture()))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();

        assertThat(captured.getDescription().isPresent()).isTrue();
        assertThat(captured.getDescription().get()).isEqualTo("New description");

        assertThat(captured.getDueDate().isPresent()).isTrue();
        assertThat(captured.getDueDate().get()).isNotNull();

        assertThat(captured.getAssigneeId().isPresent()).isTrue();
        assertThat(captured.getAssigneeId().get()).isEqualTo(5L);

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketWithExplicitNullsForJsonNullableFields_thenAllPresentWithNull() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
            {
                "description": null,
                "dueDate": null,
                "assigneeId": null
            }
            """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Test Ticket");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture()))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();

        assertThat(captured.getDescription().isPresent()).isTrue();
        assertThat(captured.getDescription().get()).isNull();

        assertThat(captured.getDueDate().isPresent()).isTrue();
        assertThat(captured.getDueDate().get()).isNull();

        assertThat(captured.getAssigneeId().isPresent()).isTrue();
        assertThat(captured.getAssigneeId().get()).isNull();

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenPatchTicketWithNullAssigneeId_thenAssigneeIdIsPresentWithNull() throws Exception {
        Long ticketId = 1L;
        String requestBody = """
                {
                    "assigneeId": null
                }
                """;

        TicketDto expectedResponse = new TicketDto();
        expectedResponse.setId(ticketId);
        expectedResponse.setTitle("Test Ticket");
        expectedResponse.setStatus(TicketStatus.OPEN);
        expectedResponse.setPriority(Priority.HIGH);
        expectedResponse.setProjectId(1L);

        ArgumentCaptor<TicketPatchRequest> captor = ArgumentCaptor.forClass(TicketPatchRequest.class);
        when(ticketService.partialUpdateTicket(eq(ticketId), captor.capture()))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ticketService).partialUpdateTicket(eq(ticketId), captor.capture());

        TicketPatchRequest captured = captor.getValue();
        assertThat(captured.getAssigneeId().isPresent()).isTrue();
        assertThat(captured.getAssigneeId().get()).isNull();

        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenDeleteTicket_thenReturns204() throws Exception {
        Long ticketId = 1L;
        doNothing().when(ticketService).deleteTicket(ticketId);

        mockMvc.perform(delete("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isNoContent());

        verify(ticketService).deleteTicket(ticketId);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    void whenDeleteNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        doThrow(new TicketNotFoundException(ticketId))
                .when(ticketService).deleteTicket(ticketId);

        mockMvc.perform(delete("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        verify(ticketService).deleteTicket(ticketId);
        verifyNoMoreInteractions(ticketService);
    }
}