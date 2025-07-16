package de.felixhertweck.seatreservation.userManagment.dto;

public class UserProfileUpdateDTO {
    private String email;
    private String firstname;
    private String lastname;
    private String password;

    public UserProfileUpdateDTO() {}

    public UserProfileUpdateDTO(String email, String firstname, String lastname, String password) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
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

    public String getPassword() {
        return password;
    }
}
