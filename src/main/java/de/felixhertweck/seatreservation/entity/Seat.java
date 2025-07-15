package de.felixhertweck.seatreservation.entity;

import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"seatNumber", "location_id"})})
public class Seat extends PanacheEntity {

    @Column(nullable = false)
    public String seatNumber; // z.B. "A1", "Reihe 5, Platz 12"

    @ManyToOne(fetch = FetchType.LAZY)
    public EventLocation location;

    public boolean reserved;

    public int xCoordinate;

    public int yCoordinate;
}