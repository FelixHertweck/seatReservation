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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationService {

    private static final Logger LOG = Logger.getLogger(EventLocationService.class);

    @Inject EventLocationRepository eventLocationRepository;
    @Inject SeatRepository seatRepository;

    /**
     * Retrieves a list of EventLocations belonging to the currently authenticated manager. If the
     * user is an administrator, all EventLocations are returned. Otherwise, only EventLocations
     * whose manager is the current user are returned.
     *
     * @return A list of DTOs representing the EventLocations.
     */
    public List<EventLocation> getEventLocationsByCurrentManager(User manager) {
        LOG.debugf(
                "Attempting to retrieve event locations for manager: %s (ID: %d)",
                manager.getUsername(), manager.getId());
        List<EventLocation> eventLocations;
        if (manager.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all event locations.");
            eventLocations = eventLocationRepository.findAllWithManagerSeats();
        } else {
            LOG.debugf(
                    "User is MANAGER, listing event locations for manager ID: %d", manager.getId());
            eventLocations = eventLocationRepository.findByManagerWithManagerSeats(manager);
        }
        LOG.infof(
                "Retrieved %d event locations for manager: %s (ID: %d)",
                eventLocations.size(), manager.getUsername(), manager.getId());
        return eventLocations;
    }

    /**
     * Creates a new EventLocation and assigns the currently authenticated manager as its creator.
     *
     * @param dto The DTO containing the details of the EventLocation to be created.
     * @return A DTO representing the newly created EventLocation.
     */
    @Transactional
    public EventLocation createEventLocation(
            String name, String address, int capacity, User manager)
            throws IllegalArgumentException {
        LOG.debugf(
                "Attempting to create event location with name: %s, address: %s, capacity: %d for"
                        + " manager: %s (ID: %d)",
                name, address, capacity, manager.getUsername(), manager.getId());
        if (name == null
                || name.trim().isEmpty()
                || address == null
                || address.trim().isEmpty()
                || capacity <= 0) {
            LOG.warnf(
                    "Invalid EventLocation data provided by manager: %s (ID: %d)",
                    manager.getUsername(), manager.getId());
            throw new IllegalArgumentException("Invalid EventLocation data provided.");
        }

        EventLocation location = new EventLocation(name, address, manager, capacity);
        eventLocationRepository.persist(location);
        LOG.infof(
                "Event location '%s' (ID: %d) created successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
        return location;
    }

    /**
     * Updates an existing EventLocation. The update is only allowed if the currently authenticated
     * user is the manager of the EventLocation or has the ADMIN role.
     *
     * @param id The ID of the EventLocation to be updated.
     * @param dto The DTO containing the updated details of the EventLocation.
     * @return A DTO representing the updated EventLocation.
     * @throws IllegalArgumentException If the EventLocation with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to update the EventLocation.
     */
    @Transactional
    public EventLocation updateEventLocation(
            Long id, String name, String address, int capacity, User manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Attempting to update event location with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdWithManagerSeats(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with ID %d not found for update by"
                                                    + " manager: %s (ID: %d)",
                                            id, manager.getUsername(), manager.getId());
                                    return new EventLocationNotFoundException(
                                            "EventLocation with id " + id + " not found");
                                });

        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to update event location with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not the manager of this location");
        }

        LOG.debugf(
                "Updating event location ID %d: name='%s' -> '%s', address='%s' -> '%s',"
                        + " capacity='%d' -> '%d'",
                id,
                location.getName(),
                name,
                location.getAddress(),
                address,
                location.getCapacity(),
                capacity);
        location.setName(name);
        location.setAddress(address);
        location.setCapacity(capacity);
        eventLocationRepository.persist(location);
        LOG.infof(
                "Event location '%s' (ID: %d) updated successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
        return location;
    }

    /**
     * Deletes an EventLocation. The deletion is only allowed if the currently authenticated user is
     * the manager of the EventLocation or has the ADMIN role.
     *
     * @param id The ID of the EventLocation to be deleted.
     * @throws IllegalArgumentException If the EventLocation with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to delete the EventLocation.
     */
    @Transactional
    public void deleteEventLocation(Long id, User manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Attempting to delete event location with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with ID %d not found for deletion by"
                                                    + " manager: %s (ID: %d)",
                                            id, manager.getUsername(), manager.getId());
                                    return new EventLocationNotFoundException(
                                            "EventLocation with id " + id + " not found");
                                });

        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to delete event location with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not the manager of this location");
        }

        eventLocationRepository.delete(location);
        LOG.infof(
                "Event location '%s' (ID: %d) deleted successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
    }

    /**
     * Imports a complete EventLocation with its seats from an import DTO. This method creates a new
     * EventLocation and optionally imports associated seats in a single transaction. The
     * authenticated user will be assigned as the manager of the new EventLocation.
     *
     * @param name The name of the EventLocation to be imported.
     * @param address The address of the EventLocation to be imported.
     * @param capacity The capacity of the EventLocation to be imported.
     * @param seats A list of InnerSeatInput objects containing the seat details to be imported
     * @param manager The currently authenticated user who will become the manager of the
     *     EventLocation.
     * @return A DTO representing the newly imported EventLocation with its seats.
     */
    @Transactional
    public EventLocation importEventLocation(
            String name,
            String address,
            int capacity,
            List<InnerSeatInput> inputSeats,
            User manager) {
        LOG.debugf(
                "Importing event location: %s by manager: %s (ID: %d)",
                name, manager.getUsername(), manager.getId());

        EventLocation location = new EventLocation();
        location.setName(name);
        location.setAddress(address);
        location.setCapacity(capacity);
        location.setManager(manager);

        eventLocationRepository.persist(location);

        if (inputSeats != null) {
            List<Seat> seats =
                    inputSeats.stream()
                            .map(
                                    seatDto -> {
                                        Seat seat = new Seat();
                                        seat.setSeatNumber(seatDto.seatNumber());
                                        seat.setxCoordinate(seatDto.xCoordinate());
                                        seat.setyCoordinate(seatDto.yCoordinate());
                                        seat.setLocation(location);
                                        seatRepository.persist(seat);

                                        return seat;
                                    })
                            .collect(Collectors.toList());
            location.setSeats(seats);
        }

        LOG.infof(
                "Event location '%s' (ID: %d) imported successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
        return location;
    }

    /**
     * Imports seats to an existing EventLocation. This method allows adding multiple seats to an
     * event location. The import is only allowed if the currently authenticated user is the manager
     * of the EventLocation or has the ADMIN role.
     *
     * @param id The ID of the EventLocation to which seats should be imported.
     * @param seats A set of InnerSeatInput objects containing the seat details to be imported.
     * @param manager The currently authenticated user attempting to import seats.
     * @return A DTO representing the updated EventLocation with the newly imported seats.
     * @throws EventLocationNotFoundException If the EventLocation with the specified ID is not
     *     found.
     * @throws SecurityException If the user is not authorized to import seats to this
     *     EventLocation.
     */
    @Transactional
    public EventLocation importSeatsToEventLocation(
            Long id, List<InnerSeatInput> seats, User manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Importing seats to event location with ID: %d by manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdWithManagerSeats(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with ID %d not found for deletion by"
                                                    + " manager: %s (ID: %d)",
                                            id, manager.getUsername(), manager.getId());
                                    return new EventLocationNotFoundException(
                                            "EventLocation with id " + id + " not found");
                                });

        // Check if manager has rights or user is admin
        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to delete event location with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not the manager of this location");
        }

        List<Seat> newSeats = new ArrayList<>();

        seats.forEach(
                seat -> {
                    LOG.debugf(
                            "Importing seat: %s to event location with ID: %d by manager: %s (ID:"
                                    + " %d)",
                            seat, id, manager.getUsername(), manager.getId());
                    Seat newSeat = new Seat();
                    newSeat.setSeatNumber(seat.seatNumber());
                    newSeat.setxCoordinate(seat.xCoordinate());
                    newSeat.setyCoordinate(seat.yCoordinate());
                    newSeat.setLocation(location);
                    seatRepository.persist(newSeat);
                    newSeats.add(newSeat);
                });

        location.getSeats().addAll(newSeats);

        return location;
    }

    public record InnerSeatInput(String seatNumber, int xCoordinate, int yCoordinate) {}
}
