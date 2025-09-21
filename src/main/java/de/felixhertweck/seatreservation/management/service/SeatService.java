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
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.management.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SeatService {

    private static final Logger LOG = Logger.getLogger(SeatService.class);

    @Inject SeatRepository seatRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Transactional
    public SeatDTO createSeatManager(SeatRequestDTO dto, User manager) {
        LOG.debugf(
                "Attempting to create seat with number: %s for event location ID: %d by manager: %s"
                        + " (ID: %d)",
                dto.getSeatNumber(),
                dto.getEventLocationId(),
                manager.getUsername(),
                manager.getId());
        EventLocation eventLocation =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %d not found for seat creation.",
                                            dto.getEventLocationId());
                                    return new IllegalArgumentException(
                                            "EventLocation with id "
                                                    + dto.getEventLocationId()
                                                    + " not found");
                                });

        if (!eventLocation.getManager().equals(manager)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "Manager %s (ID: %d) does not own EventLocation with ID %d.",
                    manager.getUsername(), manager.getId(), eventLocation.getId());
            throw new SecurityException("Manager does not own this EventLocation");
        }

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            LOG.warnf(
                    "Invalid seat data: seat number is empty for event location ID %d.",
                    eventLocation.getId());
            throw new IllegalArgumentException("Seat number cannot be empty");
        }
        if (dto.getxCoordinate() < 0 || dto.getyCoordinate() < 0) {
            LOG.warnf(
                    "Invalid seat data: coordinates are negative for seat number '%s' in event"
                            + " location ID %d.",
                    dto.getSeatNumber(), eventLocation.getId());
            throw new IllegalArgumentException("Coordinates cannot be negative");
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
                        dto.getxCoordinate(),
                        dto.getyCoordinate());
        seatRepository.persist(seat);
        LOG.infof(
                "Seat %s created successfully for event location ID %d",
                seat.getSeatNumber(), eventLocation.getId());
        LOG.debugf(
                "Seat with ID %d created successfully for event location ID %d by manager: %s (ID:"
                        + " %d)",
                seat.id, eventLocation.getId(), manager.getUsername(), manager.getId());
        return new SeatDTO(seat);
    }

    public List<SeatDTO> findAllSeatsForManager(User manager) {
        LOG.debugf(
                "Attempting to retrieve all seats for manager: %s (ID: %d)",
                manager.getUsername(), manager.getId());
        if (manager.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all seats.");
            return seatRepository.listAll().stream().map(SeatDTO::new).collect(Collectors.toList());
        }
        List<EventLocation> managerLocations;
        if (manager.getRoles().contains(Roles.ADMIN)) {
            managerLocations = new ArrayList<>(eventLocationRepository.listAll());
        } else {
            managerLocations = new ArrayList<>(eventLocationRepository.findByManager(manager));
        }
        List<SeatDTO> result =
                managerLocations.stream()
                        .flatMap(location -> seatRepository.findByEventLocation(location).stream())
                        .map(SeatDTO::new)
                        .collect(Collectors.toList());
        LOG.debugf(
                "Retrieved %d seats for manager: %s (ID: %d)",
                result.size(), manager.getUsername(), manager.getId());
        return result;
    }

    public SeatDTO findSeatByIdForManager(Long id, User manager) {
        LOG.debugf(
                "Attempting to retrieve seat with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
        LOG.debugf(
                "Successfully retrieved seat with ID %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        return new SeatDTO(seat);
    }

    @Transactional
    public SeatDTO updateSeatForManager(Long id, SeatRequestDTO dto, User manager) {
        LOG.debugf(
                "Attempting to update seat with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        Seat seat = findSeatEntityById(id, manager);

        EventLocation newEventLocation =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %d not found for seat update.",
                                            dto.getEventLocationId());
                                    return new IllegalArgumentException(
                                            "EventLocation with id "
                                                    + dto.getEventLocationId()
                                                    + " not found");
                                });

        if (!manager.getRoles().contains(Roles.ADMIN)
                && !newEventLocation.getManager().equals(manager)) {
            LOG.warnf(
                    "Manager %s (ID: %d) does not own the new EventLocation with ID %d for seat"
                            + " update.",
                    manager.getUsername(), manager.getId(), newEventLocation.getId());
            throw new SecurityException("Manager does not own the new EventLocation");
        }

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            LOG.warnf("Invalid seat data: seat number is empty for seat ID %d.", id);
            throw new IllegalArgumentException("Seat number cannot be empty");
        }
        if (dto.getxCoordinate() < 0 || dto.getyCoordinate() < 0) {
            LOG.warnf("Invalid seat data: coordinates are negative for seat ID %d.", id);
            throw new IllegalArgumentException("Coordinates cannot be negative");
        }

        LOG.debugf(
                "Updating seat ID %d: seatNumber='%s' -> '%s', location ID='%d' -> '%d',"
                        + " xCoordinate='%d' -> '%d', yCoordinate='%d' -> '%d'",
                id,
                seat.getSeatNumber(),
                dto.getSeatNumber(),
                seat.getLocation().getId(),
                newEventLocation.getId(),
                seat.getxCoordinate(),
                dto.getxCoordinate(),
                seat.getyCoordinate(),
                dto.getyCoordinate());
        seat.setSeatNumber(dto.getSeatNumber());
        seat.setLocation(newEventLocation);
        seat.setxCoordinate(dto.getxCoordinate());
        seat.setyCoordinate(dto.getyCoordinate());
        seatRepository.persist(seat);
        LOG.infof("Seat %s updated successfully", seat.getSeatNumber());
        LOG.debugf(
                "Seat with ID %d updated successfully by manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        return new SeatDTO(seat);
    }

    @Transactional
    public void deleteSeatForManager(Long id, User manager) {
        LOG.debugf(
                "Attempting to delete seat with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
        seatRepository.delete(seat);
        LOG.infof("Seat %s deleted successfully", seat.getSeatNumber());
        LOG.debugf(
                "Seat with ID %d deleted successfully by manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
    }

    public Seat findSeatEntityById(Long id, User currentUser) {
        LOG.debugf(
                "Attempting to find seat entity by ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        // Check if user has access to linked location
        Seat seat =
                seatRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Seat with ID %d not found for user: %s (ID: %d)",
                                            id, currentUser.getUsername(), currentUser.getId());
                                    return new SeatNotFoundException(
                                            "Seat with id " + id + " not found");
                                });

        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debugf("User is ADMIN, allowing access to seat ID %d.", id);
            return seat; // Admin can access any seat
        }

        if (!seat.getLocation().getManager().getId().equals(currentUser.getId())) {
            LOG.warnf(
                    "User %s (ID: %d) does not have permission to access seat ID %d.",
                    currentUser.getUsername(), currentUser.getId(), id);
            throw new SecurityException("You do not have permission to access this seat");
        }
        LOG.debugf(
                "User %s (ID: %d) has permission to access seat ID %d.",
                currentUser.getUsername(), currentUser.getId(), id);
        return seat;
    }
}
