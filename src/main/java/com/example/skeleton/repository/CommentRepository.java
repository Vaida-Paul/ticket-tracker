package com.example.skeleton.repository;

import com.example.skeleton.domain.model.Comment;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicket(Ticket ticket);

    List<Comment> findByAuthor(User author);

    List<Comment> findByTicketAndAuthor(Ticket ticket, User author);

    List<Comment> findByBodyContainingIgnoreCase(String body);

    long countByTicketId(Long ticketId);
}