package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventUserAllowanceRepository implements PanacheRepository<EventUserAllowance> {
    public List<EventUserAllowance> findByUser(User user) {
        return find("user", user).list();
    }

    public Optional<EventUserAllowance> findByEventIdAndUserId(Long eventId, Long userId) {
        return find("event.id = ?1 and user.id = ?2", eventId, userId).firstResultOptional();
    }
}
