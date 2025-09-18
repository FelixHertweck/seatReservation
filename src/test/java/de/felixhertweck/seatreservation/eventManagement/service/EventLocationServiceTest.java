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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.eventManagement.service.EventLocationService.InnerSeatInput;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
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

        existingLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        existingLocation.id = 1L;
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsAdmin() {
        List<EventLocation> allLocations =
                List.of(
                        existingLocation,
                        new EventLocation("Another Hall", "Another Street", regularUser, 200));
        when(eventLocationRepository.findAllWithManagerSeats()).thenReturn(allLocations);

        List<EventLocation> result =
                eventLocationService.getEventLocationsByCurrentManager(adminUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventLocationRepository, times(1)).findAllWithManagerSeats();
        verify(eventLocationRepository, never()).findByManagerWithManagerSeats(any(User.class));
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsManager() {
        List<EventLocation> managerLocations = List.of(existingLocation);
        when(eventLocationRepository.findByManagerWithManagerSeats(managerUser))
                .thenReturn(managerLocations);

        List<EventLocation> result =
                eventLocationService.getEventLocationsByCurrentManager(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingLocation.getName(), result.getFirst().getName());
        verify(eventLocationRepository, times(1)).findByManagerWithManagerSeats(managerUser);
        verify(eventLocationRepository, never()).findAllWithManagerSeats();
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsManagerWithNoLocations() {
        when(eventLocationRepository.findByManagerWithManagerSeats(managerUser))
                .thenReturn(Collections.emptyList());

        List<EventLocation> result =
                eventLocationService.getEventLocationsByCurrentManager(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventLocationRepository, times(1)).findByManagerWithManagerSeats(managerUser);
        verify(eventLocationRepository, never()).findAllWithManagerSeats();
    }

    @Test
    void createEventLocation_Success() {
        // Mock persist to do nothing or return the object itself if needed for further operations
        doAnswer(
                        invocation -> {
                            EventLocation loc = invocation.getArgument(0);
                            loc.id = 10L; // Simulate ID generation
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocation createdLocation =
                eventLocationService.createEventLocation(
                        "New Hall", "New Street 1", 500, managerUser);

        assertNotNull(createdLocation);
        assertEquals("New Hall", createdLocation.getName());
        assertEquals("New Street 1", createdLocation.getAddress());
        assertEquals(500, createdLocation.getCapacity());
        assertEquals(managerUser.getUsername(), createdLocation.getManager().getUsername());
        verify(eventLocationRepository, times(1)).persist(any(EventLocation.class));
    }

    @Test
    void createEventLocation_InvalidInput() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        eventLocationService.createEventLocation(
                                null, "Some Address", 100, managerUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void createEventLocation_InvalidInput_NegativeCapacity() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        eventLocationService.createEventLocation(
                                "New Hall", "Some Address", -100, managerUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void updateEventLocation_Success_AsManager() {
        when(eventLocationRepository.findByIdWithManagerSeats(1L))
                .thenReturn(Optional.of(existingLocation));

        EventLocation updatedLocation =
                eventLocationService.updateEventLocation(
                        1L, "Updated Hall", "Updated Street 1", 600, managerUser);

        assertNotNull(updatedLocation);
        assertEquals("Updated Hall", updatedLocation.getName());
        assertEquals("Updated Street 1", updatedLocation.getAddress());
        assertEquals(600, updatedLocation.getCapacity());
        verify(eventLocationRepository, times(1)).persist(existingLocation);
    }

    @Test
    void updateEventLocation_NotFound() {
        when(eventLocationRepository.findByIdWithManagerSeats(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () ->
                        eventLocationService.updateEventLocation(
                                99L, "Updated Hall", "Updated Street 1", 600, managerUser));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void updateEventLocation_Success_AsAdmin() {
        when(eventLocationRepository.findByIdWithManagerSeats(1L))
                .thenReturn(Optional.of(existingLocation));

        EventLocation updatedLocation =
                eventLocationService.updateEventLocation(
                        1L, "Updated Hall by Admin", "Updated Street 1 by Admin", 700, adminUser);

        assertNotNull(updatedLocation);
        assertEquals("Updated Hall by Admin", updatedLocation.getName());
        assertEquals("Updated Street 1 by Admin", updatedLocation.getAddress());
        assertEquals(700, updatedLocation.getCapacity());
        verify(eventLocationRepository, times(1)).persist(existingLocation);
    }

    @Test
    void updateEventLocation_ForbiddenException_NotManagerOrAdmin() {
        when(eventLocationRepository.findByIdWithManagerSeats(1L))
                .thenReturn(Optional.of(existingLocation));

        assertThrows(
                SecurityException.class,
                () ->
                        eventLocationService.updateEventLocation(
                                1L, "Updated Hall", "Updated Street 1", 600, regularUser));
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
        InnerSeatInput seat1 = new InnerSeatInput("A1", 1, 1);

        InnerSeatInput seat2 = new InnerSeatInput("A2", 1, 2);

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
        EventLocation result =
                eventLocationService.importEventLocation(
                        "New Location with Seats",
                        "123 Seat Street",
                        10,
                        List.of(seat1, seat2),
                        managerUser);

        // Assert
        assertNotNull(result);
        assertEquals("New Location with Seats", result.getName());
        assertEquals(10, result.getCapacity());
        assertEquals(managerUser.getUsername(), result.getManager().getUsername());

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
        List<InnerSeatInput> seats = new ArrayList<>();
        seats.add(new InnerSeatInput("B1", 2, 1));
        seats.add(new InnerSeatInput("B2", 2, 2));

        when(eventLocationRepository.findByIdWithManagerSeats(existingLocation.id))
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
        EventLocation result =
                eventLocationService.importSeatsToEventLocation(
                        existingLocation.id, seats, managerUser);

        // Assert
        assertNotNull(result);
        assertEquals(existingLocation.getName(), result.getName());
        assertEquals(2, result.getSeats().size());
        verify(eventLocationRepository, times(1)).findByIdWithManagerSeats(existingLocation.id);
        verify(seatRepository, times(2)).persist(any(Seat.class));
    }

    @Test
    void importSeatsToEventLocation_EventLocationNotFound() {
        // Arrange
        List<InnerSeatInput> seats = new ArrayList<>();
        seats.add(new InnerSeatInput("B1", 2, 1));
        when(eventLocationRepository.findByIdWithManagerSeats(anyLong()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                EventLocationNotFoundException.class,
                () -> eventLocationService.importSeatsToEventLocation(999L, seats, managerUser));
        verify(eventLocationRepository, times(1)).findByIdWithManagerSeats(anyLong());
        verify(seatRepository, never()).persist(any(Seat.class));
    }

    @Test
    void importSeatsToEventLocation_Forbidden() {
        // Arrange
        List<InnerSeatInput> seats = new ArrayList<>();
        seats.add(new InnerSeatInput("B1", 2, 1));
        when(eventLocationRepository.findByIdWithManagerSeats(existingLocation.id))
                .thenReturn(Optional.of(existingLocation));

        // Act & Assert
        assertThrows(
                SecurityException.class,
                () ->
                        eventLocationService.importSeatsToEventLocation(
                                existingLocation.id, seats, regularUser));
        verify(eventLocationRepository, times(1)).findByIdWithManagerSeats(existingLocation.id);
        verify(seatRepository, never()).persist(any(Seat.class));
    }
}
