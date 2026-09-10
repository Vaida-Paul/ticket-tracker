package com.example.skeleton.config;

import com.example.skeleton.mapper.EnumMapper;
import com.example.skeleton.mapper.TicketMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.TicketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TicketService ticketService(TicketRepository ticketRepository,
                                       UserRepository userRepository,
                                       ProjectRepository projectRepository,
                                       TicketMapper ticketMapper,
                                       EnumMapper enumMapper) {

        return new TicketService(ticketRepository, userRepository, projectRepository, ticketMapper, enumMapper);
    }
}