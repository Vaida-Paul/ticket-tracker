package com.example.skeleton.domain.dto;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSearchRequest {
    private TicketStatus status;
    private Priority priority;
    private Long assigneeId;
    private LocalDate dueBefore;
    private LocalDate dueAfter;
    private String titleContains;
}
