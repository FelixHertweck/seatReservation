package de.felixhertweck.seatreservation.eventManagement.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.SecurityContext;

import de.felixhertweck.seatreservation.eventManagement.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ReservationResponseDTO;
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

    @Inject SecurityContext securityContext;

    /**
     * Retrieves all reservations. Access is restricted based on user roles: - ADMIN: Returns all
     * reservations. - MANAGER: Returns reservations only for events the manager is allowed to
     * manage. - Other roles: Throws ForbiddenException.
     *
     * @return A list of Reservation entities.
     * @throws ForbiddenException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    public List<ReservationResponseDTO> findAllReservations(User currentUser) {
        if (securityContext.isUserInRole(Roles.ADMIN)) {
            return reservationRepository.listAll().stream()
                    .map(ReservationResponseDTO::new)
                    .toList();
        }
        List<Long> allowedEventIds =
                currentUser.getEventAllowances().stream()
                        .map(allowance -> allowance.getEvent().getId())
                        .collect(Collectors.toList());

        if (allowedEventIds.isEmpty()) {
            return Collections.emptyList();
        }
        return reservationRepository.find("event.id IN ?1", allowedEventIds).list().stream()
                .map(ReservationResponseDTO::new)
                .toList();
    }

    /**
     * Retrieves a reservation by its ID. Access is restricted based on user roles: - ADMIN: Returns
     * the reservation if found. - MANAGER: Returns the reservation only if it belongs to an event
     * the manager is allowed to manage. - Other roles: Throws ForbiddenException.
     *
     * @param id The ID of the reservation to retrieve.
     * @return The Reservation entity.
     * @throws NotFoundException If the reservation with the given ID is not found.
     * @throws ForbiddenException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    public ReservationResponseDTO findReservationById(Long id, User currentUser) {
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Reservation with id " + id + " not found"));

        // Admins können jede Reservierung sehen
        if (securityContext.isUserInRole(Roles.ADMIN)) {
            return new ReservationResponseDTO(reservation);
        }

        if (isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
            return new ReservationResponseDTO(reservation);
        }

        throw new ForbiddenException("You are not allowed to access this reservation.");
    }

    /**
     * Creates a new reservation. Access is restricted based on user roles: - ADMIN: Allows creation
     * for any user and event. - MANAGER: Allows creation only for events the manager is allowed to
     * manage. - Other roles: Throws ForbiddenException.
     *
     * @param dto The ReservationCreationDTO containing reservation details.
     * @return The created Reservation entity.
     * @throws UserNotFoundException If the target user or current user cannot be found.
     * @throws NotFoundException If the event or seat specified in the DTO is not found.
     * @throws ForbiddenException If the current user does not have the necessary permissions.
     * @throws BadRequestException If the user has no reservation allowance for the event.
     */
    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO dto, User currentUser)
            throws ForbiddenException {
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
                                        new NotFoundException(
                                                "Event with id "
                                                        + dto.getEventId()
                                                        + " not found"));

        if (!isManagerAllowedToAccessEvent(currentUser, event)
                && !securityContext.isUserInRole(Roles.ADMIN)) {
            throw new ForbiddenException("You are not allowed to access this reservation.");
        }

        Seat seat =
                seatRepository
                        .findByIdOptional(dto.getSeatId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Seat with id " + dto.getSeatId() + " not found"));

        try {
            EventUserAllowance allowance =
                    eventUserAllowanceRepository
                            .find("user = ?1 and event = ?2", targetUser, event)
                            .singleResult();
            if (allowance.getReservationsAllowedCount() <= 0) {
                throw new BadRequestException(
                        "No more reservations allowed for this user and event.");
            }
            allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() - 1);
            eventUserAllowanceRepository.persist(allowance);
        } catch (NoResultException e) {
            throw new BadRequestException("User has no reservation allowance for this event.");
        }

        Reservation reservation = new Reservation(targetUser, event, seat, LocalDateTime.now());
        reservationRepository.persist(reservation);
        return new ReservationResponseDTO(reservation);
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
     * @throws NotFoundException If the reservation, event, user, or seat is not found.
     * @throws ForbiddenException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    @Transactional
    public ReservationResponseDTO updateReservation(
            Long id, ReservationRequestDTO dto, User currentUser) {
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Reservation with id " + id + " not found"));

        if (!isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())
                && !securityContext.isUserInRole(Roles.ADMIN)) {
            throw new ForbiddenException("You are not allowed to update this reservation.");
        }
        // If the event is being changed, check if the new event is allowed
        if (dto.getEventId() != null && !dto.getEventId().equals(reservation.getEvent().getId())) {
            Event newEvent =
                    eventRepository
                            .findByIdOptional(dto.getEventId())
                            .orElseThrow(
                                    () ->
                                            new NotFoundException(
                                                    "Event with id "
                                                            + dto.getEventId()
                                                            + " not found"));
            if (!isManagerAllowedToAccessEvent(currentUser, newEvent)) {
                throw new ForbiddenException(
                        "You are not allowed to update this reservation to the new event.");
            }
        }

        Event event =
                eventRepository
                        .findByIdOptional(dto.getEventId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Event with id "
                                                        + dto.getEventId()
                                                        + " not found"));
        User user =
                userRepository
                        .findByIdOptional(dto.getUserId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "User with id " + dto.getUserId() + " not found"));
        Seat seat =
                seatRepository
                        .findByIdOptional(dto.getSeatId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Seat with id " + dto.getSeatId() + " not found"));

        reservation.setEvent(event);
        reservation.setUser(user);
        reservation.setSeat(seat);
        reservationRepository.persist(reservation);
        return new ReservationResponseDTO(reservation);
    }

    /**
     * Deletes a reservation by its ID. Access is restricted based on user roles: - ADMIN: Allows
     * deletion of any reservation. - MANAGER: Allows deletion only if the reservation belongs to an
     * event the manager is allowed to manage. - Other roles: Throws ForbiddenException.
     *
     * @param id The ID of the reservation to delete.
     * @throws NotFoundException If the reservation with the given ID is not found.
     * @throws ForbiddenException If the current user does not have the necessary permissions.
     * @throws UserNotFoundException If the current user cannot be found.
     */
    @Transactional
    public void deleteReservation(Long id, User currentUser) {
        Reservation reservation =
                reservationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Reservation with id " + id + " not found"));

        if (securityContext.isUserInRole(Roles.MANAGER)) {
            if (!isManagerAllowedToAccessEvent(currentUser, reservation.getEvent())) {
                throw new ForbiddenException("You are not allowed to delete this reservation.");
            }
        } else {
            throw new ForbiddenException("You are not allowed to delete reservations.");
        }
        reservationRepository.delete(reservation);
    }

    // ######################################################################################
    // Private Helper Methods
    // ######################################################################################

    /**
     * Checks if a manager is allowed to access a specific event. A manager is allowed if they have
     * an EventUserAllowance for that event.
     *
     * @param manager The User entity representing the manager.
     * @param event The Event entity to check access for.
     * @return true if the manager is allowed to access the event, false otherwise.
     */
    private boolean isManagerAllowedToAccessEvent(User manager, Event event) {
        return eventUserAllowanceRepository
                .find("user = ?1 and event = ?2", manager, event)
                .firstResultOptional()
                .isPresent();
    }
}
