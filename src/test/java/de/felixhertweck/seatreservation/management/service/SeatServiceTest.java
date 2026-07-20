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
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.management.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Coordinate;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
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
    @InjectMock EventLocationAreaRepository eventLocationAreaRepository;
    @InjectMock EventLocationEntranceRepository eventLocationEntranceRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock SecurityContext securityContext;

    @Inject SeatService seatService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private EventLocation eventLocation;
    private Seat existingSeat;
    private EventLocationArea areaParkett;
    private EventLocationArea areaBalkon;
    private EventLocationEntrance entranceA;
    private EventLocationEntrance entranceB;
    private EventLocationEntrance entranceC;
    private EventLocationArea areaInOtherLocation;
    private EventLocationEntrance entranceInOtherLocation;

    @BeforeEach
    void setUp() {
        Mockito.reset(seatRepository, eventLocationRepository, userRepository, securityContext);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        false,
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
                        false,
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
                        false,
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

        existingSeat = new Seat("A1", "", eventLocation);
        existingSeat.setCoordinate(new Coordinate(1, 1));
        existingSeat.id = 1L;

        areaParkett = new EventLocationArea("Parkett");
        areaParkett.id = 100L;
        areaParkett.setEventLocation(eventLocation);
        areaBalkon = new EventLocationArea("Balkon");
        areaBalkon.id = 101L;
        areaBalkon.setEventLocation(eventLocation);

        entranceA = new EventLocationEntrance("A");
        entranceA.id = 200L;
        entranceA.setEventLocation(eventLocation);
        entranceB = new EventLocationEntrance("B");
        entranceB.id = 201L;
        entranceB.setEventLocation(eventLocation);
        entranceC = new EventLocationEntrance("C");
        entranceC.id = 202L;
        entranceC.setEventLocation(eventLocation);

        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        areaInOtherLocation = new EventLocationArea("Foreign Area");
        areaInOtherLocation.id = 300L;
        areaInOtherLocation.setEventLocation(otherLocation);
        entranceInOtherLocation = new EventLocationEntrance("Foreign Entrance");
        entranceInOtherLocation.id = 301L;
        entranceInOtherLocation.setEventLocation(otherLocation);

        when(eventLocationAreaRepository.findByIdOptional(areaParkett.id))
                .thenReturn(Optional.of(areaParkett));
        when(eventLocationAreaRepository.findByIdOptional(areaBalkon.id))
                .thenReturn(Optional.of(areaBalkon));
        when(eventLocationAreaRepository.findByIdOptional(areaInOtherLocation.id))
                .thenReturn(Optional.of(areaInOtherLocation));
        when(eventLocationEntranceRepository.findByIdOptional(entranceA.id))
                .thenReturn(Optional.of(entranceA));
        when(eventLocationEntranceRepository.findByIdOptional(entranceB.id))
                .thenReturn(Optional.of(entranceB));
        when(eventLocationEntranceRepository.findByIdOptional(entranceC.id))
                .thenReturn(Optional.of(entranceC));
        when(eventLocationEntranceRepository.findByIdOptional(entranceInOtherLocation.id))
                .thenReturn(Optional.of(entranceInOtherLocation));
    }

    @Test
    void createSeat_Success_AsManager() {
        SeatRequestDTO dto =
                new SeatRequestDTO(
                        "B2", "Row 1", eventLocation.id, 2, 2, entranceA.id, areaParkett.id);
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
        assertEquals("Row 1", createdSeat.seatRow());
        assertEquals(2, createdSeat.coordinate().xCoordinate());
        assertEquals(2, createdSeat.coordinate().yCoordinate());
        assertEquals("A", createdSeat.entrance());
        assertEquals("Parkett", createdSeat.area());
        verify(seatRepository, times(1)).persist(any(Seat.class));
    }

    @Test
    void createSeat_Success_AsAdmin() {
        SeatRequestDTO dto =
                new SeatRequestDTO(
                        "C3", "Row 1", eventLocation.id, 3, 3, entranceA.id, areaBalkon.id);

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
        assertEquals("Row 1", createdSeat.seatRow());
        assertEquals(3, createdSeat.coordinate().xCoordinate());
        assertEquals(3, createdSeat.coordinate().yCoordinate());
        assertEquals("A", createdSeat.entrance());
        verify(seatRepository, times(1)).persist(any(Seat.class));
    }

    @Test
    void createSeat_ForbiddenException_NotManagerOfLocation() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("D4");
        dto.setEventLocationId(eventLocation.id);
        dto.setCoordinate(new CoordinateDTO(4, 4));

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
        dto.setCoordinate(new CoordinateDTO(-1, 2)); // Negative coordinate

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));

        dto.setSeatNumber("C3");
        dto.setCoordinate(new CoordinateDTO(1, -1)); // Negative coordinate
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void createSeat_InvalidInput_AreaNotFound() {
        SeatRequestDTO dto = new SeatRequestDTO("D4", "Row 1", eventLocation.id, 1, 1, null, 999L);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(eventLocationAreaRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void createSeat_InvalidInput_AreaBelongsToDifferentLocation() {
        SeatRequestDTO dto =
                new SeatRequestDTO(
                        "D4", "Row 1", eventLocation.id, 1, 1, null, areaInOtherLocation.id);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void createSeat_InvalidInput_EntranceNotFound() {
        SeatRequestDTO dto = new SeatRequestDTO("D4", "Row 1", eventLocation.id, 1, 1, 999L, null);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(eventLocationEntranceRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void createSeat_InvalidInput_EntranceBelongsToDifferentLocation() {
        SeatRequestDTO dto =
                new SeatRequestDTO(
                        "D4", "Row 1", eventLocation.id, 1, 1, entranceInOtherLocation.id, null);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void findSeatsForManagerByLocation_Success_AsAdmin() {
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(seatRepository.findByEventLocation(eventLocation)).thenReturn(List.of(existingSeat));

        List<SeatDTO> result =
                seatService.findSeatsForManagerByLocation(eventLocation.id, adminUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(seatRepository, times(1)).findByEventLocation(eventLocation);
    }

    @Test
    void findSeatsForManagerByLocation_Success_AsManager() {
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(seatRepository.findByEventLocation(eventLocation)).thenReturn(List.of(existingSeat));

        List<SeatDTO> result =
                seatService.findSeatsForManagerByLocation(eventLocation.id, managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingSeat.getSeatNumber(), result.getFirst().seatNumber());
        assertEquals(
                existingSeat.getCoordinate().xCoordinate(),
                result.getFirst().coordinate().xCoordinate());
        assertEquals(
                existingSeat.getCoordinate().yCoordinate(),
                result.getFirst().coordinate().yCoordinate());
        verify(seatRepository, times(1)).findByEventLocation(eventLocation);
    }

    @Test
    void findSeatsForManagerByLocation_Forbidden_NotOwner() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        when(eventLocationRepository.findByIdOptional(otherLocation.id))
                .thenReturn(Optional.of(otherLocation));

        assertThrows(
                SecurityException.class,
                () -> seatService.findSeatsForManagerByLocation(otherLocation.id, managerUser));
    }

    @Test
    void findSeatsForManagerByLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> seatService.findSeatsForManagerByLocation(999L, managerUser));
    }

    @Test
    void findSeatByIdForManager_Success_AsAdmin() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        SeatDTO foundSeat = seatService.findSeatByIdForManager(existingSeat.id, adminUser);

        assertNotNull(foundSeat);
        assertEquals(existingSeat.getSeatNumber(), foundSeat.seatNumber());
        assertEquals(
                existingSeat.getCoordinate().xCoordinate(), foundSeat.coordinate().xCoordinate());
        assertEquals(
                existingSeat.getCoordinate().yCoordinate(), foundSeat.coordinate().yCoordinate());
    }

    @Test
    void findSeatByIdForManager_Success_AsManager() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        SeatDTO foundSeat = seatService.findSeatByIdForManager(existingSeat.id, managerUser);

        assertNotNull(foundSeat);
        assertEquals(existingSeat.getSeatNumber(), foundSeat.seatNumber());
        assertEquals(
                existingSeat.getCoordinate().xCoordinate(), foundSeat.coordinate().xCoordinate());
        assertEquals(
                existingSeat.getCoordinate().yCoordinate(), foundSeat.coordinate().yCoordinate());
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
        Seat seatInOtherLocation = new Seat("X1", "", otherLocation);
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
        dto.setCoordinate(new CoordinateDTO(10, 10));
        dto.setSeatRow("Row 2");
        dto.setEntranceId(entranceB.id);
        dto.setAreaId(areaBalkon.id);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        SeatDTO updatedSeat = seatService.updateSeatForManager(existingSeat.id, dto, managerUser);

        assertNotNull(updatedSeat);
        assertEquals("Updated A1", updatedSeat.seatNumber());
        assertEquals(10, updatedSeat.coordinate().xCoordinate());
        assertEquals(10, updatedSeat.coordinate().yCoordinate());
        assertEquals("Row 2", updatedSeat.seatRow());
        assertEquals("B", updatedSeat.entrance());
        assertEquals("Balkon", updatedSeat.area());
        verify(seatRepository, times(1)).persist(existingSeat);
    }

    @Test
    void updateSeat_EntranceAndRowUpdate_Success() {
        // Set initial values
        existingSeat.setSeatRow("Row 1");
        existingSeat.setEntrance(new EventLocationEntrance("A"));

        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("A1");
        dto.setEventLocationId(eventLocation.id);
        dto.setCoordinate(new CoordinateDTO(1, 1));
        dto.setSeatRow("Row 5");
        dto.setEntranceId(entranceC.id);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        SeatDTO updatedSeat = seatService.updateSeatForManager(existingSeat.id, dto, managerUser);

        assertNotNull(updatedSeat);
        assertEquals("A1", updatedSeat.seatNumber());
        assertEquals("Row 5", updatedSeat.seatRow());
        assertEquals("C", updatedSeat.entrance());
        verify(seatRepository, times(1)).persist(existingSeat);
    }

    @Test
    void updateSeat_InvalidInput_AreaBelongsToDifferentLocation() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("A1");
        dto.setEventLocationId(eventLocation.id);
        dto.setCoordinate(new CoordinateDTO(1, 1));
        dto.setSeatRow("Row 1");
        dto.setAreaId(areaInOtherLocation.id);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void updateSeat_InvalidInput_EntranceBelongsToDifferentLocation() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("A1");
        dto.setEventLocationId(eventLocation.id);
        dto.setCoordinate(new CoordinateDTO(1, 1));
        dto.setSeatRow("Row 1");
        dto.setEntranceId(entranceInOtherLocation.id);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void updateSeat_Success_AsAdmin() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated A1 by Admin");
        dto.setEventLocationId(eventLocation.id);
        dto.setCoordinate(new CoordinateDTO(20, 20));
        dto.setSeatRow("Row 2");
        dto.setEntranceId(entranceB.id);

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        SeatDTO updatedSeat = seatService.updateSeatForManager(existingSeat.id, dto, adminUser);

        assertNotNull(updatedSeat);
        assertEquals("Updated A1 by Admin", updatedSeat.seatNumber());
        assertEquals(20, updatedSeat.coordinate().xCoordinate());
        assertEquals(20, updatedSeat.coordinate().yCoordinate());
        assertEquals("Row 2", updatedSeat.seatRow());
        assertEquals("B", updatedSeat.entrance());
        verify(seatRepository, times(1)).persist(existingSeat);
    }

    @Test
    void updateSeat_NotFound() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated A1");
        dto.setEventLocationId(eventLocation.id);
        dto.setCoordinate(new CoordinateDTO(10, 10));

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
        dto.setCoordinate(new CoordinateDTO(-1, 10)); // Invalid coordinate

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));

        dto.setSeatNumber("A1");
        dto.setCoordinate(new CoordinateDTO(1, -1)); // Invalid coordinate
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));

        dto.setCoordinate(new CoordinateDTO(1, 1));
        dto.setSeatRow(""); // Invalid seat row
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void createSeat_EventLocationNotFound() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Z1");
        dto.setEventLocationId(999L);
        dto.setCoordinate(new CoordinateDTO(1, 1));
        dto.setSeatRow("Row 1");

        when(eventLocationRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> seatService.createSeatManager(dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void updateSeat_EventLocationNotFound() {
        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Z1");
        dto.setEventLocationId(999L);
        dto.setCoordinate(new CoordinateDTO(1, 1));
        dto.setSeatRow("Row 1");

        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        when(eventLocationRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> seatService.updateSeatForManager(existingSeat.id, dto, managerUser));
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void deleteSeat_InvalidInput_EmptyIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> seatService.deleteSeatForManager(List.of(), managerUser));
        verify(seatRepository, never()).delete(any(Seat.class));
    }

    @Test
    void updateSeat_ForbiddenException_NotManagerOfSeatLocation() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        Seat seatInOtherLocation = new Seat("X1", "", otherLocation);
        seatInOtherLocation.id = 2L;

        SeatRequestDTO dto = new SeatRequestDTO();
        dto.setSeatNumber("Updated X1");
        dto.setEventLocationId(otherLocation.id);
        dto.setCoordinate(new CoordinateDTO(10, 10));

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
        dto.setCoordinate(new CoordinateDTO(10, 10));

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

        seatService.deleteSeatForManager(List.of(existingSeat.id), managerUser);

        verify(seatRepository, times(1)).delete(existingSeat);
    }

    @Test
    void deleteSeat_Success_AsAdmin() {
        when(seatRepository.findByIdOptional(existingSeat.id))
                .thenReturn(Optional.of(existingSeat));
        doNothing().when(seatRepository).delete(any(Seat.class));

        seatService.deleteSeatForManager(List.of(existingSeat.id), adminUser);

        verify(seatRepository, times(1)).delete(existingSeat);
    }

    @Test
    void deleteSeat_NotFound() {
        when(seatRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                SeatNotFoundException.class,
                () -> seatService.deleteSeatForManager(List.of(99L), managerUser));
        verify(seatRepository, never()).delete(any(Seat.class));
    }

    @Test
    void deleteSeat_ForbiddenException_NotManager() {
        EventLocation otherLocation =
                new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;
        Seat seatInOtherLocation = new Seat("X1", "", otherLocation);
        seatInOtherLocation.id = 2L;

        when(seatRepository.findByIdOptional(seatInOtherLocation.id))
                .thenReturn(Optional.of(seatInOtherLocation));

        assertThrows(
                SecurityException.class,
                () ->
                        seatService.deleteSeatForManager(
                                List.of(seatInOtherLocation.id), managerUser));
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
