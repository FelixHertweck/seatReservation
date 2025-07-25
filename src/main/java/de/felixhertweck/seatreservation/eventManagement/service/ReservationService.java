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
package de.felixhertweck.seatreservation.eventManagement.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.eventManagement.dto.DetailedReservationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReservationService {

    private static final Logger LOG = Logger.getLogger(ReservationService.class);

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
    public List<DetailedReservationResponseDTO> findAllReservations(User currentUser)
            throws SecurityException, UserNotFoundException {
        LOG.debugf(
                "Attempting to retrieve all reservations for user: %s (ID: %d)",
                currentUser.getUsername(), currentUser.getId());
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all reservations.");
            return reservationRepository.listAll().stream()
                    .map(DetailedReservationResponseDTO::new)
                    .toList();
        }
        List<DetailedReservationResponseDTO> result =
                reservationRepository.find("event.manager", currentUser).list().stream()
                        .map(DetailedReservationResponseDTO::new)
                        .toList();
        LOG.infof(
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
    public DetailedReservationResponseDTO findReservationById(Long id, User currentUser)
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
            LOG.infof(
                    "Successfully retrieved reservation with ID %d for ADMIN user: %s (ID: %d)",
                    id, currentUser.getUsername(), currentUser.getId());
            return new DetailedReservationResponseDTO(reservation);
        }

        if (isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
            LOG.infof(
                    "Successfully retrieved reservation with ID %d for manager: %s (ID: %d)",
                    id, currentUser.getUsername(), currentUser.getId());
            return new DetailedReservationResponseDTO(reservation);
        }

        LOG.warnf(
                "User %s (ID: %d) is not allowed to access reservation with ID %d.",
                currentUser.getUsername(), currentUser.getId(), id);
        throw new SecurityException("You are not allowed to access this reservation.");
    }

    public List<DetailedReservationResponseDTO> findReservationsByEventId(
            Long eventId, User currentUser) {
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

        List<DetailedReservationResponseDTO> result =
                reservationRepository.find("event", event).list().stream()
                        .map(DetailedReservationResponseDTO::new)
                        .toList();
        LOG.infof(
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
    public DetailedReservationResponseDTO createReservation(
            ReservationRequestDTO dto, User currentUser)
            throws SecurityException, UserNotFoundException, IllegalArgumentException {
        LOG.debugf(
                "Attempting to create reservation for seat ID: %d, user ID: %d, event ID: %d by"
                        + " user: %s (ID: %d)",
                dto.getSeatId(),
                dto.getUserId(),
                dto.getEventId(),
                currentUser.getUsername(),
                currentUser.getId());
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

        if (!isManagerAllowedToAccessEvent(currentUser, event)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not allowed to access this reservation for creation.",
                    currentUser.getUsername(), currentUser.getId());
            throw new SecurityException("You are not allowed to access this reservation.");
        }

        Seat seat =
                seatRepository
                        .findByIdOptional(dto.getSeatId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Seat with ID %d not found for reservation creation.",
                                            dto.getSeatId());
                                    return new IllegalArgumentException(
                                            "Seat with id " + dto.getSeatId() + " not found");
                                });

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
            allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() - 1);
            eventUserAllowanceRepository.persist(allowance);
            LOG.debug(
                    String.format(
                            "Decremented reservation allowance for user ID %d and event ID %d. New"
                                    + " allowance: %d",
                            targetUser.getId(),
                            event.getId(),
                            allowance.getReservationsAllowedCount()));
        } catch (NoResultException e) {
            LOG.warnf(
                    e,
                    "User ID %d has no reservation allowance for event ID %d.",
                    targetUser.getId(),
                    event.getId());
            throw new IllegalArgumentException("User has no reservation allowance for this event.");
        }

        Reservation reservation =
                new Reservation(
                        targetUser, event, seat, LocalDateTime.now(), ReservationStatus.RESERVED);
        reservationRepository.persist(reservation);
        LOG.infof(
                "Reservation created successfully for seat ID %d, user ID %d, event ID %d."
                        + " Reservation ID: %d",
                dto.getSeatId(), dto.getUserId(), dto.getEventId(), reservation.id);

        try {
            emailService.sendReservationConfirmation(
                    targetUser, Collections.singletonList(reservation));
            LOG.debugf(
                    "Reservation confirmation email sent to user ID %d for reservation ID %d.",
                    targetUser.getId(), reservation.id);
        } catch (IOException | PersistenceException | IllegalStateException e) {
            LOG.errorf(
                    e,
                    "Failed to send reservation confirmation email for reservation ID %d.",
                    reservation.id);
        }

        return new DetailedReservationResponseDTO(reservation);
    }

    /**
     * Updates an existing reservation. Access is restricted based on user roles: - ADMIN: Allows
     * update for any reservation. - MANAGER: Allows update only if the reservation and the new
     * event (if changed) belong to events the manager is allowed to manage. - Other roles: Throws
     * ForbiddenException.
     *
     * @param id The ID of the reservation to update.
     * @param dto The ReservationUpdateDTO containing updated reservation details.
     * @return The updated Reservation entity.
     * @throws SecurityException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    @Transactional
    public DetailedReservationResponseDTO updateReservation(
            Long id, ReservationRequestDTO dto, User currentUser)
            throws SecurityException, UserNotFoundException {
        LOG.debugf(
                "Attempting to update reservation with ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation with ID %d not found for update by user:"
                                                    + " %s (ID: %d)",
                                            id, currentUser.getUsername(), currentUser.getId());
                                    return new ReservationNotFoundException(
                                            "Reservation with id " + id + " not found");
                                });

        if (!isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not allowed to update reservation with ID %d.",
                    currentUser.getUsername(), currentUser.getId(), id);
            throw new SecurityException("You are not allowed to update this reservation.");
        }
        // If the event is being changed, check if the new event is allowed
        if (dto.getEventId() != null && !dto.getEventId().equals(reservation.getEvent().getId())) {
            LOG.debugf(
                    "Event ID changed from %d to %d for reservation ID %d.",
                    reservation.getEvent().getId(), dto.getEventId(), id);
            Event newEvent =
                    eventRepository
                            .findByIdOptional(dto.getEventId())
                            .orElseThrow(
                                    () -> {
                                        LOG.warnf(
                                                "New event with ID %d not found for reservation"
                                                        + " update.",
                                                dto.getEventId());
                                        return new IllegalArgumentException(
                                                "Event with id " + dto.getEventId() + " not found");
                                    });
            if (!isManagerAllowedToAccessEvent(currentUser, newEvent)) {
                LOG.warnf(
                        "User %s (ID: %d) is not allowed to update reservation ID %d to new event"
                                + " ID %d.",
                        currentUser.getUsername(), currentUser.getId(), id, newEvent.getId());
                throw new SecurityException(
                        "You are not allowed to update this reservation to the new event.");
            }
        }

        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found during reservation update.",
                                            dto.getEventId());
                                    return new IllegalArgumentException(
                                            "Event with id " + dto.getEventId() + " not found");
                                });
        User user =
                userRepository
                        .findByIdOptional(dto.getUserId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "User with ID %d not found during reservation update.",
                                            dto.getUserId());
                                    return new IllegalArgumentException(
                                            "User with id " + dto.getUserId() + " not found");
                                });
        Seat seat =
                seatRepository
                        .findByIdOptional(dto.getSeatId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Seat with ID %d not found during reservation update.",
                                            dto.getSeatId());
                                    return new IllegalArgumentException(
                                            "Seat with id " + dto.getSeatId() + " not found");
                                });

        LOG.debugf(
                "Updating reservation ID %d: event ID='%d' -> '%d', user ID='%d' -> '%d', seat"
                        + " ID='%d' -> '%d'",
                id,
                reservation.getEvent().getId(),
                event.getId(),
                reservation.getUser().getId(),
                user.getId(),
                reservation.getSeat().getId(),
                seat.getId());
        reservation.setEvent(event);
        reservation.setUser(user);
        reservation.setSeat(seat);
        reservationRepository.persist(reservation);
        LOG.infof(
                "Reservation with ID %d updated successfully by user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        return new DetailedReservationResponseDTO(reservation);
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

        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debugf("User is ADMIN, deleting reservation with ID %d.", id);
            reservationRepository.delete(reservation);
            LOG.infof(
                    "Reservation with ID %d deleted successfully by ADMIN user: %s (ID: %d)",
                    id, currentUser.getUsername(), currentUser.getId());
            return;
        }

        if (currentUser.getRoles().contains(Roles.MANAGER)) {
            if (isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
                LOG.debugf("User is MANAGER and authorized, deleting reservation with ID %d.", id);
                reservationRepository.delete(reservation);
                LOG.infof(
                        "Reservation with ID %d deleted successfully by manager: %s (ID: %d)",
                        id, currentUser.getUsername(), currentUser.getId());
                return;
            }
        }

        LOG.warnf(
                "User %s (ID: %d) is not allowed to delete reservation with ID %d.",
                currentUser.getUsername(), currentUser.getId(), id);
        throw new SecurityException("You are not allowed to delete this reservation.");
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
    public void blockSeats(Long eventId, List<Long> seatIds, User currentUser)
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
                                                LocalDateTime.now(),
                                                ReservationStatus.BLOCKED))
                        .toList();

        reservationRepository.persist(newReservations);
        LOG.infof(
                "Successfully blocked %d seats for event ID %d by user: %s (ID: %d)",
                seats.size(), eventId, currentUser.getUsername(), currentUser.getId());
    }
}
