package com.example.skeleton.controller;

import com.example.skeleton.service.implementation.TicketDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/digest")
@RequiredArgsConstructor
public class TicketDigestAdminController {

    private final TicketDigestService digestService;

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> trigger() {
        int sent = digestService.sendDigests();
        return ResponseEntity.ok(Map.of(
                "emailsSent", sent,
                "triggeredAt", OffsetDateTime.now()
        ));
    }
}