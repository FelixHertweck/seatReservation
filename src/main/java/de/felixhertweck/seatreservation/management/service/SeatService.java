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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.common.events.ReservationCancelledEvent;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.management.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SeatService {

    private static final Logger LOG = Logger.getLogger(SeatService.class);

    @Inject SeatRepository seatRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    @Inject EventLocationAccessService eventLocationAccessService;

    @Inject EventLocationAreaRepository eventLocationAreaRepository;

    @Inject EventLocationEntranceRepository eventLocationEntranceRepository;

    @Inject SeatmapCacheService seatmapCacheService;

    @Inject jakarta.enterprise.event.Event<ReservationCancelledEvent> reservationCancelledBus;

    /**
     * Creates a new seat for the specified event location by a manager.
     *
     * @param dto the seat request DTO containing seat details
     * @param manager the manager attempting to create the seat
     * @return the created seat DTO
     * @throws ValidationException if the event location is not found or seat data is invalid
     * @throws AccessDeniedException if the manager does not own the event location
     */
    @Transactional
    public SeatDTO createSeatManager(SeatRequestDTO dto, AuthenticatedUser manager)
            throws ValidationException, AccessDeniedException {
        LOG.debugf(
                "Attempting to create seat with number: %s for event location ID: %s by manager"
                        + " ID: %s",
                dto.getSeatNumber(), dto.getEventLocationId(), manager.id());
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            LOG.warnf(
                    "Invalid seat data: seat number is empty for event location ID %s.",
                    eventLocation.getId());
            throw new ValidationException("Seat number cannot be empty");
        }
        if (dto.getSeatRow() == null || dto.getSeatRow().trim().isEmpty()) {
            LOG.warnf(
                    "Invalid seat data: seat row is empty for event location ID %s.",
                    eventLocation.getId());
            throw new ValidationException("Seat row cannot be empty");
        }

        Seat seat =
                new Seat(
                        dto.getSeatNumber(),
                        eventLocation,
                        dto.getSeatRow(),
                        dto.getCoordinate().xCoordinate(),
                        dto.getCoordinate().yCoordinate(),
                        resolveEntrance(dto.getEntranceId(), eventLocation),
                        resolveArea(dto.getAreaId(), eventLocation));
        seatRepository.persist(seat);
        seatmapCacheService.runAfterSuccessfulCommit(
                () -> seatmapCacheService.invalidateSeats(eventLocation.getId()));
        LOG.infof(
                "Seat ID: %s created successfully for event location ID %s",
                seat.id, eventLocation.getId());
        LOG.debugf(
                "Seat with ID %s created successfully for event location ID %s by manager ID: %s",
                seat.id, eventLocation.getId(), manager.id());
        return new SeatDTO(seat);
    }

    /**
     * Finds all seats of an event location for a given manager, verifying the manager owns that
     * location (or is ADMIN).
     *
     * @param eventLocationId the event location to list seats for
     * @param manager the manager whose seats should be retrieved
     * @return a list of seat DTOs
     */
    public List<SeatDTO> findSeatsForManagerByLocation(
            UUID eventLocationId, AuthenticatedUser manager) {
        LOG.debugf(
                "Attempting to retrieve seats for event location ID: %s for manager ID: %s",
                eventLocationId, manager.id());
        eventLocationAccessService.findOwnedEventLocation(eventLocationId, manager);
        List<SeatDTO> result = seatmapCacheService.getSeatsByLocation(eventLocationId);
        LOG.debugf(
                "Retrieved %d seats for event location ID: %s for manager ID: %s",
                result.size(), eventLocationId, manager.id());
        return result;
    }

    /**
     * Finds a seat by its ID for a given manager. Access control checks are performed to ensure the
     * manager owns the seat's event location.
     *
     * @param id the seat ID to retrieve
     * @param manager the manager attempting to access the seat
     * @return the seat DTO
     * @throws SeatNotFoundException if the seat is not found
     * @throws AccessDeniedException if the manager does not have permission to access the seat
     */
    public SeatDTO findSeatByIdForManager(UUID id, AuthenticatedUser manager)
            throws SeatNotFoundException, AccessDeniedException {
        LOG.debugf("Attempting to retrieve seat with ID: %s for manager ID: %s", id, manager.id());
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
        LOG.debugf("Successfully retrieved seat with ID %s for manager ID: %s", id, manager.id());
        return new SeatDTO(seat);
    }

    /**
     * Updates an existing seat for the specified event location by a manager.
     *
     * @param id the seat ID to update
     * @param dto the seat request DTO containing updated seat details
     * @param manager the manager attempting to update the seat
     * @return the updated seat DTO
     * @throws SeatNotFoundException if the seat is not found
     * @throws AccessDeniedException if the manager does not own the seat or the new event location
     * @throws ValidationException if the event location is not found or seat data is invalid
     */
    @Transactional
    public SeatDTO updateSeatForManager(UUID id, SeatRequestDTO dto, AuthenticatedUser manager)
            throws SeatNotFoundException, AccessDeniedException, ValidationException {
        LOG.debugf("Attempting to update seat with ID: %s for manager ID: %s", id, manager.id());
        Seat seat = findSeatEntityById(id, manager);
        UUID oldLocationId = seat.getLocation().getId();

        EventLocation newEventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            LOG.warnf("Invalid seat data: seat number is empty for seat ID %s.", id);
            throw new ValidationException("Seat number cannot be empty");
        }
        if (dto.getSeatRow() == null || dto.getSeatRow().trim().isEmpty()) {
            LOG.warnf("Invalid seat data: seat row is empty for seat ID %s.", id);
            throw new ValidationException("Seat row cannot be empty");
        }

        LOG.debugf(
                "Updating seat ID %s: seatNumber='%s' -> '%s', location ID='%s' -> '%s',"
                        + " coordinate='%s' -> '%s',"
                        + " seatRow='%s' -> '%s', entrance='%s' -> '%s', area='%s' -> '%s'",
                id,
                seat.getSeatNumber(),
                dto.getSeatNumber(),
                seat.getLocation().getId(),
                newEventLocation.getId(),
                seat.getCoordinate(),
                dto.getCoordinate(),
                seat.getSeatRow(),
                dto.getSeatRow(),
                seat.getEntrance(),
                dto.getEntranceId(),
                seat.getArea(),
                dto.getAreaId());

        seat.setSeatNumber(dto.getSeatNumber());
        seat.setLocation(newEventLocation);
        seat.setCoordinate(dto.getCoordinate().toEntity());
        seat.setSeatRow(dto.getSeatRow());
        seat.setEntrance(resolveEntrance(dto.getEntranceId(), newEventLocation));
        seat.setArea(resolveArea(dto.getAreaId(), newEventLocation));

        seatRepository.persist(seat);

        UUID newLocationId = newEventLocation.getId();
        seatmapCacheService.runAfterSuccessfulCommit(
                () -> {
                    seatmapCacheService.invalidateSeats(oldLocationId);
                    if (!oldLocationId.equals(newLocationId)) {
                        seatmapCacheService.invalidateSeats(newLocationId);
                    }
                });

        LOG.infof("Seat ID: %s updated successfully", seat.id);
        LOG.debugf("Seat with ID %s updated successfully by manager ID: %s", id, manager.id());
        return new SeatDTO(seat);
    }

    /**
     * Resolves an existing {@link EventLocationArea} by id, verifying it belongs to the given
     * {@code eventLocation}. No auto-create: the area must already exist, created via the dedicated
     * {@code AreaResource}.
     *
     * @param areaId The area id; {@code null} resolves to no area
     * @param eventLocation The event location the area must belong to
     * @return The resolved area, or {@code null} if {@code areaId} is {@code null}
     * @throws ValidationException if the area does not exist or belongs to another location
     */
    private EventLocationArea resolveArea(UUID areaId, EventLocation eventLocation) {
        if (areaId == null) {
            return null;
        }
        EventLocationArea area =
                eventLocationAreaRepository
                        .findByIdOptional(areaId)
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                "Area with id " + areaId + " not found"));
        if (!area.getEventLocation().getId().equals(eventLocation.getId())) {
            throw new ValidationException(
                    "Area with id " + areaId + " does not belong to this EventLocation");
        }
        return area;
    }

    /**
     * Resolves an existing {@link EventLocationEntrance} by id, verifying it belongs to the given
     * {@code eventLocation}. No auto-create: the entrance must already exist, created via the
     * dedicated {@code EntranceResource}.
     *
     * @param entranceId The entrance id; {@code null} resolves to no entrance
     * @param eventLocation The event location the entrance must belong to
     * @return The resolved entrance, or {@code null} if {@code entranceId} is {@code null}
     * @throws ValidationException if the entrance does not exist or belongs to another location
     */
    private EventLocationEntrance resolveEntrance(UUID entranceId, EventLocation eventLocation) {
        if (entranceId == null) {
            return null;
        }
        EventLocationEntrance entrance =
                eventLocationEntranceRepository
                        .findByIdOptional(entranceId)
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                "Entrance with id " + entranceId + " not found"));
        if (!entrance.getEventLocation().getId().equals(eventLocation.getId())) {
            throw new ValidationException(
                    "Entrance with id " + entranceId + " does not belong to this EventLocation");
        }
        return entrance;
    }

    /**
     * Delete seats by their IDs for a given manager. This method checks if the manager has the
     * right to delete each seat.
     *
     * @param ids list of seat IDs to delete
     * @param manager the manager attempting to delete the seats
     * @throws AccessDeniedException if the manager does not own any of the seats
     * @throws ValidationException if the ids list is null or empty
     */
    @Transactional
    public void deleteSeatForManager(List<UUID> ids, AuthenticatedUser manager)
            throws AccessDeniedException, ValidationException {
        if (ids == null || ids.isEmpty()) {
            LOG.warnf("No seat IDs provided for deletion by manager ID: %s", manager.id());
            throw new ValidationException("No seat IDs provided for deletion");
        }

        LOG.debugf("Attempting to delete seats with IDs: %s for manager ID: %s", ids, manager.id());

        Map<UUID, Seat> seatMap =
                seatRepository.findByIdsWithLocation(ids).stream()
                        .collect(Collectors.toMap(s -> s.id, s -> s));

        for (UUID id : ids) {
            Seat seat = seatMap.get(id);
            if (seat == null) {
                LOG.warnf("Seat with ID %s not found for user ID: %s", id, manager.id());
                throw new SeatNotFoundException("Seat with id " + id + " not found");
            }
            eventLocationAccessService.requireAccess(seat.getLocation(), manager);
        }

        List<Reservation> allReservations =
                reservationRepository.findBySeatIdsWithUserAndEvent(ids);
        List<Reservation> activeReservations =
                allReservations.stream()
                        .filter(r -> r.getStatus() != ReservationStatus.BLOCKED)
                        .toList();

        if (!activeReservations.isEmpty()) {
            Map<Event, List<Reservation>> byEvent =
                    activeReservations.stream()
                            .filter(r -> r.getEvent() != null)
                            .collect(Collectors.groupingBy(Reservation::getEvent));

            Set<UUID> deletingSeatIds = new HashSet<>(ids);

            for (Map.Entry<Event, List<Reservation>> entry : byEvent.entrySet()) {
                Event event = entry.getKey();
                List<Reservation> eventRes = entry.getValue();

                Map<User, List<Reservation>> byUser =
                        eventRes.stream()
                                .filter(r -> r.getUser() != null)
                                .collect(Collectors.groupingBy(Reservation::getUser));

                for (Map.Entry<User, List<Reservation>> userEntry : byUser.entrySet()) {
                    User user = userEntry.getKey();
                    List<Reservation> userDeletedReservations = userEntry.getValue();

                    List<Reservation> allUserEventReservations =
                            reservationRepository.findByUserAndEvent(user, event);
                    List<Reservation> remainingActive =
                            allUserEventReservations.stream()
                                    .filter(
                                            r ->
                                                    r.getStatus() != ReservationStatus.BLOCKED
                                                            && r.getSeat() != null
                                                            && !deletingSeatIds.contains(
                                                                    r.getSeat().id))
                                    .toList();

                    reservationCancelledBus.fire(
                            new ReservationCancelledEvent(
                                    user,
                                    userDeletedReservations,
                                    remainingActive,
                                    "One or more of your reserved seats have been removed by the"
                                            + " event organizer."));
                }

                restoreAllowances(event, eventRes);
            }
        }

        allReservations.forEach(reservationRepository::delete);

        Set<UUID> locationIdsToInvalidate = new HashSet<>();
        for (UUID id : ids) {
            Seat seat = seatMap.get(id);
            seatRepository.delete(seat);
            locationIdsToInvalidate.add(seat.getLocation().getId());
            LOG.infof("Seat ID: %s deleted successfully", seat.id);
        }
        if (!locationIdsToInvalidate.isEmpty()) {
            seatmapCacheService.runAfterSuccessfulCommit(
                    () -> locationIdsToInvalidate.forEach(seatmapCacheService::invalidateSeats));
        }
        LOG.debugf("Seats with IDs %s deleted successfully by manager ID: %s", ids, manager.id());
    }

    /**
     * Restores each affected user's EventUserAllowance.reservationsAllowedCount by one per deleted
     * reservation, since the reservation slot is being freed by a manager-initiated seat deletion
     * without the user having chosen to give it up.
     */
    private void restoreAllowances(Event event, List<Reservation> deletedReservations) {
        if (deletedReservations.isEmpty()) {
            return;
        }

        Set<UUID> userIds =
                deletedReservations.stream()
                        .filter(r -> r.getUser() != null)
                        .map(r -> r.getUser().id)
                        .collect(Collectors.toSet());
        Map<UUID, EventUserAllowance> allowanceByUserId =
                eventUserAllowanceRepository.findByEventAndUserIds(event, userIds).stream()
                        .collect(Collectors.toMap(a -> a.getUser().id, a -> a));

        Map<UUID, EventUserAllowance> updatedAllowancesByUserId = new java.util.LinkedHashMap<>();
        for (Reservation reservation : deletedReservations) {
            if (reservation.getUser() == null) {
                continue;
            }
            EventUserAllowance allowance = allowanceByUserId.get(reservation.getUser().id);
            if (allowance == null) {
                LOG.debugf(
                        "No allowance found for user ID %s and event ID %s, skipping allowance"
                                + " increment.",
                        reservation.getUser().getId(), event.getId());
                continue;
            }
            allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() + 1);
            updatedAllowancesByUserId.put(reservation.getUser().id, allowance);
        }
        if (!updatedAllowancesByUserId.isEmpty()) {
            eventUserAllowanceRepository.persist(
                    new ArrayList<>(updatedAllowancesByUserId.values()));
        }
    }

    /**
     * Finds a seat entity by its ID for a given user. Verifies ownership by checking if the user is
     * an ADMIN or the manager of the seat's event location.
     *
     * @param id the seat ID to find
     * @param currentUser the user attempting to access the seat
     * @return the seat entity
     * @throws SeatNotFoundException if the seat is not found
     * @throws AccessDeniedException if the user does not have permission to access the seat
     */
    public Seat findSeatEntityById(UUID id, AuthenticatedUser currentUser)
            throws SeatNotFoundException, AccessDeniedException {
        LOG.debugf(
                "Attempting to find seat entity by ID: %s for user ID: %s", id, currentUser.id());
        // Check if user has access to linked location
        Seat seat =
                seatRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Seat with ID %s not found for user ID: %s",
                                            id, currentUser.id());
                                    return new SeatNotFoundException(
                                            "Seat with id " + id + " not found");
                                });

        if (currentUser.isAdmin()) {
            LOG.debugf("User is ADMIN, allowing access to seat ID %s.", id);
            return seat; // Admin can access any seat
        }

        try {
            eventLocationAccessService.requireAccess(seat.getLocation(), currentUser);
        } catch (AccessDeniedException e) {
            LOG.warnf(
                    "user ID: %s does not have permission to access seat ID %s.",
                    currentUser.id(), id);
            throw new AccessDeniedException("You do not have permission to access this seat");
        }
        LOG.debugf("user ID: %s has permission to access seat ID %s.", currentUser.id(), id);
        return seat;
    }
}
