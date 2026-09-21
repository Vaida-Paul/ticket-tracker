package com.example.skeleton.repository;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByPriority(Priority priority);

    List<Ticket> findByProject(Project project);

    List<Ticket> findByAssignee(User assignee);

    long countByAssigneeId(Long userId);

    boolean existsByProjectId(Long projectId);
}