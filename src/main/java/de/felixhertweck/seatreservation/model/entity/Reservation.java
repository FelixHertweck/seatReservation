package de.felixhertweck.seatreservation.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(
        uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "seat_id"})},
        name = "reservations")
public class Reservation extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    private Seat seat;

    private LocalDateTime reservationDate;

    public Reservation() {}

    public Reservation(User user, Event event, Seat seat, LocalDateTime reservationDate) {
        this.user = user;
        this.event = event;
        this.seat = seat;
        this.reservationDate = reservationDate;
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

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(user, that.user)
                && Objects.equals(event, that.event)
                && Objects.equals(seat, that.seat)
                && Objects.equals(reservationDate, that.reservationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, event, seat, reservationDate);
    }

    @Override
    public String toString() {
        return "Reservation{"
                + "seat="
                + seat
                + ", user="
                + user
                + ", event="
                + event
                + ", reservationDate="
                + reservationDate
                + ", id="
                + id
                + '}';
    }
}
