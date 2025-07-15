package de.felixhertweck.seatreservation.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "seat_id"})})
public class Reservation extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    public User user;

    @ManyToOne(fetch = FetchType.LAZY)
    public Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    public Seat seat;

    public LocalDateTime reservationDate;
}
