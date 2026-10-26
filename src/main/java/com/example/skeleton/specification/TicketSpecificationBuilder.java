package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Ticket;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketSpecificationBuilder {

    private final List<Specification<Ticket>> specifications = new ArrayList<>();

    public TicketSpecificationBuilder withStatus(TicketStatus status) {
        Specification<Ticket> spec = TicketSpecifications.hasStatus(status);
        if (spec != null) specifications.add(spec);
        return this;
    }

    public TicketSpecificationBuilder withPriority(Priority priority) {
        Specification<Ticket> spec = TicketSpecifications.hasPriority(priority);
        if (spec != null) specifications.add(spec);
        return this;
    }

    public TicketSpecificationBuilder withAssignee(Long assigneeId) {
        Specification<Ticket> spec = TicketSpecifications.hasAssignee(assigneeId);
        if (spec != null) specifications.add(spec);
        return this;
    }

    public TicketSpecificationBuilder withDueBefore(LocalDate date) {
        Specification<Ticket> spec = TicketSpecifications.dueBefore(date);
        if (spec != null) specifications.add(spec);
        return this;
    }

    public TicketSpecificationBuilder withDueAfter(LocalDate date) {
        Specification<Ticket> spec = TicketSpecifications.dueAfter(date);
        if (spec != null) specifications.add(spec);
        return this;
    }

    public TicketSpecificationBuilder withStatusNotIn(
            List<com.example.skeleton.domain.enums.TicketStatus> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            specifications.add((root, query, cb) -> root.get("status").in(statuses).not());
        }
        return this;
    }

    public TicketSpecificationBuilder withDueBeforeOrEqual(OffsetDateTime cutoff) {
        if (cutoff != null) {
            specifications.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("dueDate"), cutoff));
        }
        return this;
    }

    public TicketSpecificationBuilder withTitleContains(String text) {
        Specification<Ticket> spec = TicketSpecifications.titleContains(text);
        if (spec != null) specifications.add(spec);
        return this;
    }
        public Specification<Ticket> build() {
        return Specification.allOf(specifications);
    }

    public static TicketSpecificationBuilder builder() {
        return new TicketSpecificationBuilder();
    }
}