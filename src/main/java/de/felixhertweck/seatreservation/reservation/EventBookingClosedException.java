package de.felixhertweck.seatreservation.reservation;

public class EventBookingClosedException extends RuntimeException {
    public EventBookingClosedException(String message) {
        super(message);
    }
}
