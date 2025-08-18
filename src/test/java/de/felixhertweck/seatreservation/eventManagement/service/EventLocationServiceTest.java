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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ImportEventLocationDto;
import de.felixhertweck.seatreservation.eventManagement.dto.ImportSeatDto;
import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
public class EventLocationServiceTest {

    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock SeatRepository seatRepository;

    @Inject EventLocationService eventLocationService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private EventLocation existingLocation;

    @BeforeEach
    void setUp() {
        Mockito.reset(eventLocationRepository, userRepository, seatRepository);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        "hash",
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN));
        adminUser.id = 1L;
        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        "hash",
                        "Event",
                        "Manager",
                        Set.of(Roles.MANAGER));
        managerUser.id = 3L;
        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        "hash",
                        "Regular",
                        "User",
                        Set.of(Roles.USER));
        regularUser.id = 2L;

        existingLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        existingLocation.id = 1L;
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsAdmin() {
        List<EventLocation> allLocations =
                List.of(
                        existingLocation,
                        new EventLocation("Another Hall", "Another Street", regularUser, 200));
        when(eventLocationRepository.listAll()).thenReturn(allLocations);

        List<EventLocationResponseDTO> result =
                eventLocationService.getEventLocationsByCurrentManager(adminUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventLocationRepository, times(1)).listAll();
        verify(eventLocationRepository, never()).findByManager(any(User.class));
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsManager() {
        List<EventLocation> managerLocations = List.of(existingLocation);
        when(eventLocationRepository.findByManager(managerUser)).thenReturn(managerLocations);

        List<EventLocationResponseDTO> result =
                eventLocationService.getEventLocationsByCurrentManager(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingLocation.getName(), result.getFirst().name());
        verify(eventLocationRepository, times(1)).findByManager(managerUser);
        verify(eventLocationRepository, never()).listAll();
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsManagerWithNoLocations() {
        when(eventLocationRepository.findByManager(managerUser))
                .thenReturn(Collections.emptyList());

        List<EventLocationResponseDTO> result =
                eventLocationService.getEventLocationsByCurrentManager(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventLocationRepository, times(1)).findByManager(managerUser);
        verify(eventLocationRepository, never()).listAll();
    }

    @Test
    void createEventLocation_Success() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("New Hall");
        dto.setAddress("New Street 1");
        dto.setCapacity(500);

        new EventLocation(dto.getName(), dto.getAddress(), managerUser, dto.getCapacity());
        // Mock persist to do nothing or return the object itself if needed for further operations
        doAnswer(
                        invocation -> {
                            EventLocation loc = invocation.getArgument(0);
                            loc.id = 10L; // Simulate ID generation
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocationResponseDTO createdLocation =
                eventLocationService.createEventLocation(dto, managerUser);

        assertNotNull(createdLocation);
        assertEquals("New Hall", createdLocation.name());
        assertEquals("New Street 1", createdLocation.address());
        assertEquals(500, createdLocation.capacity());
        assertEquals(managerUser.getUsername(), createdLocation.manager().username());
        verify(eventLocationRepository, times(1)).persist(any(EventLocation.class));
    }

    @Test
    void createEventLocation_InvalidInput() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName(null); // Invalid input
        dto.setAddress("Some Address");
        dto.setCapacity(100);

        assertThrows(
                IllegalArgumentException.class,
                () -> eventLocationService.createEventLocation(dto, managerUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void createEventLocation_InvalidInput_NegativeCapacity() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("New Hall");
        dto.setAddress("Some Address");
        dto.setCapacity(-100); // Invalid input

        assertThrows(
                IllegalArgumentException.class,
                () -> eventLocationService.createEventLocation(dto, managerUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void updateEventLocation_Success_AsManager() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Updated Hall");
        dto.setAddress("Updated Street 1");
        dto.setCapacity(600);

        when(eventLocationRepository.findByIdOptional(1L))
                .thenReturn(Optional.of(existingLocation));

        EventLocationResponseDTO updatedLocation =
                eventLocationService.updateEventLocation(1L, dto, managerUser);

        assertNotNull(updatedLocation);
        assertEquals("Updated Hall", updatedLocation.name());
        assertEquals("Updated Street 1", updatedLocation.address());
        assertEquals(600, updatedLocation.capacity());
        verify(eventLocationRepository, times(1)).persist(existingLocation);
    }

    @Test
    void updateEventLocation_NotFound() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Updated Hall");
        dto.setAddress("Updated Street 1");
        dto.setCapacity(600);

        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> eventLocationService.updateEventLocation(99L, dto, managerUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void updateEventLocation_Success_AsAdmin() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Updated Hall by Admin");
        dto.setAddress("Updated Street 1 by Admin");
        dto.setCapacity(700);

        when(eventLocationRepository.findByIdOptional(1L))
                .thenReturn(Optional.of(existingLocation));

        EventLocationResponseDTO updatedLocation =
                eventLocationService.updateEventLocation(1L, dto, adminUser);

        assertNotNull(updatedLocation);
        assertEquals("Updated Hall by Admin", updatedLocation.name());
        assertEquals("Updated Street 1 by Admin", updatedLocation.address());
        assertEquals(700, updatedLocation.capacity());
        verify(eventLocationRepository, times(1)).persist(existingLocation);
    }

    @Test
    void updateEventLocation_ForbiddenException_NotManagerOrAdmin() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Updated Hall");
        dto.setAddress("Updated Street 1");
        dto.setCapacity(600);

        when(eventLocationRepository.findByIdOptional(1L))
                .thenReturn(Optional.of(existingLocation));

        assertThrows(
                SecurityException.class,
                () -> eventLocationService.updateEventLocation(1L, dto, regularUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void deleteEventLocation_Success_AsManager() {
        when(eventLocationRepository.findByIdOptional(1L))
                .thenReturn(Optional.of(existingLocation));
        doNothing().when(eventLocationRepository).delete(any(EventLocation.class));

        eventLocationService.deleteEventLocation(1L, managerUser);

        verify(eventLocationRepository, times(1)).delete(existingLocation);
    }

    @Test
    void deleteEventLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> eventLocationService.deleteEventLocation(99L, managerUser));
        verify(eventLocationRepository, never()).delete(any(EventLocation.class));
    }

    @Test
    void deleteEventLocation_Success_AsAdmin() {
        when(eventLocationRepository.findByIdOptional(1L))
                .thenReturn(Optional.of(existingLocation));
        doNothing().when(eventLocationRepository).delete(any(EventLocation.class));

        eventLocationService.deleteEventLocation(1L, adminUser);

        verify(eventLocationRepository, times(1)).delete(existingLocation);
    }

    @Test
    void deleteEventLocation_ForbiddenException_NotManagerOrAdmin() {
        when(eventLocationRepository.findByIdOptional(1L))
                .thenReturn(Optional.of(existingLocation));

        assertThrows(
                SecurityException.class,
                () -> eventLocationService.deleteEventLocation(1L, regularUser));
        verify(eventLocationRepository, never()).delete(any(EventLocation.class));
    }

    @Test
    void createEventLocationWithSeats_Success() {
        // Arrange
        ImportEventLocationDto dto = new ImportEventLocationDto();
        dto.setName("New Location with Seats");
        dto.setAddress("123 Seat Street");
        dto.setCapacity(10);

        ImportSeatDto seat1 = new ImportSeatDto();
        seat1.setSeatNumber("A1");
        seat1.setXCoordinate(1);
        seat1.setYCoordinate(1);

        ImportSeatDto seat2 = new ImportSeatDto();
        seat2.setSeatNumber("A2");
        seat2.setXCoordinate(1);
        seat2.setYCoordinate(2);

        dto.setSeats(List.of(seat1, seat2));

        doAnswer(
                        invocation -> {
                            EventLocation loc = invocation.getArgument(0);
                            loc.id = 20L; // Simulate ID generation
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));
        doNothing().when(seatRepository).persist(any(Seat.class));

        // Act
        EventLocationResponseDTO result =
                eventLocationService.importEventLocation(dto, managerUser);

        // Assert
        assertNotNull(result);
        assertEquals("New Location with Seats", result.name());
        assertEquals(10, result.capacity());
        assertEquals(managerUser.getUsername(), result.manager().username());

        verify(eventLocationRepository, times(1)).persist(any(EventLocation.class));

        ArgumentCaptor<EventLocation> eventLocationCaptor =
                ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocationRepository, times(1)).persist(eventLocationCaptor.capture());

        EventLocation persistedLocation = eventLocationCaptor.getValue();
        assertNotNull(persistedLocation.getSeats());
        assertEquals(2, persistedLocation.getSeats().size());
        assertEquals("A1", persistedLocation.getSeats().get(0).getSeatNumber());
        assertEquals("A2", persistedLocation.getSeats().get(1).getSeatNumber());
    }

    @Test
    void importSeatsToEventLocation_Success() {
        // Arrange
        Set<ImportSeatDto> seats = new HashSet<>();
        seats.add(new ImportSeatDto("B1", 2, 1));
        seats.add(new ImportSeatDto("B2", 2, 2));

        when(eventLocationRepository.findByIdOptional(existingLocation.id))
                .thenReturn(Optional.of(existingLocation));
        doAnswer(
                        invocation -> {
                            Seat seat = invocation.getArgument(0);
                            seat.id = 100L; // Simulate ID generation
                            return null;
                        })
                .when(seatRepository)
                .persist(any(Seat.class));

        // Act
        EventLocationResponseDTO result =
                eventLocationService.importSeatsToEventLocation(
                        existingLocation.id, seats, managerUser);

        // Assert
        assertNotNull(result);
        assertEquals(existingLocation.getName(), result.name());
        assertEquals(2, result.seats().size());
        verify(eventLocationRepository, times(1)).findByIdOptional(existingLocation.id);
        verify(seatRepository, times(2)).persist(any(Seat.class));
    }

    @Test
    void importSeatsToEventLocation_EventLocationNotFound() {
        // Arrange
        Set<ImportSeatDto> seats = new HashSet<>();
        seats.add(new ImportSeatDto("B1", 2, 1));
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                EventLocationNotFoundException.class,
                () -> eventLocationService.importSeatsToEventLocation(999L, seats, managerUser));
        verify(eventLocationRepository, times(1)).findByIdOptional(anyLong());
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void importSeatsToEventLocation_Forbidden() {
        // Arrange
        Set<ImportSeatDto> seats = new HashSet<>();
        seats.add(new ImportSeatDto("B1", 2, 1));
        when(eventLocationRepository.findByIdOptional(existingLocation.id))
                .thenReturn(Optional.of(existingLocation));

        // Act & Assert
        assertThrows(
                SecurityException.class,
                () ->
                        eventLocationService.importSeatsToEventLocation(
                                existingLocation.id, seats, regularUser));
        verify(eventLocationRepository, times(1)).findByIdOptional(existingLocation.id);
        verify(seatRepository, never()).persist(any(Seat.class));
    }
}
