package com.example.skeleton.repository;

import com.example.skeleton.domain.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByUserIdAndTicketIdAndDigestDate(Long userId, Long ticketId, LocalDate digestDate);

    List<NotificationLog> findByBatchId(String batchId);

    long countByDigestDate(LocalDate digestDate);
}