package de.felixhertweck.seatreservation.userManagment.dto;

import java.util.Set;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;

public class UserProfileUpdateDTO {
    @NotNull(message = "Email cannot be null")
    @NoHtmlSanitize
    private String email;

    @NotNull(message = "Firstname cannot be null")
    private String firstname;

    @NotNull(message = "Lastname cannot be null")
    private String lastname;

    @NotNull(message = "Password cannot be null")
    private String password;

    @NotNull(message = "Roles cannot be null")
    @NotEmpty(message = "User must have at least one role")
    private Set<String> roles;

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public UserProfileUpdateDTO(
            String firstname,
            String lastname,
            String passwordHash,
            String email,
            Set<String> roles) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = passwordHash;
        this.email = email;
        this.roles = roles;
    }
}
