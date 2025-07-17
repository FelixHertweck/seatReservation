package de.felixhertweck.seatreservation.reservation;

import jakarta.ws.rs.NotAcceptableException;

public class NoSeatsAvailableException extends NotAcceptableException {
    public NoSeatsAvailableException(String message) {
        super(message);
    }
}
