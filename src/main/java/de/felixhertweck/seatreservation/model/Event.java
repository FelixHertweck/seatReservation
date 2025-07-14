package de.felixhertweck.seatreservation.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Event extends PanacheEntity {

    public String name;
    public LocalDate eventDate;
    public LocalDate bookingDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    public User creatorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    public EventLocation location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Reservation> reservations = new ArrayList<>();
}