package de.felixhertweck.seatreservation.security.dto;

import jakarta.validation.constraints.NotNull;

public class LoginRequestDTO {
    @NotNull(message = "Username must not be null")
    private String username;

    @NotNull(message = "Password must not be null")
    private String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
