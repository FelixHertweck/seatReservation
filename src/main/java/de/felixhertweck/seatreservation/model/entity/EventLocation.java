package de.felixhertweck.seatreservation.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "eventlocations")
public class EventLocation extends PanacheEntity {

    private String name;
    private String address;
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    private User manager;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    public EventLocation() {}

    public EventLocation(String name, String address, User manager, Integer capacity) {
        this.name = name;
        this.address = address;
        this.manager = manager;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventLocation that = (EventLocation) o;
        return Objects.equals(name, that.name)
                && Objects.equals(address, that.address)
                && Objects.equals(capacity, that.capacity)
                && Objects.equals(manager, that.manager); // seats excluded
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, capacity, manager); // seats excluded
    }

    @Override
    public String toString() {
        return "EventLocation{"
                + "id="
                + id
                + ", seats="
                + seats
                + ", manager="
                + manager
                + ", capacity="
                + capacity
                + ", address='"
                + address
                + '\''
                + ", name='"
                + name
                + '\''
                + '}';
    }
}
