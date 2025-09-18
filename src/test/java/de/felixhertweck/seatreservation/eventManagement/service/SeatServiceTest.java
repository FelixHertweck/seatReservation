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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class SeatServiceTest {

    @InjectMock SeatRepository seatRepository;
    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock SecurityContext securityContext;

    @Inject SeatService seatService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private EventLocation eventLocation;
    private Seat existingSeat;

    @BeforeEach
    void setUp() {
        Mockito.reset(seatRepository, eventLocationRepository, userRepository, securityContext);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        "hash",
                        "salt",
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN),
                        Set.of());
        adminUser.id = 1L;
        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        "hash",
                        "salt",
                        "Event",
                        "Manager",
                        Set.of(Roles.MANAGER),
                        Set.of());
        managerUser.id = 3L;
        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        "hash",
                        "salt",
                        "Regular",
                        "User",
                        Set.of(Roles.USER),
                        Set.of());
        regularUser.id = 2L;

        eventLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        eventLocation.id = 1L;
        Event dummyEvent = new Event();
        dummyEvent.setEventLocation(eventLocation);
        EventUserAllowance allowance = new EventUserAllowance(managerUser, dummyEvent, 1);
        managerUser.getEventAllowances().add(allowance);

        existingSeat = new Seat();
        existingSeat.setSeatNumber("A1");
        existingSeat.setLocation(eventLocation);
        existingSeat.setxCoordinate(1);
        existingSeat.setyCoordinate(1);
        existingSeat.id = 1L;
    }

    @Test
    void createSeat_Success_AsManager() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("B2");
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(2);
        dto.setyCoordinate(2);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Seat seat = invocation.getArgument(0);
                            seat.id = 10L; // Simulate ID generation
                            return null;
                        })
                .when(seatRepository)
                .persist(any(Seat.class));

        SeatDTO createdSeat = seatService.createSeatManager(dto, managerUser);

        assertNotNull(createdSeat);
        assertEquals("B2", createdSeat.seatNumber());
        assertEquals(eventLocation.id, createdSeat.locationId());
        assertEquals(2, createdSeat.xCoordinate());
        assertEquals(2, createdSeat.yCoordinate());
        verify(seatRepository, times(1)).persist(any(Seat.class));
    }

    @Test
    void createSeat_Success_AsAdmin() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("C3");
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(3);
        dto.setyCoordinate(3);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Seat seat = invocation.getArgument(0);
                            seat.id = 11L; // Simulate ID generation
                            return null;
                        })
                .when(seatRepository)
                .persist(any(Seat.class));

        SeatDTO createdSeat = seatService.createSeatManager(dto, adminUser);

        assertNotNull(createdSeat);
        assertEquals("C3", createdSeat.seatNumber());
        assertEquals(eventLocation.id, createdSeat.locationId());
        assertEquals(3, createdSeat.xCoordinate());
        assertEquals(3, createdSeat.yCoordinate());
        verify(seatRepository, times(1)).persist(any(Seat.class));
    }

    @Test
    void createSeat_ForbiddenException_NotManagerOfLocation() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("D4");
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(4);
        dto.setyCoordinate(4);

        // The eventLocation is managed by managerUser, not regularUser
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                SecurityException.class, () -> seatService.createSeatManager(dto, regularUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void createSeat_InvalidInput() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber(""); // Empty seat number
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(-1); // Negative coordinate
        dto.setyCoordinate(2);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));

        dto.setSeatNumber("C3");
        dto.setxCoordinate(1);
        dto.setyCoordinate(-1); // Negative coordinate
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void findAllSeatsForManager_Success_AsAdmin() {
        EventLocation otherLocation = new EventLocation("Hall 2", "Addr 2", regularUser, 50);
        otherLocation.id = 2L; // Assign an ID for consistency
        List<Seat> allSeats = Arrays.asList(existingSeat, new Seat("C1", otherLocation, 3, 3));
        when(seatRepository.listAll()).thenReturn(allSeats);
        List<SeatDTO> result = seatService.findAllSeatsForManager(adminUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(seatRepository, times(1)).listAll();
    }

    @Test
    void findAllSeatsForManager_Success_AsManager() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        Seat otherSeat = new Seat("X1", otherLocation, 1, 1);
        otherSeat.id = 2L;

        List<Seat> managerSeats = Collections.singletonList(existingSeat);
        when(eventLocationRepository.findByManager(managerUser))
                .thenReturn(Collections.singletonList(eventLocation));
        when(seatRepository.findByEventLocation(eventLocation)).thenReturn(managerSeats);

        List<SeatDTO> result = seatService.findAllSeatsForManager(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size()); // Should only find the one seat they manage
        assertEquals(existingSeat.getSeatNumber(), result.getFirst().seatNumber());
        assertEquals(existingSeat.getxCoordinate(), result.getFirst().xCoordinate());
        assertEquals(existingSeat.getyCoordinate(), result.getFirst().yCoordinate());
        verify(seatRepository, times(1)).findByEventLocation(eventLocation);
    }

    @Test
    void findAllSeatsForManager_Success_NoSeatsForManager() {
        when(eventLocationRepository.findByManager(managerUser))
                .thenReturn(Collections.emptyList());
        // No direct seatRepository.list call when managerLocations is empty

        List<SeatDTO> result = seatService.findAllSeatsForManager(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(seatRepository, never()).findByEventLocation(any(EventLocation.class));
    }

    @Test
    void findSeatByIdForManager_Success_AsAdmin() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        SeatDTO foundSeat = seatService.findSeatByIdForManager(existingSeat.id, adminUser);

        assertNotNull(foundSeat);
        assertEquals(existingSeat.getSeatNumber(), foundSeat.seatNumber());
        assertEquals(existingSeat.getxCoordinate(), foundSeat.xCoordinate());
        assertEquals(existingSeat.getyCoordinate(), foundSeat.yCoordinate());
    }

    @Test
    void findSeatByIdForManager_Success_AsManager() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        SeatDTO foundSeat = seatService.findSeatByIdForManager(existingSeat.id, managerUser);

        assertNotNull(foundSeat);
        assertEquals(existingSeat.getSeatNumber(), foundSeat.seatNumber());
        assertEquals(existingSeat.getxCoordinate(), foundSeat.xCoordinate());
        assertEquals(existingSeat.getyCoordinate(), foundSeat.yCoordinate());
    }

    @Test
    void findSeatByIdForManager_NotFound() {
        when(seatRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                SeatNotFoundException.class,
                () -> seatService.findSeatByIdForManager(99L, managerUser));
    }

    @Test
    void findSeatByIdForManager_ForbiddenException() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        Seat seatInOtherLocation = new Seat();
        seatInOtherLocation.setSeatNumber("X1");
        seatInOtherLocation.setLocation(otherLocation);
        seatInOtherLocation.id = 2L;

        when(seatRepository.findByIdOptional(seatInOtherLocation.id))
                .thenReturn(Optional.of(seatInOtherLocation));
        assertThrows(
                SecurityException.class,
                () -> seatService.findSeatByIdForManager(seatInOtherLocation.id, managerUser));
    }

    @Test
    void updateSeat_Success_AsManager() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated A1");
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(10);
        dto.setyCoordinate(10);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        SeatDTO updatedSeat = seatService.updateSeatForManager(existingSeat.id, dto, managerUser);

        assertNotNull(updatedSeat);
        assertEquals("Updated A1", updatedSeat.seatNumber());
        assertEquals(10, updatedSeat.xCoordinate());
        assertEquals(10, updatedSeat.yCoordinate());
        verify(seatRepository, times(1)).persist(existingSeat);
    }

    @Test
    void updateSeat_Success_AsAdmin() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated A1 by Admin");
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(20);
        dto.setyCoordinate(20);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        SeatDTO updatedSeat = seatService.updateSeatForManager(existingSeat.id, dto, adminUser);

        assertNotNull(updatedSeat);
        assertEquals("Updated A1 by Admin", updatedSeat.seatNumber());
        assertEquals(20, updatedSeat.xCoordinate());
        assertEquals(20, updatedSeat.yCoordinate());
        verify(seatRepository, times(1)).persist(existingSeat);
    }

    @Test
    void updateSeat_NotFound() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated A1");
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(10);
        dto.setyCoordinate(10);

        when(seatRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                SeatNotFoundException.class,
                () -> seatService.updateSeatForManager(99L, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void updateSeat_InvalidInput() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber(""); // Invalid seat number
        dto.setEventLocationId(eventLocation.id);
        dto.setxCoordinate(-1); // Invalid coordinate
        dto.setyCoordinate(10);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));

        dto.setSeatNumber("A1");
        dto.setxCoordinate(1);
        dto.setyCoordinate(-1); // Invalid coordinate
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void updateSeat_ForbiddenException_NotManagerOfSeatLocation() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        Seat seatInOtherLocation = new Seat();
        seatInOtherLocation.setSeatNumber("X1");
        seatInOtherLocation.setLocation(otherLocation);
        seatInOtherLocation.id = 2L;

        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated X1");
        dto.setEventLocationId(otherLocation.id);
        dto.setxCoordinate(10);
        dto.setyCoordinate(10);

        when(seatRepository.findByIdOptional(seatInOtherLocation.id))
                .thenReturn(Optional.of(seatInOtherLocation));
        when(eventLocationRepository.findByIdOptional(otherLocation.id))
                .thenReturn(Optional.of(otherLocation));

        assertThrows(
                SecurityException.class,
                () -> seatService.updateSeatForManager(seatInOtherLocation.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void updateSeat_ForbiddenException_NotManagerOfNewLocation() {
        EventLocation newOtherLocation =
                new EventLocation("New Other Hall", "New Other Address", regularUser, 50);
        newOtherLocation.id = 3L;

        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated A1");
        dto.setEventLocationId(newOtherLocation.id); // New location not owned by manager
        dto.setxCoordinate(10);
        dto.setyCoordinate(10);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(newOtherLocation.id))
                .thenReturn(Optional.of(newOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void deleteSeat_Success_AsManager() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        doNothing().when(seatRepository).delete(any(Seat.class));

        seatService.deleteSeatForManager(existingSeat.id, managerUser);

        verify(seatRepository, times(1)).delete(existingSeat);
    }

    @Test
    void deleteSeat_Success_AsAdmin() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        doNothing().when(seatRepository).delete(any(Seat.class));

        seatService.deleteSeatForManager(existingSeat.id, adminUser);

        verify(seatRepository, times(1)).delete(existingSeat);
    }

    @Test
    void deleteSeat_NotFound() {
        when(seatRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                SeatNotFoundException.class,
                () -> seatService.deleteSeatForManager(99L, managerUser));
        verify(seatRepository, never()).delete(any(Seat.class));
    }

    @Test
    void deleteSeat_ForbiddenException_NotManager() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        Seat seatInOtherLocation = new Seat();
        seatInOtherLocation.setSeatNumber("X1");
        seatInOtherLocation.setLocation(otherLocation);
        seatInOtherLocation.id = 2L;

        when(seatRepository.findByIdOptional(seatInOtherLocation.id))
                .thenReturn(Optional.of(seatInOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> seatService.deleteSeatForManager(seatInOtherLocation.id, managerUser));
        verify(seatRepository, never()).delete(any(Seat.class));
    }

    @Test
    void findSeatEntityById_Success() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        Seat foundSeat = seatService.findSeatEntityById(existingSeat.id, adminUser);

        assertNotNull(foundSeat);
        assertEquals(existingSeat.getSeatNumber(), foundSeat.getSeatNumber());
    }

    @Test
    void findSeatEntityById_ForbiddenException() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));

        assertThrows(
                SecurityException.class,
                () -> seatService.findSeatEntityById(existingSeat.id, regularUser));
    }
}
