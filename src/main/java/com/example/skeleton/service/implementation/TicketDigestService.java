package com.example.skeleton.service.implementation;

import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.NotificationLog;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.repository.NotificationLogRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.specification.TicketSpecificationBuilder;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketDigestService {

    private static final List<TicketStatus> EXCLUDED_STATUSES = List.of(
            TicketStatus.DONE,
            TicketStatus.CANCELLED
    );

    private final TicketRepository ticketRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.digest.from}")
    private String fromAddress;

    @Value("${app.digest.subject}")
    private String subject;

    @Transactional
    public int sendDigests() {
        LocalDate today = LocalDate.now();
        String batchId = UUID.randomUUID().toString();

        log.info("Starting digest batchId={} for date={}", batchId, today);

        List<Ticket> dueSoon = findDueSoonOrOverdue(today);
        log.info("Found {} candidate tickets", dueSoon.size());

        Map<User, List<Ticket>> byAssignee = dueSoon.stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(Ticket::getAssignee));

        int emailsSent = 0;
        for (Map.Entry<User, List<Ticket>> entry : byAssignee.entrySet()) {
            User user = entry.getKey();
            List<Ticket> notYetSent = entry.getValue().stream()
                    .filter(t -> !notificationLogRepository
                            .existsByUserIdAndTicketIdAndDigestDate(
                                    user.getId(), t.getId(), today))
                    .toList();

            if (notYetSent.isEmpty()) {
                log.debug("Skipping user id={} — nothing new to send", user.getId());
                continue;
            }

            try {
                sendEmail(user, notYetSent);
                logNotifications(user, notYetSent, today, batchId);
                emailsSent++;
                log.info("Sent digest to user id={} with {} tickets",
                        user.getId(), notYetSent.size());
            } catch (Exception e) {
                log.error("Failed to send digest to user id={}", user.getId(), e);
            }
        }

        log.info("Digest batch {} completed: {} emails sent", batchId, emailsSent);
        return emailsSent;
    }

    private List<Ticket> findDueSoonOrOverdue(LocalDate today) {
        OffsetDateTime cutoff = today.plusDays(1)
                .atTime(23, 59, 59)
                .atOffset(ZoneOffset.UTC);

        var spec = new TicketSpecificationBuilder()
                .withDueBeforeOrEqual(cutoff)
                .withStatusNotIn(EXCLUDED_STATUSES)
                .build();

        return ticketRepository.findAll(spec);
    }

    private void sendEmail(User user, List<Ticket> tickets) throws Exception {
        Context ctx = new Context();
        ctx.setVariable("userName", user.getFullName());
        ctx.setVariable("tickets", tickets);

        String html = templateEngine.process("email/ticket-digest", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);
    }

    private void logNotifications(User user, List<Ticket> tickets,
                                  LocalDate digestDate, String batchId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<NotificationLog> logs = tickets.stream()
                .map(t -> NotificationLog.builder()
                        .user(user)
                        .ticket(t)
                        .digestDate(digestDate)
                        .batchId(batchId)
                        .sentAt(now)
                        .build())
                .toList();
        notificationLogRepository.saveAll(logs);
    }
}