package de.felixhertweck.seatreservation.model.entity;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class EventUserAllowance extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @Column(nullable = false, columnDefinition = "int default 1")
    private int reservationsAllowedCount = 1;

    public EventUserAllowance() {}

    public EventUserAllowance(User user, Event event, int reservationsAllowedCount) {
        this.user = user;
        this.event = event;
        this.reservationsAllowedCount = reservationsAllowedCount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public int getReservationsAllowedCount() {
        return reservationsAllowedCount;
    }

    public void setReservationsAllowedCount(int reservationsAllowedCount) {
        this.reservationsAllowedCount = reservationsAllowedCount;
    }

    public EventLocation getEventLocation() {
        return event.getEventLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventUserAllowance that = (EventUserAllowance) o;
        return reservationsAllowedCount == that.reservationsAllowedCount; // user and event excluded
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationsAllowedCount); // user and event excluded
    }

    @Override
    public String toString() {
        return "EventUserAllowance{"
                + "id="
                + id
                + ", reservationsAllowedCount="
                + reservationsAllowedCount
                + ", event="
                + event
                + ", user="
                + user
                + '}';
    }
}
