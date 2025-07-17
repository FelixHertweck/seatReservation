package de.felixhertweck.seatreservation.userManagment.dto;

import java.util.Set;

public class UserProfileUpdateDTO {
    private String email;
    private String firstname;
    private String lastname;
    private String passwordHash;
    private Set<String> roles;

    public UserProfileUpdateDTO() {}

    public UserProfileUpdateDTO(
            String email, String firstname, String lastname, String password, Set<String> roles) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.passwordHash = password;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
