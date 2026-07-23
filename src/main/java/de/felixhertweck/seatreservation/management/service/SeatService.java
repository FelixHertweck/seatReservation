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

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.management.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SeatService {

    private static final Logger LOG = Logger.getLogger(SeatService.class);

    @Inject SeatRepository seatRepository;

    @Inject EventLocationAccessService eventLocationAccessService;

    @Inject EventLocationAreaRepository eventLocationAreaRepository;

    @Inject EventLocationEntranceRepository eventLocationEntranceRepository;

    /**
     * Creates a new seat for the specified event location by a manager.
     *
     * @param dto the seat request DTO containing seat details
     * @param manager the manager attempting to create the seat
     * @return the created seat DTO
     * @throws IllegalArgumentException if the event location is not found or seat data is invalid
     * @throws SecurityException if the manager does not own the event location
     */
    @Transactional
    public SeatDTO createSeatManager(SeatRequestDTO dto, AuthenticatedUser manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Attempting to create seat with number: %s for event location ID: %d by manager"
                        + " ID: %d",
                dto.getSeatNumber(), dto.getEventLocationId(), manager.id());
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            LOG.warnf(
                    "Invalid seat data: seat number is empty for event location ID %d.",
                    eventLocation.getId());
            throw new IllegalArgumentException("Seat number cannot be empty");
        }
        if (dto.getSeatRow() == null || dto.getSeatRow().trim().isEmpty()) {
            LOG.warnf(
                    "Invalid seat data: seat row is empty for event location ID %d.",
                    eventLocation.getId());
            throw new IllegalArgumentException("Seat row cannot be empty");
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
        LOG.infof(
                "Seat ID: %d created successfully for event location ID %d",
                seat.id, eventLocation.getId());
        LOG.debugf(
                "Seat with ID %d created successfully for event location ID %d by manager ID: %d",
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
            Long eventLocationId, AuthenticatedUser manager) {
        LOG.debugf(
                "Attempting to retrieve seats for event location ID: %d for manager ID: %d",
                eventLocationId, manager.id());
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(eventLocationId, manager);
        List<SeatDTO> result =
                seatRepository.findByEventLocation(eventLocation).stream()
                        .map(SeatDTO::new)
                        .toList();
        LOG.debugf(
                "Retrieved %d seats for event location ID: %d for manager ID: %d",
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
     * @throws SecurityException if the manager does not have permission to access the seat
     */
    public SeatDTO findSeatByIdForManager(Long id, AuthenticatedUser manager)
            throws SeatNotFoundException, SecurityException {
        LOG.debugf("Attempting to retrieve seat with ID: %d for manager ID: %d", id, manager.id());
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
        LOG.debugf("Successfully retrieved seat with ID %d for manager ID: %d", id, manager.id());
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
     * @throws SecurityException if the manager does not own the seat or the new event location
     * @throws IllegalArgumentException if the event location is not found or seat data is invalid
     */
    @Transactional
    public SeatDTO updateSeatForManager(Long id, SeatRequestDTO dto, AuthenticatedUser manager)
            throws SeatNotFoundException, SecurityException, IllegalArgumentException {
        LOG.debugf("Attempting to update seat with ID: %d for manager ID: %d", id, manager.id());
        Seat seat = findSeatEntityById(id, manager);

        EventLocation newEventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            LOG.warnf("Invalid seat data: seat number is empty for seat ID %d.", id);
            throw new IllegalArgumentException("Seat number cannot be empty");
        }
        if (dto.getSeatRow() == null || dto.getSeatRow().trim().isEmpty()) {
            LOG.warnf("Invalid seat data: seat row is empty for seat ID %d.", id);
            throw new IllegalArgumentException("Seat row cannot be empty");
        }

        LOG.debugf(
                "Updating seat ID %d: seatNumber='%s' -> '%s', location ID='%d' -> '%d',"
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

        LOG.infof("Seat ID: %d updated successfully", seat.id);
        LOG.debugf("Seat with ID %d updated successfully by manager ID: %d", id, manager.id());
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
     * @throws IllegalArgumentException if the area does not exist or belongs to another location
     */
    private EventLocationArea resolveArea(Long areaId, EventLocation eventLocation) {
        if (areaId == null) {
            return null;
        }
        EventLocationArea area =
                eventLocationAreaRepository
                        .findByIdOptional(areaId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Area with id " + areaId + " not found"));
        if (!area.getEventLocation().getId().equals(eventLocation.getId())) {
            throw new IllegalArgumentException(
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
     * @throws IllegalArgumentException if the entrance does not exist or belongs to another
     *     location
     */
    private EventLocationEntrance resolveEntrance(Long entranceId, EventLocation eventLocation) {
        if (entranceId == null) {
            return null;
        }
        EventLocationEntrance entrance =
                eventLocationEntranceRepository
                        .findByIdOptional(entranceId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Entrance with id " + entranceId + " not found"));
        if (!entrance.getEventLocation().getId().equals(eventLocation.getId())) {
            throw new IllegalArgumentException(
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
     * @throws SecurityException if the manager does not own any of the seats
     * @throws IllegalArgumentException if the ids list is null or empty
     */
    @Transactional
    public void deleteSeatForManager(List<Long> ids, AuthenticatedUser manager)
            throws SecurityException, IllegalArgumentException {
        if (ids == null || ids.isEmpty()) {
            LOG.warnf("No seat IDs provided for deletion by manager ID: %d", manager.id());
            throw new IllegalArgumentException("No seat IDs provided for deletion");
        }

        LOG.debugf("Attempting to delete seats with IDs: %s for manager ID: %d", ids, manager.id());
        for (Long id : ids) {
            Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
            seatRepository.delete(seat);
            LOG.infof("Seat ID: %d deleted successfully", seat.id);
        }
        LOG.debugf("Seats with IDs %s deleted successfully by manager ID: %d", ids, manager.id());
    }

    /**
     * Finds a seat entity by its ID for a given user. Verifies ownership by checking if the user is
     * an ADMIN or the manager of the seat's event location.
     *
     * @param id the seat ID to find
     * @param currentUser the user attempting to access the seat
     * @return the seat entity
     * @throws SeatNotFoundException if the seat is not found
     * @throws SecurityException if the user does not have permission to access the seat
     */
    public Seat findSeatEntityById(Long id, AuthenticatedUser currentUser)
            throws SeatNotFoundException, SecurityException {
        LOG.debugf(
                "Attempting to find seat entity by ID: %d for user ID: %d", id, currentUser.id());
        // Check if user has access to linked location
        Seat seat =
                seatRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Seat with ID %d not found for user ID: %d",
                                            id, currentUser.id());
                                    return new SeatNotFoundException(
                                            "Seat with id " + id + " not found");
                                });

        if (currentUser.isAdmin()) {
            LOG.debugf("User is ADMIN, allowing access to seat ID %d.", id);
            return seat; // Admin can access any seat
        }

        if (!seat.getLocation().getManager().getId().equals(currentUser.id())) {
            LOG.warnf(
                    "user ID: %d does not have permission to access seat ID %d.",
                    currentUser.id(), id);
            throw new SecurityException("You do not have permission to access this seat");
        }
        LOG.debugf("user ID: %d has permission to access seat ID %d.", currentUser.id(), id);
        return seat;
    }
}
