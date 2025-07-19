package de.felixhertweck.seatreservation.model.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class EmailVerification extends PanacheEntity {
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expirationTime;

    /** Constructor for JPA. */
    public EmailVerification() {}

    public EmailVerification(User user, String token, LocalDateTime expirationTime) {
        this.user = user;
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }
}
