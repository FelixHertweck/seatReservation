package de.felixhertweck.seatreservation.eventManagement.exception;

public class EventLocationNotFoundException extends RuntimeException {
    public EventLocationNotFoundException(String message) {
        super(message);
    }
}
