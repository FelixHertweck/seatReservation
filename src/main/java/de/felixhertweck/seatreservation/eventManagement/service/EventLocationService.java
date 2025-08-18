/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck_
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.AreaDTO;
import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.common.dto.MarkerDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Area;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Marker;
import de.felixhertweck.seatreservation.model.entity.Point;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.AreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.MarkerRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.security.Roles;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationService {

    private static final Logger LOG = Logger.getLogger(EventLocationService.class);

    @Inject EventLocationRepository eventLocationRepository;

    @Inject SeatRepository seatRepository;

    @Inject MarkerRepository markerRepository;

    @Inject AreaRepository areaRepository;

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

        // Create seats
        if (dto.getSeats() != null) {
            for (SeatDTO seatDTO : dto.getSeats()) {
                Seat seat =
                        new Seat(
                                seatDTO.seatNumber(),
                                location,
                                seatDTO.xCoordinate(),
                                seatDTO.yCoordinate());
                seatRepository.persist(seat);
            }
        }

        // Create markers
        if (dto.getMarkers() != null) {
            for (MarkerDTO markerDTO : dto.getMarkers()) {
                Marker marker =
                        new Marker(markerDTO.label(), markerDTO.x(), markerDTO.y(), location);
                markerRepository.persist(marker);
            }
        }

        // Create areas
        if (dto.getAreas() != null) {
            for (AreaDTO areaDTO : dto.getAreas()) {
                // Convert AreaDTO.pointsDto (List<PointDTO>) to List<Point>
                List<Point> points =
                        areaDTO.points().stream()
                                .map(pointDto -> new Point(pointDto.x(), pointDto.y()))
                                .collect(Collectors.toList());
                Area area = new Area(areaDTO.label(), points, location);
                areaRepository.persist(area);
            }
        }

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

        // Update seats, markers, and areas based on the provided DTO.
        // If a collection is null in the DTO, it will not be changed.
        if (dto.getSeats() != null) {
            updateSeats(location, dto.getSeats());
        }

        if (dto.getMarkers() != null) {
            updateMarkers(location, dto.getMarkers());
        }

        if (dto.getAreas() != null) {
            updateAreas(location, dto.getAreas());
        }

        LOG.infof(
                "Event location '%s' (ID: %d) updated successfully by manager: %s (ID: %d)",
                location.getName(), location.getId(), manager.getUsername(), manager.getId());
        // We need to fetch the location again to get the updated collections
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

    private void updateSeats(EventLocation location, Set<SeatDTO> seatDTOs) {
        Map<String, Seat> existingSeatsMap =
                location.getSeats().stream()
                        .collect(Collectors.toMap(Seat::getSeatNumber, seat -> seat));

        Stream<Seat> updatedAndNewSeats =
                seatDTOs.stream()
                        .map(
                                dto -> {
                                    Seat seat = existingSeatsMap.get(dto.seatNumber());
                                    if (seat != null) {
                                        // Update existing seat
                                        seat.setXCoordinate(dto.xCoordinate());
                                        seat.setYCoordinate(dto.yCoordinate());
                                        return seat;
                                    } else {
                                        // Create new seat
                                        return new Seat(
                                                dto.seatNumber(),
                                                location,
                                                dto.xCoordinate(),
                                                dto.yCoordinate());
                                    }
                                });

        location.getSeats().clear();
        location.getSeats().addAll(updatedAndNewSeats.collect(Collectors.toSet()));
    }

    private void updateMarkers(EventLocation location, Set<MarkerDTO> markerDTOs) {
        Map<String, Marker> existingMarkersMap =
                location.getMarkers().stream()
                        .collect(Collectors.toMap(Marker::getLabel, marker -> marker));

        Stream<Marker> updatedAndNewMarkers =
                markerDTOs.stream()
                        .map(
                                dto -> {
                                    Marker marker = existingMarkersMap.get(dto.label());
                                    if (marker != null) {
                                        // Update existing marker
                                        marker.setX(dto.x());
                                        marker.setY(dto.y());
                                        return marker;
                                    } else {
                                        // Create new marker
                                        return new Marker(dto.label(), dto.x(), dto.y(), location);
                                    }
                                });

        location.getMarkers().clear();
        location.getMarkers().addAll(updatedAndNewMarkers.collect(Collectors.toSet()));
    }

    private void updateAreas(EventLocation location, Set<AreaDTO> areaDTOs) {
        Map<String, Area> existingAreasMap =
                location.getAreas().stream()
                        .collect(Collectors.toMap(Area::getLabel, area -> area));

        Stream<Area> updatedAndNewAreas =
                areaDTOs.stream()
                        .map(
                                dto -> {
                                    Area area = existingAreasMap.get(dto.label());
                                    List<Point> points =
                                            dto.points().stream()
                                                    .map(
                                                            pointDto ->
                                                                    new Point(
                                                                            pointDto.x(),
                                                                            pointDto.y()))
                                                    .collect(Collectors.toList());

                                    if (area != null) {
                                        // Update existing area
                                        area.setPoints(points);
                                        return area;
                                    } else {
                                        // Create new area
                                        return new Area(dto.label(), points, location);
                                    }
                                });

        location.getAreas().clear();
        location.getAreas().addAll(updatedAndNewAreas.collect(Collectors.toSet()));
    }
}
