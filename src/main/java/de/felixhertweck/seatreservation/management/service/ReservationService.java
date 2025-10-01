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
package de.felixhertweck.seatreservation.management.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.management.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.management.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.management.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.utils.ReservationExporter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReservationService {

    private static final Logger LOG = Logger.getLogger(ReservationService.class);

    @ConfigProperty(name = "exporter.pdf.minutesBeforeEventStart", defaultValue = "10")
    Integer EXPORTER_PDF_MINUTES_BEFORE_EVENT_START;

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject UserRepository userRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject EmailService emailService;

    /**
     * Retrieves all reservations. Access is restricted based on user roles: - ADMIN: Returns all
     * reservations. - MANAGER: Returns reservations only for events the manager is allowed to
     * manage. - Other roles: Throws ForbiddenException.
     *
     * @return A list of Reservation entities.
     * @throws SecurityException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    public List<ReservationResponseDTO> findAllReservations(User currentUser)
            throws SecurityException, UserNotFoundException {
        LOG.debugf(
                "Attempting to retrieve all reservations for user: %s (ID: %d)",
                currentUser.getUsername(), currentUser.getId());
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all reservations.");
            return reservationRepository.listAll().stream()
                    .map(ReservationResponseDTO::new)
                    .toList();
        }
        List<ReservationResponseDTO> result =
                reservationRepository.find("event.manager", currentUser).list().stream()
                        .map(ReservationResponseDTO::new)
                        .toList();
        LOG.debugf(
                "Retrieved %d reservations for manager: %s (ID: %d)",
                result.size(), currentUser.getUsername(), currentUser.getId());
        return result;
    }

    /**
     * Retrieves a reservation by its ID. Access is restricted based on user roles: - ADMIN: Returns
     * the reservation if found. - MANAGER: Returns the reservation only if it belongs to an event
     * the manager is allowed to manage. - Other roles: Throws ForbiddenException.
     *
     * @param id The ID of the reservation to retrieve.
     * @return The Reservation entity.
     * @throws SecurityException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    public ReservationResponseDTO findReservationById(Long id, User currentUser)
            throws SecurityException, UserNotFoundException {
        LOG.debugf(
                "Attempting to retrieve reservation with ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation with ID %d not found for user: %s (ID:"
                                                    + " %d)",
                                            id, currentUser.getUsername(), currentUser.getId());
                                    return new ReservationNotFoundException(
                                            "Reservation with id " + id + " not found");
                                });

        // Admins können jede Reservierung sehen
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debugf(
                    "Successfully retrieved reservation with ID %d for ADMIN user: %s (ID: %d)",
                    id, currentUser.getUsername(), currentUser.getId());
            return new ReservationResponseDTO(reservation);
        }

        if (isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
            LOG.debugf(
                    "Successfully retrieved reservation with ID %d for manager: %s (ID: %d)",
                    id, currentUser.getUsername(), currentUser.getId());
            return new ReservationResponseDTO(reservation);
        }

        LOG.warnf(
                "User %s (ID: %d) is not allowed to access reservation with ID %d.",
                currentUser.getUsername(), currentUser.getId(), id);
        throw new SecurityException("You are not allowed to access this reservation.");
    }

    public List<ReservationResponseDTO> findReservationsByEventId(Long eventId, User currentUser) {
        LOG.debugf(
                "Attempting to retrieve reservations for event ID: %d by user: %s (ID: %d)",
                eventId, currentUser.getUsername(), currentUser.getId());
        Event event =
                eventRepository
                        .findByIdOptional(eventId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for retrieving reservations"
                                                    + " by user: %s (ID: %d)",
                                            eventId,
                                            currentUser.getUsername(),
                                            currentUser.getId());
                                    return new IllegalArgumentException(
                                            "Event with id " + eventId + " not found");
                                });

        if (!currentUser.getRoles().contains(Roles.ADMIN)
                && !isManagerAllowedToAccessEvent(currentUser, event)) {
            LOG.warnf(
                    "User %s (ID: %d) is not allowed to access event ID %d for retrieving"
                            + " reservations.",
                    currentUser.getUsername(), currentUser.getId(), eventId);
            throw new SecurityException("You are not allowed to access this event.");
        }

        List<ReservationResponseDTO> result =
                reservationRepository.find("event", event).list().stream()
                        .map(ReservationResponseDTO::new)
                        .toList();
        LOG.debugf(
                "Retrieved %d reservations for event ID %d by user: %s (ID: %d)",
                result.size(), eventId, currentUser.getUsername(), currentUser.getId());
        return result;
    }

    /**
     * Creates a new reservation. Access is restricted based on user roles: - ADMIN: Allows creation
     * for any user and event. - MANAGER: Allows creation only for events the manager is allowed to
     * manage. - Other roles: Throws ForbiddenException.
     *
     * @param dto The ReservationCreationDTO containing reservation details.
     * @return The created Reservation entity.
     * @throws UserNotFoundException If the target user or current user cannot be found.
     * @throws SecurityException If the current user does not have the necessary permissions.
     * @throws IllegalArgumentException If the user has no reservation allowance for the event.
     */
    @Transactional
    public Set<ReservationResponseDTO> createReservations(
            ReservationRequestDTO dto, User managerUser)
            throws SecurityException, UserNotFoundException, IllegalArgumentException {
        LOG.debugf(
                "Attempting to create reservation for seat ID: %d, user ID: %d, event ID: %d by"
                        + " user: %s (ID: %d)",
                dto.getSeatIds(),
                dto.getUserId(),
                dto.getEventId(),
                managerUser.getUsername(),
                managerUser.getId());
        User targetUser =
                userRepository
                        .findByIdOptional(dto.getUserId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Target user with ID %d not found for reservation"
                                                    + " creation.",
                                            dto.getUserId());
                                    return new UserNotFoundException(
                                            "User with id " + dto.getUserId() + " not found.");
                                });

        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for reservation creation.",
                                            dto.getEventId());
                                    return new IllegalArgumentException(
                                            "Event with id " + dto.getEventId() + " not found");
                                });

        if (!isManagerAllowedToAccessEvent(managerUser, event)
                && !managerUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not allowed to access this reservation for creation.",
                    targetUser.getUsername(), targetUser.getId());
            throw new SecurityException("You are not allowed to access this reservation.");
        }

        List<Reservation> existingReservations = new ArrayList<>();

        for (Long dtoSeatId : dto.getSeatIds()) {
            Seat seat =
                    seatRepository
                            .findByIdOptional(dtoSeatId)
                            .orElseThrow(
                                    () -> {
                                        LOG.warnf(
                                                "Seat with ID %d not found for reservation"
                                                        + " creation.",
                                                dtoSeatId);
                                        return new IllegalArgumentException(
                                                "Seat with id " + dtoSeatId + " not found");
                                    });

            if (!dto.isDeductAllowance()) {
                LOG.debugf(
                        "Allowance check skipped for user %s (ID: %d).",
                        targetUser.getUsername(), targetUser.getId());
            } else {
                try {
                    EventUserAllowance allowance =
                            eventUserAllowanceRepository
                                    .find("user = ?1 and event = ?2", targetUser, event)
                                    .singleResult();
                    if (allowance.getReservationsAllowedCount() <= 0) {
                        LOG.warnf(
                                "No more reservations allowed for user ID %d and event ID %d.",
                                targetUser.getId(), event.getId());
                        throw new IllegalArgumentException(
                                "No more reservations allowed for this user and event.");
                    }
                    allowance.setReservationsAllowedCount(
                            allowance.getReservationsAllowedCount() - 1);
                    eventUserAllowanceRepository.persist(allowance);
                    LOG.debug(
                            String.format(
                                    "Decremented reservation allowance for user ID %d and event ID"
                                            + " %d. New allowance: %d",
                                    targetUser.getId(),
                                    event.getId(),
                                    allowance.getReservationsAllowedCount()));
                } catch (NoResultException e) {
                    LOG.warnf(
                            e,
                            "User ID %d has no reservation allowance for event ID %d.",
                            targetUser.getId(),
                            event.getId());
                    throw new IllegalArgumentException(
                            "User has no reservation allowance for this event.");
                }
            }

            Reservation reservation =
                    new Reservation(
                            targetUser, event, seat, Instant.now(), ReservationStatus.RESERVED);
            reservationRepository.persist(reservation);
            existingReservations.add(reservation);
            LOG.infof(
                    "Reservation created successfully for seat ID %d, user ID %d, event ID %d.",
                    dtoSeatId, dto.getUserId(), dto.getEventId());
        }

        try {
            emailService.sendReservationConfirmation(
                    targetUser, existingReservations, managerUser.getEmail());
            LOG.debugf(
                    "Reservation confirmation email sent to user ID %d for reservation ID %d.",
                    targetUser.getId(), existingReservations.getFirst().getEvent().id);
        } catch (IOException | PersistenceException | IllegalStateException e) {
            LOG.errorf(
                    e,
                    "Failed to send reservation confirmation email to user ID %d for reservation ID"
                            + " %d.",
                    targetUser.getId(),
                    existingReservations.getFirst().id);
        }

        return existingReservations.stream()
                .map(ReservationResponseDTO::new)
                .collect(Collectors.toSet());
    }

    /**
     * Deletes a reservation by its ID. Access is restricted based on user roles: - ADMIN: Allows
     * deletion of any reservation. - MANAGER: Allows deletion only if the reservation belongs to an
     * event the manager is allowed to manage. - Other roles: Throws ForbiddenException.
     *
     * @param id The ID of the reservation to delete.
     * @throws SecurityException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    @Transactional
    public void deleteReservation(Long id, User currentUser)
            throws SecurityException, UserNotFoundException {
        LOG.debugf(
                "Attempting to delete reservation with ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());

        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation with ID %d not found for deletion by user:"
                                                    + " %s (ID: %d)",
                                            id, currentUser.getUsername(), currentUser.getId());
                                    return new ReservationNotFoundException(
                                            "Reservation with id " + id + " not found");
                                });

        if (!currentUser.getRoles().contains(Roles.ADMIN)
                && !isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
            LOG.warnf(
                    "User %s (ID: %d) is not allowed to delete reservation with ID %d.",
                    currentUser.getUsername(), currentUser.getId(), id);
            throw new SecurityException("You are not allowed to delete this reservation.");
        }

        reservationRepository.delete(reservation);
        LOG.infof("Reservation with ID %d deleted successfully.", id);

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

        LOG.debugf(
                "Sent reservation update confirmation for user %s (ID: %d) and reservation %d.",
                currentUser.getUsername(), currentUser.getId(), reservation.id);
    }

    /**
     * Checks if a manager is allowed to access a specific event. A manager is allowed if they have
     * an EventUserAllowance for that event.
     *
     * @param manager The User entity representing the manager.
     * @param event The Event entity to check access for.
     * @return true if the manager is allowed to access the event, false otherwise.
     */
    public boolean isManagerAllowedToAccessEvent(User manager, Event event) {
        LOG.debugf(
                "Checking if manager %s (ID: %d) is allowed to access event ID %d.",
                manager.getUsername(), manager.getId(), event.getId());
        boolean isAllowed = event.getManager().equals(manager);
        if (isAllowed) {
            LOG.debugf(
                    "Manager %s (ID: %d) is allowed to access event ID %d.",
                    manager.getUsername(), manager.getId(), event.getId());
        } else {
            LOG.debugf(
                    "Manager %s (ID: %d) is NOT allowed to access event ID %d.",
                    manager.getUsername(), manager.getId(), event.getId());
        }
        return isAllowed;
    }

    /**
     * Blocks seats for an event. Access is restricted based on user roles: - ADMIN: Allows blocking
     * seats for any event. - MANAGER: Allows blocking seats only for events the manager is allowed
     * to manage. - Other roles: Throws SecurityException.
     *
     * @param eventId The ID of the event for which to block seats.
     * @param seatIds The IDs of the seats to block.
     * @param currentUser The user performing the action.
     * @throws IllegalArgumentException If the event or any seat is not found.
     * @throws SecurityException If the current user does not have the necessary permissions.
     * @throws IllegalStateException If any of the specified seats are already reserved or blocked.
     */
    @Transactional
    public Set<ReservationResponseDTO> blockSeats(
            Long eventId, List<Long> seatIds, User currentUser)
            throws IllegalArgumentException, SecurityException, IllegalStateException {
        LOG.debugf(
                "Attempting to block seats for event ID: %d, seat IDs: %s by user: %s (ID: %d)",
                eventId, seatIds, currentUser.getUsername(), currentUser.getId());
        Event event =
                eventRepository
                        .findByIdOptional(eventId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for blocking seats.",
                                            eventId);
                                    return new IllegalArgumentException(
                                            "Event with id " + eventId + " not found");
                                });

        if (!isManagerAllowedToAccessEvent(currentUser, event)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not allowed to block seats for event ID %d.",
                    currentUser.getUsername(), currentUser.getId(), eventId);
            throw new SecurityException("You are not allowed to block seats for this event.");
        }

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
                                                                        + " blocking seats in event"
                                                                        + " ID %d.",
                                                                    seatId, eventId);
                                                            return new IllegalArgumentException(
                                                                    "Seat with id "
                                                                            + seatId
                                                                            + " not found");
                                                        }))
                        .toList();

        List<Reservation> existingReservations = reservationRepository.findByEventId(eventId);
        for (Seat seat : seats) {
            if (existingReservations.stream().anyMatch(r -> r.getSeat().equals(seat))) {
                LOG.warnf(
                        "Seat with ID %d is already reserved or blocked for event ID %d.",
                        seat.id, eventId);
                throw new IllegalStateException(
                        "Seat with id " + seat.id + " is already reserved or blocked.");
            }
        }

        List<Reservation> newReservations =
                seats.stream()
                        .map(
                                seat ->
                                        new Reservation(
                                                currentUser,
                                                event,
                                                seat,
                                                Instant.now(),
                                                ReservationStatus.BLOCKED))
                        .toList();

        reservationRepository.persist(newReservations);

        LOG.debugf(
                "Successfully blocked %d seats for event ID %d by user: %s (ID: %d)",
                seats.size(), eventId, currentUser.getUsername(), currentUser.getId());
        return newReservations.stream()
                .map(ReservationResponseDTO::new)
                .collect(Collectors.toSet());
    }

    public byte[] exportReservationsToCsv(Long eventId, User currentUser)
            throws EventNotFoundException, SecurityException, IOException {
        LOG.debugf(
                "Attempting to export reservations for event ID %d by user: %s (ID: %d)",
                eventId, currentUser.getUsername(), currentUser.getId());

        List<Reservation> reservations =
                findByEventSorted(getSortedReservations(eventId, currentUser));
        ByteArrayOutputStream baos = ReservationExporter.exportReservationsToCsv(reservations);

        LOG.debugf(
                "Successfully exported %d reservations for event ID %d to CSV by user: %s (ID: %d)",
                reservations.size(), eventId, currentUser.getUsername(), currentUser.getId());
        return baos.toByteArray();
    }

    public byte[] exportReservationsToPdf(Long eventId, User currentUser)
            throws EventNotFoundException, SecurityException, IOException {
        LOG.debugf(
                "Attempting to export reservations for event ID %d by user: %s (ID: %d)",
                eventId, currentUser.getUsername(), currentUser.getId());

        Event event = getSortedReservations(eventId, currentUser);
        List<Reservation> reservations = findByEventSorted(event);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String reservedUntilValue =
                event.getStartTime()
                        .minusSeconds(EXPORTER_PDF_MINUTES_BEFORE_EVENT_START * 60)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);

        ByteArrayOutputStream baos =
                ReservationExporter.exportReservationsToPdf(reservations, reservedUntilValue);

        LOG.debugf(
                "Successfully exported %d reservations for event ID %d to PDF by user: %s (ID: %d)",
                reservations.size(), eventId, currentUser.getUsername(), currentUser.getId());

        return baos.toByteArray();
    }

    private Event getSortedReservations(Long eventId, User currentUser) {
        Event event =
                eventRepository
                        .findByIdOptional(eventId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for CSV export.", eventId);
                                    return new EventNotFoundException(
                                            "Event with id " + eventId + " not found");
                                });

        if (!event.getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to export reservations for event ID %d.",
                    currentUser.getUsername(), currentUser.getId(), eventId);
            throw new SecurityException(
                    "User is not authorized to export this event's reservations");
        }

        return event;
    }

    private List<Reservation> findByEventSorted(Event event) {
        List<Reservation> reservations = findByEvent(event);

        // Sort by seat number
        reservations.sort(Comparator.comparing(r -> r.getSeat().getSeatNumber()));

        return reservations;
    }

    public List<Reservation> findByEvent(Event event) {
        return reservationRepository.find("event", event).list();
    }
}
