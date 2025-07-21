package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class SeatRepository implements PanacheRepository<Seat> {

    public List<Seat> findByEventLocation(EventLocation eventLocation) {
        return find("location", eventLocation).list();
    }
}
