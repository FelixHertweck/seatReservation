package de.felixhertweck.seatreservation.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"seatNumber", "location_id"})
})
public class Seat extends PanacheEntity {

    @Column(nullable = false)
    public String seatNumber; // z.B. "A1", "Reihe 5, Platz 12"

    @ManyToOne(fetch = FetchType.LAZY)
    public EventLocation location;
}