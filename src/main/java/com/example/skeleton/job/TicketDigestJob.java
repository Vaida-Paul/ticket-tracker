package com.example.skeleton.job;

import com.example.skeleton.service.implementation.TicketDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.digest.enabled", havingValue = "true", matchIfMissing = true)
public class TicketDigestJob {

    private final TicketDigestService digestService;

    @Scheduled(cron = "${app.digest.cron}")
    public void sendDailyDigest() {
        log.info("Scheduled digest job triggered");
        try {
            int sent = digestService.sendDigests();
            log.info("Scheduled digest job done: {} emails", sent);
        } catch (Exception e) {
            log.error("Scheduled digest job failed", e);
        }
    }
}