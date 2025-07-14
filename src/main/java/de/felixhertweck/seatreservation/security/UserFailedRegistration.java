package de.felixhertweck.seatreservation.security;

public class UserFailedRegistration extends RuntimeException {
    public UserFailedRegistration(String username) {
        super(String.format("Failed to register new user with username '%s'.", username));
    }
}
