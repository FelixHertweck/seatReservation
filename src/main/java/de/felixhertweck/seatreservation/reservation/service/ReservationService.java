/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.reservation.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.reservation.exception.EventBookingClosedException;
import de.felixhertweck.seatreservation.reservation.exception.NoSeatsAvailableException;
import de.felixhertweck.seatreservation.reservation.exception.SeatAlreadyReservedException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReservationService {

    private static final Logger LOG = Logger.getLogger(ReservationService.class);

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject EmailService emailService;

    public List<Reservation> findReservationsByUser(User currentUser) {
        LOG.infof("Attempting to find reservations for user: %s", currentUser.getUsername());
        List<Reservation> reservations = reservationRepository.findByUser(currentUser);
        LOG.infof(
                "Found %d reservations for user: %s",
                reservations.size(), currentUser.getUsername());
        return reservations;
    }

    public Reservation findReservationByIdForUser(Long id, User currentUser) {
        LOG.infof(
                "Attempting to find reservation with ID %d for user: %s",
                id, currentUser.getUsername());
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation with ID %d not found for user %s.",
                                            id, currentUser.getUsername());
                                    return new ReservationNotFoundException(
                                            "Reservation not found");
                                });
        if (!reservation.getUser().equals(currentUser)) {
            LOG.warnf(
                    "User %s attempted to access reservation %d which belongs to user %s.",
                    currentUser.getUsername(), id, reservation.getUser().getUsername());
            throw new SecurityException("You are not allowed to access this reservation");
        }
        LOG.infof("Reservation with ID %d found for user %s.", id, currentUser.getUsername());
        return reservation;
    }

    @Transactional
    public List<Reservation> createReservationForUser(
            Long eventId, Set<Long> seatIds, User currentUser)
            throws NoSeatsAvailableException, EventBookingClosedException {
        LOG.infof(
                "Attempting to create reservation for user %s for event ID %d with %d seats.",
                currentUser.getUsername(), eventId, seatIds.size());

        if (currentUser.getEmail() == null
                || currentUser.getEmail().trim().isEmpty()
                || !currentUser.isEmailVerified()) {
            LOG.warnf(
                    "User %s attempted to create reservation without a verified email.",
                    currentUser.getUsername());
            throw new IllegalStateException(
                    "User must have a verified email address to create a reservation.");
        }

        if (seatIds == null || seatIds.isEmpty()) {
            LOG.warnf(
                    "User %s attempted to create reservation with no seats selected.",
                    currentUser.getUsername());
            throw new IllegalArgumentException("At least one seat must be selected");
        }

        // Validate the eventId, ensure it exists
        Event event =
                eventRepository
                        .findByIdOptional(eventId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for reservation creation by"
                                                    + " user %s.",
                                            eventId, currentUser.getUsername());
                                    return new EventNotFoundException("Event or Seat not found");
                                });
        LOG.debugf("Event %s (ID: %d) found for reservation.", event.getName(), event.id);

        // Validate the seatIds, ensure they exist
        List<Seat> seats =
                seatIds.stream()
                        .map(
                                seatId ->
                                        seatRepository
                                                .findByIdOptional(seatId)
                                                .orElseThrow(
                                                        () -> {
                                                            LOG.warnf(
                                                                    "Seat with ID %d not found for"
                                                                        + " reservation creation by"
                                                                        + " user %s.",
                                                                    seatId,
                                                                    currentUser.getUsername());
                                                            return new EventNotFoundException(
                                                                    "Minimum one seat not"
                                                                            + " found");
                                                        }))
                        .toList();
        LOG.debugf("All %d seats found for reservation.", seats.size());

        // Check if the user has an allowance for this event
        // And if the user is allowed to reserve that amount of seats
        List<EventUserAllowance> allowances = eventUserAllowanceRepository.findByUser(currentUser);
        EventUserAllowance eventUserAllowance =
                allowances.stream()
                        .filter(a -> a.getEvent().id.equals(event.id))
                        .findFirst()
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "User %s has no allowance for event %s (ID: %d).",
                                            currentUser.getUsername(), event.getName(), event.id);
                                    return new EventNotFoundException(
                                            "You are not allowed to reserve seats for this"
                                                    + " event");
                                });
        LOG.debugf(
                "User %s has allowance for event %s. Allowed: %d, Requested: %d",
                currentUser.getUsername(),
                event.getName(),
                eventUserAllowance.getReservationsAllowedCount(),
                seats.size());

        if (eventUserAllowance.getReservationsAllowedCount() < seats.size()) {
            LOG.warnf(
                    "User %s exceeded reservation limit for event %s (ID: %d). Allowed: %d,"
                            + " Requested: %d",
                    currentUser.getUsername(),
                    event.getName(),
                    event.id,
                    eventUserAllowance.getReservationsAllowedCount(),
                    seats.size());
            throw new NoSeatsAvailableException(
                    "You have reached your reservation limit for this event");
        }

        LocalDateTime reservationTime = LocalDateTime.now();

        // Check if the event is still bookable
        if (event.getBookingDeadline() != null
                && reservationTime.isAfter(event.getBookingDeadline())) {
            LOG.warnf(
                    "Event %s (ID: %d) booking deadline passed. Deadline: %s, Current time: %s",
                    event.getName(), event.id, event.getBookingDeadline(), reservationTime);
            throw new EventBookingClosedException("Event is no longer bookable");
        }
        LOG.debugf("Event %s (ID: %d) is still bookable.", event.getName(), event.id);

        // Check if seats are already reserved
        List<Reservation> existingReservations = reservationRepository.findByEventId(event.id);
        List<Reservation> newReservations = new ArrayList<>();
        for (Seat seat : seats) {
            if (existingReservations.stream()
                    .anyMatch(
                            r ->
                                    r.getSeat().id.equals(seat.id)
                                            && (r.getStatus() == ReservationStatus.RESERVED
                                                    || r.getStatus()
                                                            == ReservationStatus.BLOCKED))) {
                LOG.warnf(
                        "Seat %s (ID: %d) is already reserved for event %s (ID: %d).",
                        seat.getSeatNumber(), seat.id, event.getName(), event.id);
                throw new SeatAlreadyReservedException("One or more seats are already reserved");
            }
            newReservations.add(
                    new Reservation(
                            currentUser, event, seat, reservationTime, ReservationStatus.RESERVED));
            LOG.debugf(
                    "Prepared new reservation for seat %s (ID: %d).",
                    seat.getSeatNumber(), seat.id);
        }

        // Persist the new reservations
        reservationRepository.persistAll(newReservations);
        LOG.infof(
                "Persisted %d new reservations for user %s and event %s (ID: %d).",
                newReservations.size(), currentUser.getUsername(), event.getName(), event.id);

        // Update the user's allowance
        eventUserAllowance.setReservationsAllowedCount(
                eventUserAllowance.getReservationsAllowedCount() - seats.size());
        eventUserAllowanceRepository.persist(eventUserAllowance);
        LOG.infof(
                "Updated reservation allowance for user %s and event %s (ID: %d). New allowance:"
                        + " %d",
                currentUser.getUsername(),
                event.getName(),
                event.id,
                eventUserAllowance.getReservationsAllowedCount());

        try {
            LOG.debugf(
                    "Attempting to send reservation confirmation email to %s.",
                    currentUser.getEmail());
            emailService.sendReservationConfirmation(currentUser, newReservations);
            LOG.infof(
                    "Reservation confirmation email sent to %s for user %s.",
                    currentUser.getEmail(), currentUser.getUsername());
        } catch (IOException | PersistenceException | IllegalStateException e) {
            // Log the exception, but don't let it fail the transaction
            LOG.error("Failed to send reservation confirmation email", e);
        }

        return newReservations;
    }

    @Transactional
    public void deleteReservationForUser(Long id, User currentUser) {
        LOG.infof(
                "Attempting to delete reservation with ID %d for user: %s",
                id, currentUser.getUsername());

        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation with ID %d not found for deletion by user"
                                                    + " %s.",
                                            id, currentUser.getUsername());
                                    return new ReservationNotFoundException(
                                            "Reservation not found");
                                });
        if (!reservation.getUser().equals(currentUser)) {
            LOG.warnf(
                    "User %s attempted to delete reservation %d which belongs to user %s.",
                    currentUser.getUsername(), id, reservation.getUser().getUsername());
            throw new SecurityException("You are not allowed to delete this reservation");
        }
        LOG.debugf(
                "Reservation with ID %d found for user %s. Proceeding with deletion.",
                id, currentUser.getUsername());

        // Update the user's allowance
        List<EventUserAllowance> allowances = eventUserAllowanceRepository.findByUser(currentUser);
        EventUserAllowance eventUserAllowance =
                allowances.stream()
                        .filter(a -> a.getEvent().id.equals(reservation.getEvent().id))
                        .findFirst()
                        .orElseThrow(
                                () -> {
                                    LOG.errorf(
                                            "EventUserAllowance not found for user %s and event %s"
                                                    + " (ID: %d) during reservation deletion.",
                                            currentUser.getUsername(),
                                            reservation.getEvent().getName(),
                                            reservation.getEvent().id);
                                    return new SecurityException(
                                            "You are not allowed to delete this reservation");
                                });

        eventUserAllowance.setReservationsAllowedCount(
                eventUserAllowance.getReservationsAllowedCount() + 1);
        eventUserAllowanceRepository.persist(eventUserAllowance);
        LOG.infof(
                "Updated reservation allowance for user %s and event %s (ID: %d). New allowance:"
                        + " %d",
                currentUser.getUsername(),
                reservation.getEvent().getName(),
                reservation.getEvent().id,
                eventUserAllowance.getReservationsAllowedCount());

        reservationRepository.delete(reservation);
        LOG.infof(
                "Reservation with ID %d deleted successfully for user %s.",
                id, currentUser.getUsername());

        List<Reservation> activeReservations =
                reservationRepository.findByUserAndEvent(currentUser, reservation.getEvent());

        try {
            emailService.sendUpdateReservationConfirmation(
                    currentUser, List.of(reservation), activeReservations);
        } catch (IOException e) {
            LOG.errorf(
                    "Failed to send reservation update confirmation for user %s (ID: %d) and"
                            + " reservation %d.",
                    currentUser.getUsername(), currentUser.getId(), reservation.id);
            return;
        }

        LOG.infof(
                "Sent reservation update confirmation for user %s (ID: %d) and reservation %d.",
                currentUser.getUsername(), currentUser.getId(), reservation.id);
    }
}
