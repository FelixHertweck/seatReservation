package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class ReservationRepository implements PanacheRepository<Reservation> {
    public List<Reservation> findByUser(User user) {
        return find("user", user).list();
    }

    public List<Reservation> findByEventId(Long eventId) {
        return find("event.id", eventId).list();
    }

    public List<Reservation> findByUserAndEvent(
            User user, de.felixhertweck.seatreservation.model.entity.Event event) {
        return find("user = ?1 and event = ?2", user, event).list();
    }

    public void persistAll(List<Reservation> newReservations) {
        newReservations.forEach(this::persist);
    }
}
