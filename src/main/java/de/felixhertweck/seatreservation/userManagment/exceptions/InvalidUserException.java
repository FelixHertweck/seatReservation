package de.felixhertweck.seatreservation.userManagment.exceptions;

import jakarta.ws.rs.NotAcceptableException;

public class InvalidUserException extends NotAcceptableException {
    public InvalidUserException(String message) {
        super(message);
    }
}
