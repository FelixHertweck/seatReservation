package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventUserAllowanceRepository implements PanacheRepository<EventUserAllowance> {
    public List<EventUserAllowance> findByUser(User user) {
        return find("user", user).list();
    }
}
