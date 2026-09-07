package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Ticket;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TicketSpecificationBuilder {

    private final List<Specification<Ticket>> specifications = new ArrayList<>();

    public TicketSpecificationBuilder withStatus(TicketStatus status) {
        specifications.add(TicketSpecifications.hasStatus(status));
        return this;
    }

    public TicketSpecificationBuilder withPriority(Priority priority) {
        specifications.add(TicketSpecifications.hasPriority(priority));
        return this;
    }

    public TicketSpecificationBuilder withAssignee(Long assigneeId) {
        specifications.add(TicketSpecifications.hasAssignee(assigneeId));
        return this;
    }

    public TicketSpecificationBuilder withDueBefore(LocalDate date) {
        specifications.add(TicketSpecifications.dueBefore(date));
        return this;
    }

    public TicketSpecificationBuilder withDueAfter(LocalDate date) {
        specifications.add(TicketSpecifications.dueAfter(date));
        return this;
    }

    public TicketSpecificationBuilder withTitleContains(String text) {
        specifications.add(TicketSpecifications.titleContains(text));
        return this;
    }

    public Specification<Ticket> build() {
        return Specification.allOf(specifications);
    }

    public static TicketSpecificationBuilder builder() {
        return new TicketSpecificationBuilder();
    }
}