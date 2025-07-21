package de.felixhertweck.seatreservation.model.entity;

import java.util.Objects;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(
        uniqueConstraints = {@UniqueConstraint(columnNames = {"seatNumber", "location_id"})},
        name = "seats")
public class Seat extends PanacheEntity {

    @Column(nullable = false)
    private String seatNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private EventLocation location;

    private int xCoordinate;

    private int yCoordinate;

    public Seat() {}

    public Seat(String seatNumber, EventLocation location) {
        this.seatNumber = seatNumber;
        this.location = location;
    }

    public Seat(String seatNumber, EventLocation location, int xCoordinate, int yCoordinate) {
        this.seatNumber = seatNumber;
        this.location = location;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public EventLocation getLocation() {
        return location;
    }

    public int getXCoordinate() {
        return xCoordinate;
    }

    public int getYCoordinate() {
        return yCoordinate;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setLocation(EventLocation location) {
        this.location = location;
    }

    public void setXCoordinate(int xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public void setYCoordinate(int yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return xCoordinate == seat.xCoordinate
                && yCoordinate == seat.yCoordinate
                && Objects.equals(seatNumber, seat.seatNumber)
                && Objects.equals(location, seat.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatNumber, location, xCoordinate, yCoordinate);
    }

    @Override
    public String toString() {
        return "Seat{"
                + "seatNumber='"
                + seatNumber
                + '\''
                + ", location="
                + location
                + ", xCoordinate="
                + xCoordinate
                + ", yCoordinate="
                + yCoordinate
                + ", id="
                + id
                + '}';
    }
}
