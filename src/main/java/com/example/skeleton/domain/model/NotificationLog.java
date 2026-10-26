package com.example.skeleton.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "notification_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_log_user_ticket_date",
                columnNames = {"user_id", "ticket_id", "digest_date"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "digest_date", nullable = false)
    private LocalDate digestDate;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;
}