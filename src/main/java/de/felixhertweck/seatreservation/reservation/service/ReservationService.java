package de.felixhertweck.seatreservation.reservation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.reservation.EventBookingClosedException;
import de.felixhertweck.seatreservation.reservation.EventNotFoundException;
import de.felixhertweck.seatreservation.reservation.NoSeatsAvailableException;
import de.felixhertweck.seatreservation.reservation.SeatAlreadyReservedException;
import de.felixhertweck.seatreservation.reservation.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.ReservationsRequestCreateDTO;

@ApplicationScoped
public class ReservationService {

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    public List<ReservationResponseDTO> findReservationsByUser(User currentUser) {
        List<Reservation> reservations = reservationRepository.findByUser(currentUser);
        return reservations.stream().map(ReservationResponseDTO::new).toList();
    }

    public ReservationResponseDTO findReservationByIdForUser(Long id, User currentUser) {
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> new ReservationNotFoundException("Reservation not found"));
        if (!reservation.getUser().equals(currentUser)) {
            throw new SecurityException("You are not allowed to access this reservation");
        }
        return new ReservationResponseDTO(reservation);
    }

    @Transactional
    public List<ReservationResponseDTO> createReservationForUser(
            ReservationsRequestCreateDTO dto, User currentUser)
            throws NoSeatsAvailableException, EventBookingClosedException {

        if (dto.getSeatIds() == null || dto.getSeatIds().isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }

        // Validate the eventId, ensure it exists
        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(() -> new EventNotFoundException("Event or Seat not found"));

        // Validate the seatIds, ensure they exist
        List<Seat> seats =
                dto.getSeatIds().stream()
                        .map(
                                seatId ->
                                        seatRepository
                                                .findByIdOptional(seatId)
                                                .orElseThrow(
                                                        () ->
                                                                new EventNotFoundException(
                                                                        "Minimum one seat not"
                                                                                + " found")))
                        .toList();

        // Check if the user has an allowance for this event
        // And if the user is allowed to reserve that amount of seats
        List<EventUserAllowance> allowances = eventUserAllowanceRepository.findByUser(currentUser);
        EventUserAllowance eventUserAllowance =
                allowances.stream()
                        .filter(a -> a.getEvent().id.equals(event.id))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new EventNotFoundException(
                                                "You are not allowed to reserve seats for this"
                                                        + " event"));

        if (eventUserAllowance.getReservationsAllowedCount() < seats.size()) {
            throw new NoSeatsAvailableException(
                    "You have reached your reservation limit for this event");
        }

        LocalDateTime reservationTime = LocalDateTime.now();

        // Check if the event is still bookable
        if (event.getBookingDeadline() != null
                && reservationTime.isAfter(event.getBookingDeadline())) {
            throw new EventBookingClosedException("Event is no longer bookable");
        }

        // Check if seats are already reserved
        List<Reservation> existingReservations = reservationRepository.findByEventId(event.id);
        List<Reservation> newReservations = new ArrayList<>();
        for (Seat seat : seats) {
            if (existingReservations.stream().anyMatch(r -> r.getSeat().id.equals(seat.id))) {
                throw new SeatAlreadyReservedException("One or more seats are already reserved");
            }
            newReservations.add(new Reservation(currentUser, event, seat, reservationTime));
        }

        // Persist the new reservations
        reservationRepository.persistAll(newReservations);
        // Update the user's allowance
        eventUserAllowance.setReservationsAllowedCount(
                eventUserAllowance.getReservationsAllowedCount() - seats.size());
        eventUserAllowanceRepository.persist(eventUserAllowance);

        return newReservations.stream()
                .map(ReservationResponseDTO::new)
                .collect(Collectors.toList());
    }

    // Maybe remove this method, only expose delete and create methods
    /*
    @Transactional
    public DetailedReservationResponseDTO updateReservationForUser(
            Long id, ReservationRequestUpdateDTO dto, User currentUser) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new NotFoundException("Reservation not found");
        }
        if (!reservation.getUser().equals(currentUser)) {
            throw new ForbiddenException("You are not allowed to update this reservation");
        }

        Event event = eventRepository.findById(dto.eventId);
        if (event == null) {
            throw new NotFoundException("Event not found");
        }

        Seat seat = seatRepository.findById(dto.seatId);
        if (seat == null) {
            throw new NotFoundException("Seat not found");
        }

        reservation.setEvent(event);
        reservation.setSeat(seat);
        reservation.setReservationDate(LocalDateTime.now());
        reservationRepository.persist(reservation);

        return new DetailedReservationResponseDTO(reservation);
    }*/

    @Transactional
    public void deleteReservationForUser(Long id, User currentUser) {
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> new ReservationNotFoundException("Reservation not found"));
        if (!reservation.getUser().equals(currentUser)) {
            throw new SecurityException("You are not allowed to delete this reservation");
        }

        // Update the user's allowance
        List<EventUserAllowance> allowances = eventUserAllowanceRepository.findByUser(currentUser);
        EventUserAllowance eventUserAllowance =
                allowances.stream()
                        .filter(a -> a.getEvent().id.equals(reservation.getEvent().id))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new SecurityException(
                                                "You are not allowed to delete this reservation"));

        eventUserAllowance.setReservationsAllowedCount(
                eventUserAllowance.getReservationsAllowedCount() + 1);
        eventUserAllowanceRepository.persist(eventUserAllowance);

        reservationRepository.delete(reservation);
    }
}
