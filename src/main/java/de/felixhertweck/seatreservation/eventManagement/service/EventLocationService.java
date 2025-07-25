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

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRegistrationDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.security.Roles;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationService {

    private static final Logger LOG = Logger.getLogger(EventLocationService.class);

    @Inject EventLocationRepository eventLocationRepository;

    /**
     * Retrieves a list of EventLocations belonging to the currently authenticated manager. If the
     * user is an administrator, all EventLocations are returned. Otherwise, only EventLocations
     * whose manager is the current user are returned.
     *
     * @return A list of DTOs representing the EventLocations.
     */
    public List<EventLocationResponseDTO> getEventLocationsByCurrentManager(User manager) {
        LOG.debugf(
                "Attempting to retrieve event locations for manager: %s (ID: %d)",
                manager.getUsername(), manager.getId());
        List<EventLocation> eventLocations;
        if (manager.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all event locations.");
            eventLocations = eventLocationRepository.listAll();
        } else {
            LOG.debugf(
                    "User is MANAGER, listing event locations for manager ID: %d", manager.getId());
            eventLocations = eventLocationRepository.findByManager(manager);
        }
        LOG.infof(
                "Retrieved %d event locations for manager: %s (ID: %d)",
                eventLocations.size(), manager.getUsername(), manager.getId());
        return eventLocations.stream()
                .map(EventLocationResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new EventLocation and assigns the currently authenticated manager as its creator.
     *
     * @param dto The DTO containing the details of the EventLocation to be created.
     * @return A DTO representing the newly created EventLocation.
     */
    @Transactional
    public EventLocationResponseDTO createEventLocation(EventLocationRequestDTO dto, User manager)
            throws IllegalArgumentException {
        LOG.debugf(
                "Attempting to create event location with name: %s, address: %s, capacity: %d for"
                        + " manager: %s (ID: %d)",
                dto.getName(),
                dto.getAddress(),
                dto.getCapacity(),
                manager.getUsername(),
                manager.getId());
        if (dto.getName() == null
                || dto.getName().trim().isEmpty()
                || dto.getAddress() == null
                || dto.getAddress().trim().isEmpty()
                || dto.getCapacity() == null
                || dto.getCapacity() <= 0) {
            LOG.warnf(
                    "Invalid EventLocation data provided by manager: %s (ID: %d)",
                    manager.getUsername(), manager.getId());
            throw new IllegalArgumentException("Invalid EventLocation data provided.");
        }

        EventLocation location =
                new EventLocation(dto.getName(), dto.getAddress(), manager, dto.getCapacity());
        eventLocationRepository.persist(location);
        LOG.infof(
                "Event location '%s' (ID: %d) created successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
        return new EventLocationResponseDTO(location);
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
    public EventLocationResponseDTO updateEventLocation(
            Long id, EventLocationRequestDTO dto, User manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Attempting to update event location with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
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
                dto.getName(),
                location.getAddress(),
                dto.getAddress(),
                location.getCapacity(),
                dto.getCapacity());
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setCapacity(dto.getCapacity());
        eventLocationRepository.persist(location);
        LOG.infof(
                "Event location '%s' (ID: %d) updated successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
        return new EventLocationResponseDTO(location);
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
     * Creates a new EventLocation with seats. The manager of the EventLocation is set to the
     * currently authenticated user.
     *
     * @param dto The DTO containing the details of the EventLocation and its seats.
     * @param manager The currently authenticated user who is the manager of the EventLocation.
     * @throws IllegalArgumentException If the provided data is invalid.
     * @throws SecurityException If the user is not authorized to create the EventLocation.
     * @return A DTO representing the newly created EventLocation with its seats.
     */
    @Transactional
    public EventLocationResponseDTO createEventLocationWithSeats(
            EventLocationRegistrationDTO dto, User manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Attempting to create event location with seats for manager: %s (ID: %d)",
                manager.getUsername(), manager.getId());
        EventLocationRegistrationDTO.EventLocationData locationData = dto.getEventLocation();
        if (locationData == null
                || locationData.getName() == null
                || locationData.getName().trim().isEmpty()
                || locationData.getAddress() == null
                || locationData.getAddress().trim().isEmpty()
                || locationData.getCapacity() == null
                || locationData.getCapacity() <= 0) {
            LOG.warnf(
                    "Invalid EventLocation data provided for creation with seats by manager: %s"
                            + " (ID: %d)",
                    manager.getUsername(), manager.getId());
            throw new IllegalArgumentException("Invalid EventLocation data provided.");
        }

        EventLocation location =
                new EventLocation(
                        locationData.getName(),
                        locationData.getAddress(),
                        manager,
                        locationData.getCapacity());
        eventLocationRepository.persist(location);
        LOG.debugf(
                "Event location '%s' (ID: %d) persisted. Processing seats...",
                location.getName(), location.getId());

        if (dto.getSeats() != null) {
            for (EventLocationRegistrationDTO.SeatData seatData : dto.getSeats()) {
                if (seatData.getSeatNumber() == null || seatData.getSeatNumber().trim().isEmpty()) {
                    LOG.warnf(
                            "Invalid seat data: seat number is empty for event location '%s' (ID:"
                                    + " %d)",
                            location.getName(), location.getId());
                    throw new IllegalArgumentException("Seat number cannot be empty");
                }
                if (seatData.getXCoordinate() < 0 || seatData.getYCoordinate() < 0) {
                    LOG.warnf(
                            "Invalid seat data: coordinates are negative for seat '%s' in event"
                                    + " location '%s' (ID: %d)",
                            seatData.getSeatNumber(), location.getName(), location.getId());
                    throw new IllegalArgumentException("Coordinates cannot be negative");
                }
                Seat seat =
                        new Seat(
                                seatData.getSeatNumber(),
                                location,
                                seatData.getXCoordinate(),
                                seatData.getYCoordinate());
                location.getSeats().add(seat);
                LOG.debugf(
                        "Added seat '%s' (X:%d, Y:%d) to event location '%s' (ID: %d)",
                        seatData.getSeatNumber(),
                        seatData.getXCoordinate(),
                        seatData.getYCoordinate(),
                        location.getName(),
                        location.getId());
            }
        }

        eventLocationRepository.persist(location);
        LOG.infof(
                "Event location '%s' (ID: %d) with %d seats created successfully by manager: %s"
                        + " (ID: %d)",
                location.getName(),
                location.getId(),
                location.getSeats().size(),
                manager.getUsername(),
                manager.getId());
        return new EventLocationResponseDTO(location);
    }
}
