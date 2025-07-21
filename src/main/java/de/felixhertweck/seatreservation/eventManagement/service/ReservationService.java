package de.felixhertweck.seatreservation.eventManagement.service;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.eventManagement.dto.DetailedReservationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;

@ApplicationScoped
public class ReservationService {

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject UserRepository userRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

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
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            return reservationRepository.listAll().stream()
                    .map(DetailedReservationResponseDTO::new)
                    .toList();
        }
        return reservationRepository.find("event.manager", currentUser).list().stream()
                .map(DetailedReservationResponseDTO::new)
                .toList();
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
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new ReservationNotFoundException(
                                                "Reservation with id " + id + " not found"));

        // Admins können jede Reservierung sehen
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            return new DetailedReservationResponseDTO(reservation);
        }

        if (isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
            return new DetailedReservationResponseDTO(reservation);
        }

        throw new SecurityException("You are not allowed to access this reservation.");
    }

    public List<DetailedReservationResponseDTO> findReservationsByEventId(
            Long eventId, User currentUser) {
        Event event =
                eventRepository
                        .findByIdOptional(eventId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Event with id " + eventId + " not found"));

        if (!currentUser.getRoles().contains(Roles.ADMIN)
                && !isManagerAllowedToAccessEvent(currentUser, event)) {
            throw new SecurityException("You are not allowed to access this event.");
        }

        return reservationRepository.find("event", event).list().stream()
                .map(DetailedReservationResponseDTO::new)
                .toList();
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
        User targetUser =
                userRepository
                        .findByIdOptional(dto.getUserId())
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                "User with id " + dto.getUserId() + " not found."));

        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Event with id "
                                                        + dto.getEventId()
                                                        + " not found"));

        if (!isManagerAllowedToAccessEvent(currentUser, event)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("You are not allowed to access this reservation.");
        }

        Seat seat =
                seatRepository
                        .findByIdOptional(dto.getSeatId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Seat with id " + dto.getSeatId() + " not found"));

        try {
            EventUserAllowance allowance =
                    eventUserAllowanceRepository
                            .find("user = ?1 and event = ?2", targetUser, event)
                            .singleResult();
            if (allowance.getReservationsAllowedCount() <= 0) {
                throw new IllegalArgumentException(
                        "No more reservations allowed for this user and event.");
            }
            allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() - 1);
            eventUserAllowanceRepository.persist(allowance);
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User has no reservation allowance for this event.");
        }

        Reservation reservation = new Reservation(targetUser, event, seat, LocalDateTime.now());
        reservationRepository.persist(reservation);
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
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new ReservationNotFoundException(
                                                "Reservation with id " + id + " not found"));

        if (!isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("You are not allowed to update this reservation.");
        }
        // If the event is being changed, check if the new event is allowed
        if (dto.getEventId() != null && !dto.getEventId().equals(reservation.getEvent().getId())) {
            Event newEvent =
                    eventRepository
                            .findByIdOptional(dto.getEventId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Event with id "
                                                            + dto.getEventId()
                                                            + " not found"));
            if (!isManagerAllowedToAccessEvent(currentUser, newEvent)) {
                throw new SecurityException(
                        "You are not allowed to update this reservation to the new event.");
            }
        }

        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Event with id "
                                                        + dto.getEventId()
                                                        + " not found"));
        User user =
                userRepository
                        .findByIdOptional(dto.getUserId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User with id " + dto.getUserId() + " not found"));
        Seat seat =
                seatRepository
                        .findByIdOptional(dto.getSeatId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Seat with id " + dto.getSeatId() + " not found"));

        reservation.setEvent(event);
        reservation.setUser(user);
        reservation.setSeat(seat);
        reservationRepository.persist(reservation);
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
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new ReservationNotFoundException(
                                                "Reservation with id " + id + " not found"));

        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            reservationRepository.delete(reservation);
            return;
        }

        if (currentUser.getRoles().contains(Roles.MANAGER)) {
            if (isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
                reservationRepository.delete(reservation);
                return;
            }
        }

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
        return event.getManager().equals(manager);
    }
}
