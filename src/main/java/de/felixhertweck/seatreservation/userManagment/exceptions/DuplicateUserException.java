package de.felixhertweck.seatreservation.userManagment.exceptions;

import jakarta.ws.rs.NotAcceptableException;

public class DuplicateUserException extends NotAcceptableException {
    public DuplicateUserException(String message) {
        super(message);
    }
}
