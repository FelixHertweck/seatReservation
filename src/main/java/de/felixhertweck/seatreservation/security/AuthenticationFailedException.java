package de.felixhertweck.seatreservation.security;

import jakarta.ws.rs.ForbiddenException;

public class AuthenticationFailedException extends ForbiddenException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
