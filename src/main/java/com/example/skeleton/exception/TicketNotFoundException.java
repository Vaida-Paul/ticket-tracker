package com.example.skeleton.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(Long id) {
        super("Ticket with id " + id + " not found");
    }
}