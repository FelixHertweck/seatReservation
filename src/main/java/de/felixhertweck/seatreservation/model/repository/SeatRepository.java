package de.felixhertweck.seatreservation.model.repository;

import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class SeatRepository implements PanacheRepository<Seat> {}
