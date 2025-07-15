package de.felixhertweck.seatreservation.repository;

import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.entity.Event;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {}
