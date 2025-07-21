package de.felixhertweck.seatreservation.userManagment.dto;

import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;

public class UserCreationDTO {
    @NotNull(message = "Username cannot be null")
    private String username;

    @NoHtmlSanitize private String email;

    @NotNull(message = "Password cannot be null")
    private String password;

    @NotNull(message = "Firstname cannot be null")
    private String firstname;

    @NotNull(message = "Lastname cannot be null")
    private String lastname;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public UserCreationDTO(
            String username, String email, String password, String firstname, String lastname) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
    }
}
