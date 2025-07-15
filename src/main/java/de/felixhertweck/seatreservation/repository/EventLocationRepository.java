package de.felixhertweck.seatreservation.repository;

import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.entity.EventLocation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventLocationRepository implements PanacheRepository<EventLocation> {}
