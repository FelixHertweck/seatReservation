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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.management.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserReservationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.UserReservationsRequestDTO;
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

    /**
     * Retrieves all reservations for the currently authenticated user.
     *
     * @param currentUser the currently authenticated user
     * @return a list of user reservation response DTOs
     */
    public List<UserReservationResponseDTO> findReservationsByUser(User currentUser) {
        LOG.debugf("Attempting to find reservations for user: %s", currentUser.getUsername());
        List<Reservation> reservations = reservationRepository.findByUser(currentUser);
        LOG.debugf(
                "Found %d reservations for user: %s",
                reservations.size(), currentUser.getUsername());
        return reservations.stream().map(UserReservationResponseDTO::new).toList();
    }

    /**
     * Retrieves a specific reservation by its ID for the currently authenticated user. Verifies
     * that the user owns the reservation before returning it.
     *
     * @param id the reservation ID to retrieve
     * @param currentUser the currently authenticated user
     * @return the user reservation response DTO
     * @throws ReservationNotFoundException if the reservation is not found
     * @throws SecurityException if the current user does not own the reservation
     */
    public UserReservationResponseDTO findReservationByIdForUser(Long id, User currentUser)
            throws ReservationNotFoundException, SecurityException {
        LOG.debugf(
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
        LOG.debugf("Reservation with ID %d found for user %s.", id, currentUser.getUsername());
        return new UserReservationResponseDTO(reservation);
    }

    /**
     * Creates reservations for the specified user for the given event and seats. Validates that the
     * user has a verified email address, has allowances for the event, and that the seats are
     * available. Automatically deducts the reserved seats from the user's allowance and sends a
     * confirmation email after successful reservation.
     *
     * @param dto the reservation request DTO containing event ID and seat IDs
     * @param currentUser the currently authenticated user attempting to create the reservations
     * @return a list of newly created user reservation response DTOs
     * @throws EventNotFoundException if the event or any seat is not found
     * @throws NoSeatsAvailableException if the user has exceeded their reservation limit for the
     *     event
     * @throws EventBookingClosedException if the event booking has not started or has already ended
     * @throws SeatAlreadyReservedException if any of the requested seats are already reserved or
     *     blocked
     * @throws IllegalStateException if the user does not have a verified email address
     * @throws IllegalArgumentException if no seats are selected
     */
    @Transactional
    public List<UserReservationResponseDTO> createReservationForUser(
            UserReservationsRequestDTO dto, User currentUser)
            throws NoSeatsAvailableException, EventBookingClosedException {
        LOG.debugf(
                "Attempting to create reservation for user %s for event ID %d with %d seats.",
                currentUser.getUsername(), dto.getEventId(), dto.getSeatIds().size());
        LOG.debugf("ReservationsRequestDTO: %s", dto.toString());

        if (currentUser.getEmail() == null
                || currentUser.getEmail().trim().isEmpty()
                || !currentUser.isEmailVerified()) {
            LOG.warnf(
                    "User %s attempted to create reservation without a verified email.",
                    currentUser.getUsername());
            throw new IllegalStateException(
                    "User must have a verified email address to create a reservation.");
        }

        if (dto.getSeatIds() == null || dto.getSeatIds().isEmpty()) {
            LOG.warnf(
                    "User %s attempted to create reservation with no seats selected.",
                    currentUser.getUsername());
            throw new IllegalArgumentException("At least one seat must be selected");
        }

        // Validate the eventId, ensure it exists
        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for reservation creation by"
                                                    + " user %s.",
                                            dto.getEventId(), currentUser.getUsername());
                                    return new EventNotFoundException("Event or Seat not found");
                                });
        LOG.debugf("Event %s (ID: %d) found for reservation.", event.getName(), event.id);

        // Validate the seatIds, ensure they exist
        List<Seat> seats =
                dto.getSeatIds().stream()
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

        Instant reservationTime = Instant.now();

        // Check if the event is already available for booking
        if (event.getBookingStartTime() != null
                && reservationTime.isBefore(event.getBookingStartTime())) {
            LOG.warnf(
                    "Event %s (ID: %d) booking not started. Booking starts at: %s, Current time:"
                            + " %s",
                    event.getName(), event.id, event.getBookingStartTime(), reservationTime);
            throw new EventBookingClosedException("Event is not yet bookable");
        }
        LOG.debugf("Event %s (ID: %d) booking has started.", event.getName(), event.id);

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
        LOG.debugf(
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
            LOG.debugf(
                    "Reservation confirmation email sent to %s for user %s.",
                    currentUser.getEmail(), currentUser.getUsername());
        } catch (IOException | PersistenceException | IllegalStateException e) {
            // Log the exception, but don't let it fail the transaction
            LOG.error("Failed to send reservation confirmation email", e);
        }

        return newReservations.stream()
                .map(UserReservationResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Deletes reservations with the given IDs for the specified user. Ensures that the user is
     * authorized to delete each reservation and updates the user's allowance accordingly. Sends a
     * confirmation email after successful deletion.
     *
     * @param ids List of reservation IDs to delete.
     * @param currentUser The user attempting to delete the reservations.
     * @throws ReservationNotFoundException if any reservation is not found.
     * @throws SecurityException if the user is not authorized to delete any reservation.
     * @throws IOException if sending the confirmation email fails.
     * @throws PersistenceException if the reservation cannot be deleted from the database.
     * @throws IllegalArgumentException if the list of IDs is null or empty.
     */
    @Transactional
    public void deleteReservationForUser(List<Long> ids, User currentUser)
            throws IOException,
                    PersistenceException,
                    ReservationNotFoundException,
                    SecurityException,
                    IllegalArgumentException {
        LOG.debugf(
                "Attempting to delete reservations with IDs %s for user: %s",
                ids != null ? ids : Collections.emptyList(), currentUser.getUsername());
        if (ids == null || ids.isEmpty()) {
            LOG.warnf(
                    "No reservation IDs provided for deletion by user: %s",
                    currentUser.getUsername());
            throw new IllegalArgumentException(
                    "At least one reservation ID must be provided for deletion");
        }

        List<Reservation> reservations = new ArrayList<>();
        for (Long id : ids) {
            Reservation uncheckedReservation =
                    reservationRepository
                            .findByIdOptional(id)
                            .orElseThrow(
                                    () -> {
                                        LOG.warnf(
                                                "Reservation with ID %d not found for deletion by"
                                                        + " user %s.",
                                                id, currentUser.getUsername());
                                        return new ReservationNotFoundException(
                                                "Reservation not found");
                                    });
            if (!uncheckedReservation.getUser().equals(currentUser)) {
                LOG.warnf(
                        "User %s attempted to delete reservation %d which belongs to user %s.",
                        currentUser.getUsername(),
                        id,
                        uncheckedReservation.getUser().getUsername());
                throw new SecurityException("You are not allowed to delete this reservation");
            }
            reservations.add(uncheckedReservation);
        }

        if (reservations.isEmpty()) {
            LOG.warnf(
                    "No valid reservations found for deletion by user: %s",
                    currentUser.getUsername());
            throw new IllegalArgumentException("No valid reservations found for deletion");
        }

        // Group reservations by event to handle allowance updates and email confirmations correctly
        Map<Long, List<Reservation>> reservationMap =
                reservations.stream().collect(Collectors.groupingBy(r -> r.getEvent().id));

        for (Map.Entry<Long, List<Reservation>> entry : reservationMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                LOG.warnf(
                        "No reservations found for event ID %d during deletion process for user"
                                + " %s.",
                        entry.getKey(), currentUser.getUsername());
                continue;
            }
            // Update allowance count for each event
            eventUserAllowanceRepository
                    .findByUserAndEventId(currentUser, entry.getKey())
                    .ifPresent(
                            eventUserAllowance -> {
                                eventUserAllowance.setReservationsAllowedCount(
                                        eventUserAllowance.getReservationsAllowedCount()
                                                + entry.getValue().size());
                                eventUserAllowanceRepository.persist(eventUserAllowance);
                                LOG.infof(
                                        "Updated reservation allowance for user %s and event ID %d."
                                                + " New allowance: %d",
                                        currentUser.getUsername(),
                                        entry.getKey(),
                                        eventUserAllowance.getReservationsAllowedCount());
                            });

            // Delete reservations for the current event
            entry.getValue()
                    .forEach(
                            reservation -> {
                                reservationRepository.delete(reservation);
                                LOG.debugf(
                                        "Deleted reservation with ID %d for user %s.",
                                        reservation.id, currentUser.getUsername());
                            });

            // Send email update confirmation for each event
            List<Reservation> activeReservations =
                    reservationRepository.findByUserAndEventId(currentUser, entry.getKey());

            try {
                emailService.sendUpdateReservationConfirmation(
                        currentUser, entry.getValue(), activeReservations);
            } catch (IOException e) {
                LOG.errorf(
                        "Failed to send reservation update confirmation for user %s (ID: %d) and"
                                + " reservations %s.",
                        currentUser.getUsername(), currentUser.getId(), entry.getValue());
                return;
            }

            LOG.debugf(
                    "Sent reservation update confirmation for user %s (ID: %d) and reservations"
                            + " %s.",
                    currentUser.getUsername(), currentUser.getId(), entry.getValue());
        }
    }
}
