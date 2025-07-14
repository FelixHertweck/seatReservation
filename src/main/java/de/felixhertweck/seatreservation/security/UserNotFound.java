package de.felixhertweck.seatreservation.security;

public class UserNotFound extends RuntimeException {
    public UserNotFound(String username) {
        super(String.format("User '%s' not found.", username));
    }
}
