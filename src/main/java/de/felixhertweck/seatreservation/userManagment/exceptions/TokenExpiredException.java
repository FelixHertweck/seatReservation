package de.felixhertweck.seatreservation.userManagment.exceptions;

import jakarta.ws.rs.BadRequestException;

public class TokenExpiredException extends BadRequestException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
