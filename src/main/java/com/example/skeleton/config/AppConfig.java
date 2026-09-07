package com.example.skeleton.config;

import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.service.TicketService;
import com.example.skeleton.specification.TicketSpecificationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TicketService ticketService(TicketRepository ticketRepository) {

        return new TicketService(ticketRepository);
    }
}