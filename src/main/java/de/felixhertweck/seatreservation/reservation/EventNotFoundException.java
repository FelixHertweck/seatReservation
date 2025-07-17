package de.felixhertweck.seatreservation.reservation;

import jakarta.ws.rs.NotFoundException;

public class EventNotFoundException extends NotFoundException {
    public EventNotFoundException(String message) {
        super(message);
    }
}
