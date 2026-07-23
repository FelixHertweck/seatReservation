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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.email.service.EmailService;
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
import de.felixhertweck.seatreservation.reservation.exception.SeatBlockedException;
import de.felixhertweck.seatreservation.utils.CodeGenerator;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReservationService {

    private static final Logger LOG = Logger.getLogger(ReservationService.class);
    private static final String ID_IN_QUERY = "id in ?1";

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
        LOG.debugf("Attempting to find reservations for user ID: %s", currentUser.id);
        List<Reservation> reservations = reservationRepository.findByUser(currentUser);
        LOG.debugf("Found %d reservations for user ID: %s", reservations.size(), currentUser.id);
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
    public UserReservationResponseDTO findReservationByIdForUser(UUID id, User currentUser)
            throws ReservationNotFoundException, SecurityException {
        LOG.debugf("Attempting to find reservation with ID %s for user ID: %s", id, currentUser.id);
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation with ID %s not found for user ID: %s.",
                                            id, currentUser.id);
                                    return new ReservationNotFoundException(
                                            "Reservation not found");
                                });
        if (!reservation.getUser().equals(currentUser)) {
            LOG.warnf(
                    "user ID: %s attempted to access reservation %s which belongs to user ID: %s.",
                    currentUser.id, id, reservation.getUser().id);
            throw new SecurityException("You are not allowed to access this reservation");
        }
        LOG.debugf("Reservation with ID %s found for user ID: %s.", id, currentUser.id);
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
     * @throws SeatAlreadyReservedException if any of the requested seats are already reserved
     * @throws SeatBlockedException if any of the requested seats are blocked
     * @throws IllegalStateException if the user does not have a verified email address
     * @throws IllegalArgumentException if no seats are selected
     */
    @Transactional
    public List<UserReservationResponseDTO> createReservationForUser(
            UserReservationsRequestDTO dto, User currentUser)
            throws NoSeatsAvailableException, EventBookingClosedException {
        LOG.debugf(
                "Attempting to create reservation for user ID: %s for event ID %s with %d seats.",
                currentUser.id, dto.getEventId(), (Integer) dto.getSeatIds().size());
        LOG.debugf("ReservationsRequestDTO: %s", dto.toString());

        if (currentUser.getEmail() == null
                || currentUser.getEmail().trim().isEmpty()
                || !currentUser.isEmailVerified()) {
            LOG.warnf(
                    "user ID: %s attempted to create reservation without a verified email.",
                    currentUser.id);
            throw new IllegalStateException(
                    "User must have a verified email address to create a reservation.");
        }

        if (dto.getSeatIds() == null || dto.getSeatIds().isEmpty()) {
            LOG.warnf(
                    "user ID: %s attempted to create reservation with no seats selected.",
                    currentUser.id);
            throw new IllegalArgumentException("At least one seat must be selected");
        }

        // Validate the eventId, ensure it exists
        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %s not found for reservation creation by"
                                                    + " user %s.",
                                            dto.getEventId(), currentUser.id);
                                    return new EventNotFoundException("Event or Seat not found");
                                });
        LOG.debugf("Event ID: %s found for reservation.", event.id);

        // Validate the seatIds, ensure they exist
        List<Seat> foundSeats = seatRepository.find(ID_IN_QUERY, dto.getSeatIds()).list();
        Map<UUID, Seat> foundSeatMap =
                foundSeats.stream().collect(Collectors.toMap(s -> s.id, s -> s, (s1, s2) -> s1));

        List<Seat> seats = new ArrayList<>();
        for (UUID seatId : dto.getSeatIds()) {
            Seat seat = foundSeatMap.get(seatId);
            if (seat == null) {
                LOG.warnf(
                        "Seat with ID %s not found for reservation creation by user %s.",
                        seatId, currentUser.id);
                throw new EventNotFoundException("Minimum one seat not found");
            }
            seats.add(seat);
        }
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
                                            "user ID: %s has no allowance for event ID: %s.",
                                            currentUser.id, event.id);
                                    return new EventNotFoundException(
                                            "You are not allowed to reserve seats for this"
                                                    + " event");
                                });
        LOG.debugf(
                "user ID: %s has allowance for event ID: %s. Allowed: %d, Requested: %d",
                currentUser.id,
                event.id,
                eventUserAllowance.getReservationsAllowedCount(),
                seats.size());

        if (eventUserAllowance.getReservationsAllowedCount() < seats.size()) {
            LOG.warnf(
                    "user ID: %s exceeded reservation limit for event %s (ID: %s). Allowed: %d,"
                            + " Requested: %d",
                    currentUser.id,
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
                    "Event %s (ID: %s) booking not started. Booking starts at: %s, Current time:"
                            + " %s",
                    event.getName(), event.id, event.getBookingStartTime(), reservationTime);
            throw new EventBookingClosedException("Event is not yet bookable");
        }
        LOG.debugf("Event ID: %s booking has started.", event.id);

        // Check if the event is still bookable
        if (event.getBookingDeadline() != null
                && reservationTime.isAfter(event.getBookingDeadline())) {
            LOG.warnf(
                    "Event ID: %s booking deadline passed. Deadline: %s, Current time: %s",
                    event.id, event.getBookingDeadline(), reservationTime);
            throw new EventBookingClosedException("Event is no longer bookable");
        }
        LOG.debugf("Event ID: %s is still bookable.", event.id);

        // Check if seats are already reserved
        List<Reservation> existingReservations = reservationRepository.findByEventId(event.id);

        Set<UUID> reservedSeatIds = new java.util.HashSet<>();
        Set<UUID> blockedSeatIds = new java.util.HashSet<>();

        for (Reservation r : existingReservations) {
            if (r.getStatus() == ReservationStatus.RESERVED) {
                reservedSeatIds.add(r.getSeat().id);
            } else if (r.getStatus() == ReservationStatus.BLOCKED) {
                blockedSeatIds.add(r.getSeat().id);
            }
        }

        List<Reservation> newReservations = new ArrayList<>();
        for (Seat seat : seats) {
            if (reservedSeatIds.contains(seat.id)) {
                LOG.warnf("Seat ID: %s is already reserved for event ID: %s.", seat.id, event.id);
                throw new SeatAlreadyReservedException("One or more seats are already reserved");
            } else if (blockedSeatIds.contains(seat.id)) {
                LOG.warnf("Seat ID: %s is blocked for event ID: %s.", seat.id, event.id);
                throw new SeatBlockedException("One or more seats are blocked");
            }
            String checkInCode = CodeGenerator.generateRandomCode();
            newReservations.add(
                    new Reservation(
                            currentUser,
                            event,
                            seat,
                            reservationTime,
                            ReservationStatus.RESERVED,
                            checkInCode));
            LOG.debugf("Prepared new reservation for seat ID: %s.", seat.id);
        }

        // Persist the new reservations
        reservationRepository.persistAll(newReservations);
        LOG.infof(
                "Persisted %d new reservations for user ID: %s and event ID: %s.",
                newReservations.size(), currentUser.id, event.id);
        LOG.debugf(
                "Persisted %d new reservations for user ID: %s and event ID: %s.",
                newReservations.size(), currentUser.id, event.id);

        // Update the user's allowance
        eventUserAllowance.setReservationsAllowedCount(
                eventUserAllowance.getReservationsAllowedCount() - seats.size());
        eventUserAllowanceRepository.persist(eventUserAllowance);
        LOG.infof(
                "Updated reservation allowance for user ID: %s and event %s (ID: %s). New"
                        + " allowance: %d",
                currentUser.id,
                event.getName(),
                event.id,
                eventUserAllowance.getReservationsAllowedCount());

        try {
            LOG.debugf(
                    "Attempting to send reservation confirmation email to user ID: %s.",
                    currentUser.id);
            emailService.sendReservationConfirmation(currentUser, newReservations);
            LOG.debugf(
                    "Reservation confirmation email sent to %s for user ID: %s.",
                    currentUser.id, currentUser.id);
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
    public void deleteReservationForUser(List<UUID> ids, User currentUser)
            throws IOException,
                    PersistenceException,
                    ReservationNotFoundException,
                    SecurityException,
                    IllegalArgumentException {
        LOG.debugf(
                "Attempting to delete reservations with IDs %s for user ID: %s",
                ids != null ? ids : Collections.emptyList(), currentUser.id);
        if (ids == null || ids.isEmpty()) {
            LOG.warnf("No reservation IDs provided for deletion by user ID: %s", currentUser.id);
            throw new IllegalArgumentException(
                    "At least one reservation ID must be provided for deletion");
        }

        List<Reservation> foundReservations = reservationRepository.find(ID_IN_QUERY, ids).list();
        Map<UUID, Reservation> foundReservationMap =
                foundReservations.stream()
                        .collect(Collectors.toMap(r -> r.id, r -> r, (r1, r2) -> r1));

        List<Reservation> reservations = new ArrayList<>();
        for (UUID id : ids) {
            Reservation uncheckedReservation = foundReservationMap.get(id);
            if (uncheckedReservation == null) {
                LOG.warnf(
                        "Reservation with ID %s not found for deletion by user %s.",
                        id, currentUser.id);
                throw new ReservationNotFoundException("Reservation not found");
            }
            if (!uncheckedReservation.getUser().equals(currentUser)) {
                LOG.warnf(
                        "user ID: %s attempted to delete reservation %s which belongs to user ID:"
                                + " %s.",
                        currentUser.id, id, uncheckedReservation.getUser().id);
                throw new SecurityException("You are not allowed to delete this reservation");
            }
            reservations.add(uncheckedReservation);
        }

        if (reservations.isEmpty()) {
            LOG.warnf("No valid reservations found for deletion by user ID: %s", currentUser.id);
            throw new IllegalArgumentException("No valid reservations found for deletion");
        }

        // Group reservations by event to handle allowance updates and email confirmations correctly
        Map<UUID, List<Reservation>> reservationMap =
                reservations.stream().collect(Collectors.groupingBy(r -> r.getEvent().id));

        for (Map.Entry<UUID, List<Reservation>> entry : reservationMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                LOG.warnf(
                        "No reservations found for event ID %s during deletion process for user"
                                + " %s.",
                        entry.getKey(), currentUser.id);
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
                                        "Updated reservation allowance for user ID: %s and event ID"
                                                + " %s. New allowance: %d",
                                        currentUser.id,
                                        entry.getKey(),
                                        eventUserAllowance.getReservationsAllowedCount());
                            });

            // Delete reservations for the current event in a single batch query
            List<UUID> reservationIdsToDelete = entry.getValue().stream().map(r -> r.id).toList();
            reservationRepository.delete(ID_IN_QUERY, reservationIdsToDelete);
            LOG.infof(
                    "Deleted reservations with IDs %s for user ID: %s.",
                    reservationIdsToDelete, currentUser.id);

            // Send email update confirmation for each event
            List<Reservation> activeReservations =
                    reservationRepository.findByUserAndEventId(currentUser, entry.getKey());

            try {
                emailService.sendUpdateReservationConfirmation(
                        currentUser, entry.getValue(), activeReservations);
            } catch (IOException e) {
                LOG.errorf(
                        "Failed to send reservation update confirmation for user ID: %s (ID: %s)"
                                + " and reservations %s.",
                        currentUser.id, currentUser.getId(), entry.getValue());
                return;
            }

            LOG.debugf(
                    "Sent reservation update confirmation for user ID: %s (ID: %s) and reservations"
                            + " %s.",
                    currentUser.id, currentUser.getId(), entry.getValue());
        }
    }
}
