package de.felixhertweck.seatreservation.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class EventLocation extends PanacheEntity {
    public String name;
    public String address;
    public String seatingChartUrl;

    @OneToMany(
            mappedBy = "location",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    public List<Seat> seats = new ArrayList<>();
}