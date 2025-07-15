package de.felixhertweck.seatreservation.userManagment.dto;

public class UserProfileUpdateDTO {
    private String email;
    private String firstname;
    private String lastname;
    private String password; // Klartext-Passwort, das gehasht werden muss

    // Konstruktor
    public UserProfileUpdateDTO() {}

    public UserProfileUpdateDTO(String email, String firstname, String lastname, String password) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
    }

    // Getter und Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
