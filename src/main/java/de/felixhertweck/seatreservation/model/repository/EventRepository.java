package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {

    public List<Event> findByManager(User manager) {
        return find("manager", manager).list();
    }
}
