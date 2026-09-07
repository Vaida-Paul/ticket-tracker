package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Ticket;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TicketSpecificationBuilder {

    private Specification<Ticket> specification;

    public TicketSpecificationBuilder() {
        this.specification = null;
    }

    public TicketSpecificationBuilder withStatus(TicketStatus status) {
        Specification<Ticket> spec = TicketSpecifications.hasStatus(status);
        this.specification = combineSpecifications(this.specification, spec);
        return this;
    }

    public TicketSpecificationBuilder withPriority(Priority priority) {
        Specification<Ticket> spec = TicketSpecifications.hasPriority(priority);
        this.specification = combineSpecifications(this.specification, spec);
        return this;
    }

    public TicketSpecificationBuilder withAssignee(Long assigneeId) {
        Specification<Ticket> spec = TicketSpecifications.hasAssignee(assigneeId);
        this.specification = combineSpecifications(this.specification, spec);
        return this;
    }

    public TicketSpecificationBuilder withDueBefore(LocalDate date) {
        Specification<Ticket> spec = TicketSpecifications.dueBefore(date);
        this.specification = combineSpecifications(this.specification, spec);
        return this;
    }

    public TicketSpecificationBuilder withDueAfter(LocalDate date) {
        Specification<Ticket> spec = TicketSpecifications.dueAfter(date);
        this.specification = combineSpecifications(this.specification, spec);
        return this;
    }

    public TicketSpecificationBuilder withTitleContains(String text) {
        Specification<Ticket> spec = TicketSpecifications.titleContains(text);
        this.specification = combineSpecifications(this.specification, spec);
        return this;
    }

    private Specification<Ticket> combineSpecifications(Specification<Ticket> existing, Specification<Ticket> newSpec) {
        if (existing == null) {
            return newSpec;
        }
        if (newSpec == null) {
            return existing;
        }
        return existing.and(newSpec);
    }

    public Specification<Ticket> build() {
        return specification == null ? Specification.allOf() : specification;
    }

    public TicketSpecificationBuilder reset() {
        this.specification = null;
        return this;
    }

    public static TicketSpecificationBuilder builder() {
        return new TicketSpecificationBuilder();
    }
}