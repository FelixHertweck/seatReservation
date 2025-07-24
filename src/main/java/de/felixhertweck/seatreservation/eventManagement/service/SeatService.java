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
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.eventManagement.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.SeatResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.security.Roles;

@ApplicationScoped
public class SeatService {

    @Inject SeatRepository seatRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Transactional
    public SeatResponseDTO createSeatManager(SeatRequestDTO dto, User manager) {
        EventLocation eventLocation =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "EventLocation with id "
                                                        + dto.getEventLocationId()
                                                        + " not found"));

        if (!manager.getEventLocations().contains(eventLocation)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("Manager does not own this EventLocation");
        }

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Seat number cannot be empty");
        }
        if (dto.getXCoordinate() < 0 || dto.getYCoordinate() < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative");
        }

        Seat seat = new Seat();
        seat.setSeatNumber(dto.getSeatNumber());
        seat.setLocation(eventLocation);
        seat.setXCoordinate(dto.getXCoordinate());
        seat.setYCoordinate(dto.getYCoordinate());
        seatRepository.persist(seat);
        return new SeatResponseDTO(seat);
    }

    public List<SeatResponseDTO> findAllSeatsForManager(User manager) {
        if (manager.getRoles().contains(Roles.ADMIN)) {
            return seatRepository.listAll().stream()
                    .map(SeatResponseDTO::new)
                    .collect(Collectors.toList());
        }
        Set<EventLocation> managerLocations = manager.getEventLocations();
        return seatRepository.listAll().stream()
                .filter(seat -> managerLocations.contains(seat.getLocation()))
                .map(SeatResponseDTO::new)
                .collect(Collectors.toList());
    }

    public SeatResponseDTO findSeatByIdForManager(Long id, User manager) {
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
        return new SeatResponseDTO(seat);
    }

    @Transactional
    public SeatResponseDTO updateSeatForManager(Long id, SeatRequestDTO dto, User manager) {
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership

        EventLocation newEventLocation =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "EventLocation with id "
                                                        + dto.getEventLocationId()
                                                        + " not found"));

        if (!manager.getRoles().contains(Roles.ADMIN)
                && !manager.getEventLocations().contains(newEventLocation)) {
            throw new SecurityException("Manager does not own the new EventLocation");
        }

        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Seat number cannot be empty");
        }
        if (dto.getXCoordinate() < 0 || dto.getYCoordinate() < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative");
        }

        seat.setSeatNumber(dto.getSeatNumber());
        seat.setLocation(newEventLocation);
        seat.setXCoordinate(dto.getXCoordinate());
        seat.setYCoordinate(dto.getYCoordinate());
        seatRepository.persist(seat);
        return new SeatResponseDTO(seat);
    }

    @Transactional
    public void deleteSeatForManager(Long id, User manager) {
        Seat seat = findSeatEntityById(id, manager); // This already checks for ownership
        seatRepository.delete(seat);
    }

    public Seat findSeatEntityById(Long id, User currentUser) {
        // Check if user has access to linked location
        Seat seat =
                seatRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new SeatNotFoundException(
                                                "Seat with id " + id + " not found"));

        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            return seat; // Admin can access any seat
        }

        if (!seat.getLocation().getManager().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to access this seat");
        }

        return seat;
    }
}
