package com.example.skeleton.security;

import com.example.skeleton.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

@Component("ticketSecurity")
@RequiredArgsConstructor
public class TicketSecurityService {

    private final TicketRepository ticketRepository;

    public boolean isAssigneeOrManagerOrAdmin(Long ticketId, Authentication authentication) {
        if (authentication == null) return false;

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return true;

        boolean isProjectManager = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PROJECT_MANAGER".equals(a.getAuthority()));
        if (isProjectManager) return true;

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) return false;

        return ticketRepository.findById(ticketId)
                .map(ticket -> ticket.getAssignee() != null
                        && ticket.getAssignee().getId().equals(currentUserId))
                .orElse(false);
    }
}
