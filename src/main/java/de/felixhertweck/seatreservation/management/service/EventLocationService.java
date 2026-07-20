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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.management.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.management.dto.EventLocationUpdateDTO;
import de.felixhertweck.seatreservation.management.dto.ImportAreaDto;
import de.felixhertweck.seatreservation.management.dto.ImportMarkerDto;
import de.felixhertweck.seatreservation.management.dto.ImportSeatDto;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
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
     * Validates that the user has permission to manage the given event location. Permission is
     * granted if the user is the manager of the location or has ADMIN role.
     *
     * @param location The event location to check permissions for
     * @param manager The user attempting the operation
     * @throws SecurityException If the user is not authorized
     */
    private void validateManagerPermission(EventLocation location, User manager) {
        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not the manager of this location");
        }
    }

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
                manager.id, manager.getId());
        List<EventLocation> eventLocations;
        if (manager.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all event locations.");
            eventLocations = eventLocationRepository.listAll();
        } else {
            LOG.debugf(
                    "User is MANAGER, listing event locations for manager ID: %d", manager.getId());
            eventLocations = eventLocationRepository.findByManager(manager);
        }
        LOG.debugf(
                "Retrieved %d event locations for manager: %s (ID: %d)",
                eventLocations.size(), manager.id, manager.getId());
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
                dto.getName(), dto.getAddress(), dto.getCapacity(), manager.id, manager.getId());
        if (dto.getName() == null
                || dto.getName().trim().isEmpty()
                || dto.getAddress() == null
                || dto.getAddress().trim().isEmpty()
                || dto.getCapacity() == null
                || dto.getCapacity() <= 0) {
            LOG.warnf(
                    "Invalid EventLocation data provided by manager: %s (ID: %d)",
                    manager.id, manager.getId());
            throw new IllegalArgumentException("Invalid EventLocation data provided.");
        }

        EventLocation location =
                new EventLocation(dto.getName(), dto.getAddress(), manager, dto.getCapacity());
        // Set markers after location is created to avoid circular dependency
        location.setMarkers(convertToMarkerEntities(dto.getmarkers(), location));
        Map<String, EventLocationArea> areasByName = new LinkedHashMap<>();
        applyAreaDtos(dto.getAreas(), location, areasByName);
        eventLocationRepository.persist(location);

        List<ImportSeatDto> seatDtos = dto.getSeats();
        if (seatDtos != null) {
            Map<String, EventLocationEntrance> entrancesByName = new LinkedHashMap<>();
            List<Seat> seats =
                    seatDtos.stream()
                            .map(
                                    seatDto -> {
                                        Seat seat = new Seat();
                                        seat.setSeatNumber(seatDto.getSeatNumber());
                                        seat.setCoordinate(seatDto.getCoordinate().toEntity());
                                        seat.setSeatRow(seatDto.getSeatRow());
                                        seat.setEntrance(
                                                resolveOrCreateEntrance(
                                                        seatDto.getEntrance(),
                                                        location,
                                                        entrancesByName));
                                        seat.setArea(
                                                resolveOrCreateArea(
                                                        seatDto.getArea(), location, areasByName));
                                        seat.setLocation(location);
                                        seatRepository.persist(seat);
                                        return seat;
                                    })
                            .toList();
            location.setSeats(seats);
        }

        LOG.infof(
                "Event location '%s' (ID: %d) created successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.id, manager.getId());
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
            Long id, EventLocationUpdateDTO dto, User manager)
            throws IllegalArgumentException, SecurityException {
        LOG.debugf(
                "Attempting to update event location with ID: %d for manager: %s (ID: %d)",
                id, manager.id, manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with ID %d not found for update by"
                                                    + " manager: %s (ID: %d)",
                                            id, manager.id, manager.getId());
                                    return new EventLocationNotFoundException(
                                            "EventLocation with id " + id + " not found");
                                });

        try {
            validateManagerPermission(location, manager);
        } catch (SecurityException e) {
            LOG.warnf(
                    "user ID: %d (ID: %d) is not authorized to update event location with ID %d.",
                    manager.id, manager.getId(), id);
            throw e;
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
                location.getName(), location.getId(), manager.id, manager.getId());
        return new EventLocationResponseDTO(location);
    }

    /**
     * Converts a list of marker DTOs to EventLocationMarker entities.
     *
     * @param dtoMarkers The list of marker DTOs to convert
     * @param eventLocation The event location to associate with the markers
     * @return A list of EventLocationMarker entities
     */
    private List<EventLocationMarker> convertToMarkerEntities(
            List<ImportMarkerDto> dtoMarkers, EventLocation eventLocation) {
        if (dtoMarkers == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(
                dtoMarkers.stream()
                        .map(
                                markerDto -> {
                                    EventLocationMarker marker =
                                            new EventLocationMarker(
                                                    markerDto.getLabel(),
                                                    markerDto.getCoordinate().xCoordinate(),
                                                    markerDto.getCoordinate().yCoordinate());
                                    marker.setEventLocation(eventLocation);
                                    return marker;
                                })
                        .toList());
    }

    /**
     * Resolves an {@link EventLocationArea} by (trimmed) name, scoped to the given {@code
     * eventLocation}, creating and registering a new one if no match exists yet. {@code
     * areasByName} is a per-call cache so that a seat and a boundary polygon referencing the same
     * area name resolve to the same entity.
     *
     * @param rawName The (possibly untrimmed) area name; {@code null}/blank resolves to no area
     * @param eventLocation The event location the area belongs to
     * @param areasByName A per-call cache of already-resolved areas, keyed by trimmed name
     * @return The resolved or newly created area, or {@code null} if {@code rawName} is blank
     */
    private EventLocationArea resolveOrCreateArea(
            String rawName,
            EventLocation eventLocation,
            Map<String, EventLocationArea> areasByName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }
        String name = rawName.trim();
        return areasByName.computeIfAbsent(
                name,
                key -> {
                    for (EventLocationArea existing : eventLocation.getAreas()) {
                        if (key.equals(existing.getName())) {
                            return existing;
                        }
                    }
                    EventLocationArea created = new EventLocationArea(key);
                    created.setEventLocation(eventLocation);
                    eventLocation.getAreas().add(created);
                    return created;
                });
    }

    /**
     * Resolves or creates the {@link EventLocationArea} named by each DTO and sets its boundary,
     * mutating {@code eventLocation.getAreas()} via {@link #resolveOrCreateArea}.
     *
     * @param dtoAreas The list of area DTOs to apply
     * @param eventLocation The event location the areas belong to
     * @param areasByName A per-call cache of already-resolved areas, keyed by trimmed name
     * @return The resolved/created areas, in the order of {@code dtoAreas}; never {@code null}
     */
    private List<EventLocationArea> applyAreaDtos(
            List<ImportAreaDto> dtoAreas,
            EventLocation eventLocation,
            Map<String, EventLocationArea> areasByName) {
        List<EventLocationArea> result = new ArrayList<>();
        if (dtoAreas == null) {
            return result;
        }
        for (ImportAreaDto areaDto : dtoAreas) {
            EventLocationArea area =
                    resolveOrCreateArea(areaDto.getName(), eventLocation, areasByName);
            if (area == null) {
                continue;
            }
            area.setBoundary(
                    areaDto.getBoundary() == null
                            ? new ArrayList<>()
                            : areaDto.getBoundary().stream().map(CoordinateDTO::toEntity).toList());
            result.add(area);
        }
        return result;
    }

    /**
     * Resolves an {@link EventLocationEntrance} by (trimmed) name, scoped to the given {@code
     * eventLocation}, creating and registering a new one if no match exists yet. {@code
     * entrancesByName} is a per-call cache so that seats referencing the same entrance name resolve
     * to the same entity.
     *
     * @param rawName The (possibly untrimmed) entrance name; {@code null}/blank resolves to no
     *     entrance
     * @param eventLocation The event location the entrance belongs to
     * @param entrancesByName A per-call cache of already-resolved entrances, keyed by trimmed name
     * @return The resolved or newly created entrance, or {@code null} if {@code rawName} is blank
     */
    private EventLocationEntrance resolveOrCreateEntrance(
            String rawName,
            EventLocation eventLocation,
            Map<String, EventLocationEntrance> entrancesByName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }
        String name = rawName.trim();
        return entrancesByName.computeIfAbsent(
                name,
                key -> {
                    for (EventLocationEntrance existing : eventLocation.getEntrances()) {
                        if (key.equals(existing.getName())) {
                            return existing;
                        }
                    }
                    EventLocationEntrance created = new EventLocationEntrance(key);
                    created.setEventLocation(eventLocation);
                    eventLocation.getEntrances().add(created);
                    return created;
                });
    }

    /**
     * Deletes an EventLocation. The deletion is only allowed if the currently authenticated user is
     * the manager of the EventLocation or has the ADMIN role.
     *
     * @param ids The IDs of the EventLocations to be deleted.
     * @throws IllegalArgumentException If the EventLocation with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to delete the EventLocation.
     */
    @Transactional
    public void deleteEventLocation(List<Long> ids, User manager)
            throws IllegalArgumentException, SecurityException {
        if (ids == null || ids.isEmpty()) {
            LOG.warnf(
                    "No event locations to delete for manager: %s (ID: %d)",
                    manager.id, manager.getId());
            throw new IllegalArgumentException("No event location IDs provided for deletion.");
        }

        LOG.debugf(
                "Attempting to delete event locations with IDs: %s for manager: %s (ID: %d)",
                ids, manager.id, manager.getId());

        for (Long id : ids) {
            EventLocation location =
                    eventLocationRepository
                            .findByIdOptional(id)
                            .orElseThrow(
                                    () -> {
                                        LOG.warnf(
                                                "EventLocation with ID %d not found for deletion by"
                                                        + " manager: %s (ID: %d)",
                                                id, manager.id, manager.getId());
                                        return new EventLocationNotFoundException(
                                                "EventLocation with id " + id + " not found");
                                    });

            try {
                validateManagerPermission(location, manager);
            } catch (SecurityException e) {
                LOG.warnf(
                        "user ID: %d (ID: %d) is not authorized to delete event location with ID"
                                + " %d.",
                        manager.id, manager.getId(), id);
                throw e;
            }

            eventLocationRepository.delete(location);
        }
        LOG.infof("Event locations %s deleted successfully by manager: %s", ids, manager.id);
    }
}
