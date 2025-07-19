package de.felixhertweck.seatreservation.userManagment.exceptions;

import jakarta.ws.rs.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
