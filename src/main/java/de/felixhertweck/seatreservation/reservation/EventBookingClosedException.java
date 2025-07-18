package de.felixhertweck.seatreservation.reservation;

import jakarta.ws.rs.NotAcceptableException;

public class EventBookingClosedException extends NotAcceptableException {
    public EventBookingClosedException(String message) {
        super(message);
    }
}
