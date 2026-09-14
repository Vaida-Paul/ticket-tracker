package com.example.skeleton.integration.controller;

import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.mapper.TicketMapper;
import com.example.skeleton.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketMapper ticketMapper;

    @Test
    void whenTicketExists_thenReturns200AndTicket() throws Exception {
        Long ticketId = 1L;
        TicketDto response = new TicketDto();
        response.setId(ticketId);
        response.setTitle("Fix login");
        response.setStatus(TicketStatus.OPEN);
        response.setPriority(Priority.HIGH);
        response.setProjectId(1L);

        com.example.skeleton.domain.model.Ticket mockEntity = new com.example.skeleton.domain.model.Ticket();

        when(ticketService.getTicketById(ticketId)).thenReturn(mockEntity);
        when(ticketMapper.toDto(mockEntity)).thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/tickets/{id}", ticketId)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix login"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        verify(ticketService).getTicketById(ticketId);
        verify(ticketMapper).toDto(mockEntity);
    }

    @Test
    void whenTicketDoesNotExist_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        when(ticketService.getTicketById(ticketId))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(
                        get("/api/v1/tickets/{id}", ticketId)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/tickets/99999"));

        verify(ticketService).getTicketById(ticketId);
    }

    @Test
    void whenGetTickets_thenReturns200AndPage() throws Exception {
        TicketDto dto = new TicketDto();
        dto.setId(1L);
        dto.setTitle("Test Ticket");
        dto.setStatus(TicketStatus.OPEN);
        dto.setPriority(Priority.HIGH);
        dto.setProjectId(1L);

        com.example.skeleton.domain.model.Ticket mockEntity = new com.example.skeleton.domain.model.Ticket();
        org.springframework.data.domain.Page<com.example.skeleton.domain.model.Ticket> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(mockEntity));

        when(ticketService.searchTickets(any(), any())).thenReturn(mockPage);
        when(ticketMapper.toDto(mockEntity)).thenReturn(dto);

        mockMvc.perform(
                        get("/api/v1/tickets")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Ticket"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));

        verify(ticketService).searchTickets(any(), any());
    }

    @Test
    void whenNoTickets_thenReturns200AndEmptyPage() throws Exception {
        org.springframework.data.domain.Page<com.example.skeleton.domain.model.Ticket> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());

        when(ticketService.searchTickets(any(), any())).thenReturn(emptyPage);

        mockMvc.perform(
                        get("/api/v1/tickets")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(ticketService).searchTickets(any(), any());
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

        com.example.skeleton.domain.model.Ticket mockEntity = new com.example.skeleton.domain.model.Ticket();

        when(ticketService.createTicket(any(TicketCreateRequest.class))).thenReturn(mockEntity);
        when(ticketMapper.toDto(mockEntity)).thenReturn(expectedResponse);

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Ticket"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        verify(ticketService).createTicket(any(TicketCreateRequest.class));
        verify(ticketMapper).toDto(mockEntity);
    }

    @Test
    void whenCreateTicketWithoutTitle_thenReturns400WithValidationError() throws Exception {
        String requestBody = """
                {
                    "priority": "HIGH",
                    "projectId": 1
                }
                """;

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")))
                .andExpect(jsonPath("$.path").value("/api/v1/tickets"));

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

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
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

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("priority")));

        verifyNoInteractions(ticketService);
    }

    @Test
    void whenCreateTicketWithMissingProjectId_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "title": "New Ticket",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("projectId")));

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

        when(ticketService.createTicket(any(TicketCreateRequest.class)))
                .thenThrow(new ProjectNotFoundException(99999L));

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"));
    }

    @Test
    void whenUpdateTicket_thenReturns200AndTicket() throws Exception {
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

        com.example.skeleton.domain.model.Ticket mockEntity = new com.example.skeleton.domain.model.Ticket();

        when(ticketService.updateTicket(eq(ticketId), any(TicketUpdateRequest.class))).thenReturn(mockEntity);
        when(ticketMapper.toDto(mockEntity)).thenReturn(expectedResponse);

        mockMvc.perform(
                        put("/api/v1/tickets/{id}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Ticket"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("URGENT"));

        verify(ticketService).updateTicket(eq(ticketId), any(TicketUpdateRequest.class));
    }

    @Test
    void whenUpdateNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        String requestBody = """
                {
                    "title": "Updated Ticket",
                    "status": "IN_PROGRESS"
                }
                """;

        when(ticketService.updateTicket(eq(ticketId), any(TicketUpdateRequest.class)))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(
                        put("/api/v1/tickets/{id}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));
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

        com.example.skeleton.domain.model.Ticket mockEntity = new com.example.skeleton.domain.model.Ticket();

        when(ticketService.partialUpdateTicket(eq(ticketId), any(TicketUpdateRequest.class))).thenReturn(mockEntity);
        when(ticketMapper.toDto(mockEntity)).thenReturn(expectedResponse);

        mockMvc.perform(
                        patch("/api/v1/tickets/{id}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        verify(ticketService).partialUpdateTicket(eq(ticketId), any(TicketUpdateRequest.class));
    }

    @Test
    void whenPatchNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        String requestBody = """
                {
                    "status": "DONE"
                }
                """;

        when(ticketService.partialUpdateTicket(eq(ticketId), any(TicketUpdateRequest.class)))
                .thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(
                        patch("/api/v1/tickets/{id}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));
    }

    @Test
    void whenDeleteTicket_thenReturns204() throws Exception {
        Long ticketId = 1L;
        doNothing().when(ticketService).deleteTicket(ticketId);

        mockMvc.perform(
                        delete("/api/v1/tickets/{id}", ticketId)
                )
                .andExpect(status().isNoContent());

        verify(ticketService).deleteTicket(ticketId);
    }

    @Test
    void whenDeleteNonExistentTicket_thenReturns404() throws Exception {
        Long ticketId = 99999L;
        doThrow(new TicketNotFoundException(ticketId))
                .when(ticketService).deleteTicket(ticketId);

        mockMvc.perform(
                        delete("/api/v1/tickets/{id}", ticketId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));
    }
}