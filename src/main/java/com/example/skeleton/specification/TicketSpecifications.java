package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Ticket;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TicketSpecifications {

    private TicketSpecifications() {}

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(Priority priority) {
        if (priority == null) return null;
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasAssignee(Long assigneeId) {
        if (assigneeId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Ticket> dueBefore(LocalDate date) {
        if (date == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("dueDate"), date.atStartOfDay());
    }

    public static Specification<Ticket> dueAfter(LocalDate date) {
        if (date == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), date.atStartOfDay());
    }

    public static Specification<Ticket> titleContains(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + text.toLowerCase() + "%");
    }
}