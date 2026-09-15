package com.example.skeleton.config;

import com.example.skeleton.mapper.*;
import com.example.skeleton.repository.*;
import com.example.skeleton.service.*;
import com.example.skeleton.service.implementation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public TicketService ticketService(TicketRepository ticketRepository,
                                       UserRepository userRepository,
                                       ProjectRepository projectRepository,
                                       TicketMapper ticketMapper,
                                       EnumMapper enumMapper) {
        return new TicketServiceImp(ticketRepository, userRepository, projectRepository, ticketMapper, enumMapper);
    }

    @Bean
    public ProjectService projectService(ProjectRepository projectRepository,
                                         UserRepository userRepository,
                                         ProjectMapper projectMapper) {
        return new ProjectServiceImp(projectRepository, userRepository, projectMapper);
    }

    @Bean
    public CommentService commentService(CommentRepository commentRepository,
                                         TicketRepository ticketRepository,
                                         UserRepository userRepository,
                                         CommentMapper commentMapper) {
        return new CommentServiceImp(commentRepository, ticketRepository, userRepository, commentMapper);
    }
}