package de.felixhertweck.seatreservation.reservation;

import jakarta.ws.rs.NotAllowedException;

public class SeatAlreadyReservedException extends NotAllowedException {
    public SeatAlreadyReservedException(String message) {
        super(message);
    }
}
