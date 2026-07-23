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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
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
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser regularAuth;
    private EventLocation existingLocation;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        Mockito.reset(eventLocationRepository, userRepository, seatRepository);

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
        adminUser.id = id(1);
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
        managerUser.id = id(3);
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
        regularUser.id = id(2);

        adminAuth = new AuthenticatedUser(adminUser.id, adminUser.getRoles());
        managerAuth = new AuthenticatedUser(managerUser.id, managerUser.getRoles());
        regularAuth = new AuthenticatedUser(regularUser.id, regularUser.getRoles());

        when(userRepository.getReference(managerUser.id)).thenReturn(managerUser);
        when(userRepository.getReference(regularUser.id)).thenReturn(regularUser);

        existingLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        existingLocation.id = id(1);
    }

    @Test
    void getEventLocationsByCurrentManager_Success_AsAdmin() {
        List<EventLocation> allLocations =
                List.of(
                        existingLocation,
                        new EventLocation("Another Hall", "Another Street", regularUser, 200));
        when(eventLocationRepository.listAll()).thenReturn(allLocations);

        List<EventLocationResponseDTO> result =
                eventLocationService.getEventLocationsByCurrentManager(adminAuth);

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
                eventLocationService.getEventLocationsByCurrentManager(managerAuth);

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
                eventLocationService.getEventLocationsByCurrentManager(managerAuth);

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
                            loc.id = id(10); // Simulate ID generation
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocationResponseDTO createdLocation =
                eventLocationService.createEventLocation(dto, managerAuth);

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
                () -> eventLocationService.createEventLocation(dto, managerAuth));
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
                () -> eventLocationService.createEventLocation(dto, managerAuth));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void updateEventLocation_Success_AsManager() {
        EventLocationUpdateDTO dto = new EventLocationUpdateDTO();
        dto.setName("Updated Hall");
        dto.setAddress("Updated Street 1");
        dto.setCapacity(600);

        when(eventLocationRepository.findByIdOptional(id(1)))
                .thenReturn(Optional.of(existingLocation));

        EventLocationResponseDTO updatedLocation =
                eventLocationService.updateEventLocation(id(1), dto, managerAuth);

        assertNotNull(updatedLocation);
        assertEquals("Updated Hall", updatedLocation.name());
        assertEquals("Updated Street 1", updatedLocation.address());
        assertEquals(600, updatedLocation.capacity());
        verify(eventLocationRepository, times(1)).persist(existingLocation);
    }

    @Test
    void updateEventLocation_NotFound() {
        EventLocationUpdateDTO dto = new EventLocationUpdateDTO();
        dto.setName("Updated Hall");
        dto.setAddress("Updated Street 1");
        dto.setCapacity(600);

        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> eventLocationService.updateEventLocation(id(99), dto, managerAuth));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void updateEventLocation_Success_AsAdmin() {
        EventLocationUpdateDTO dto = new EventLocationUpdateDTO();
        dto.setName("Updated Hall by Admin");
        dto.setAddress("Updated Street 1 by Admin");
        dto.setCapacity(700);

        when(eventLocationRepository.findByIdOptional(id(1)))
                .thenReturn(Optional.of(existingLocation));

        EventLocationResponseDTO updatedLocation =
                eventLocationService.updateEventLocation(id(1), dto, adminAuth);

        assertNotNull(updatedLocation);
        assertEquals("Updated Hall by Admin", updatedLocation.name());
        assertEquals("Updated Street 1 by Admin", updatedLocation.address());
        assertEquals(700, updatedLocation.capacity());
        verify(eventLocationRepository, times(1)).persist(existingLocation);
    }

    @Test
    void updateEventLocation_ForbiddenException_NotManagerOrAdmin() {
        EventLocationUpdateDTO dto = new EventLocationUpdateDTO();
        dto.setName("Updated Hall");
        dto.setAddress("Updated Street 1");
        dto.setCapacity(600);

        when(eventLocationRepository.findByIdOptional(id(1)))
                .thenReturn(Optional.of(existingLocation));

        assertThrows(
                SecurityException.class,
                () -> eventLocationService.updateEventLocation(id(1), dto, regularAuth));
        verify(eventLocationRepository, never()).persist(any(EventLocation.class));
    }

    @Test
    void deleteEventLocation_Success_AsManager() {
        when(eventLocationRepository.findByIdOptional(id(1)))
                .thenReturn(Optional.of(existingLocation));
        doNothing().when(eventLocationRepository).delete(any(EventLocation.class));

        eventLocationService.deleteEventLocation(List.of(id(1)), managerAuth);

        verify(eventLocationRepository, times(1)).delete(existingLocation);
    }

    @Test
    void deleteEventLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> eventLocationService.deleteEventLocation(List.of(id(99)), managerAuth));
        verify(eventLocationRepository, never()).delete(any(EventLocation.class));
    }

    @Test
    void deleteEventLocation_Success_AsAdmin() {
        when(eventLocationRepository.findByIdOptional(id(1)))
                .thenReturn(Optional.of(existingLocation));
        doNothing().when(eventLocationRepository).delete(any(EventLocation.class));

        eventLocationService.deleteEventLocation(List.of(id(1)), adminAuth);

        verify(eventLocationRepository, times(1)).delete(existingLocation);
    }

    @Test
    void deleteEventLocation_ForbiddenException_NotManagerOrAdmin() {
        when(eventLocationRepository.findByIdOptional(id(1)))
                .thenReturn(Optional.of(existingLocation));

        assertThrows(
                SecurityException.class,
                () -> eventLocationService.deleteEventLocation(List.of(id(1)), regularAuth));
        verify(eventLocationRepository, never()).delete(any(EventLocation.class));
    }

    @Test
    void createEventLocation_WithSeats_Success() {
        // Arrange
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("New Location with Seats");
        dto.setAddress("123 Seat Street");
        dto.setCapacity(10);

        ImportSeatDto seat1 = new ImportSeatDto();
        seat1.setSeatNumber("A1");
        seat1.setCoordinate(new CoordinateDTO(1, 1));
        seat1.setSeatRow("Row A");
        seat1.setEntrance("Main");
        seat1.setArea("Parkett");

        ImportSeatDto seat2 = new ImportSeatDto();
        seat2.setSeatNumber("A2");
        seat2.setCoordinate(new CoordinateDTO(1, 2));
        seat2.setSeatRow("Row A");
        seat2.setEntrance("Main");
        seat2.setArea("Parkett");

        dto.setSeats(List.of(seat1, seat2));

        doAnswer(
                        invocation -> {
                            EventLocation loc = invocation.getArgument(0);
                            loc.id = id(20); // Simulate ID generation
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));
        doNothing().when(seatRepository).persist(any(Seat.class));

        // Act
        EventLocationResponseDTO result =
                eventLocationService.createEventLocation(dto, managerAuth);

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
        assertEquals("Main", persistedLocation.getSeats().get(0).getEntrance().getName());
        assertEquals("Parkett", persistedLocation.getSeats().get(0).getArea().getName());
        assertEquals("Parkett", persistedLocation.getSeats().get(1).getArea().getName());

        // Seats sharing an area are grouped into a single AreaDTO on the response
        assertEquals(1, result.areas().size());
        assertEquals("Parkett", result.areas().get(0).name());
        assertEquals(2, result.areas().get(0).seatIds().size());
    }

    @Test
    void createEventLocation_WithMarkers_Success() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Concert Hall");
        dto.setAddress("Music Street 1");
        dto.setCapacity(300);

        List<ImportMarkerDto> markers =
                List.of(
                        new ImportMarkerDto("Main Entrance", 100, 200),
                        new ImportMarkerDto("Emergency Exit", 50, 250),
                        new ImportMarkerDto("Stage", 150, 50));
        dto.setmarkers(markers);

        doAnswer(
                        invocation -> {
                            EventLocation location = invocation.getArgument(0);
                            location.id = id(15);
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocationResponseDTO result =
                eventLocationService.createEventLocation(dto, managerAuth);

        assertNotNull(result);
        assertEquals("Concert Hall", result.name());
        assertEquals(3, result.markers().size());

        // Verify marker content
        assertEquals("Main Entrance", result.markers().get(0).label());
        assertEquals(100, result.markers().get(0).coordinate().xCoordinate());
        assertEquals(200, result.markers().get(0).coordinate().yCoordinate());

        assertEquals("Emergency Exit", result.markers().get(1).label());
        assertEquals("Stage", result.markers().get(2).label());

        ArgumentCaptor<EventLocation> locationCaptor = ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocationRepository, times(1)).persist(locationCaptor.capture());

        EventLocation capturedLocation = locationCaptor.getValue();
        assertEquals(3, capturedLocation.getMarkers().size());
    }

    @Test
    void createEventLocation_WithNullMarkers_Success() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Simple Hall");
        dto.setAddress("Simple Street 1");
        dto.setCapacity(100);
        dto.setmarkers(null);

        doAnswer(
                        invocation -> {
                            EventLocation location = invocation.getArgument(0);
                            location.id = id(16);
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocationResponseDTO result =
                eventLocationService.createEventLocation(dto, managerAuth);

        assertNotNull(result);
        assertEquals("Simple Hall", result.name());
        assertTrue(result.markers().isEmpty());

        ArgumentCaptor<EventLocation> locationCaptor = ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocationRepository, times(1)).persist(locationCaptor.capture());

        EventLocation capturedLocation = locationCaptor.getValue();
        assertTrue(capturedLocation.getMarkers().isEmpty());
    }

    @Test
    void createEventLocation_WithEmptyMarkers_Success() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Empty Markers Hall");
        dto.setAddress("Empty Street 1");
        dto.setCapacity(150);
        dto.setmarkers(Collections.emptyList());

        doAnswer(
                        invocation -> {
                            EventLocation location = invocation.getArgument(0);
                            location.id = id(17);
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocationResponseDTO result =
                eventLocationService.createEventLocation(dto, managerAuth);

        assertNotNull(result);
        assertEquals("Empty Markers Hall", result.name());
        assertTrue(result.markers().isEmpty());
    }

    @Test
    void convertToMarkerEntities_ValidInput() {
        // This is indirectly tested through the create and update tests, but let's add specific
        // validation
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Test Hall");
        dto.setAddress("Test Street");
        dto.setCapacity(100);

        List<ImportMarkerDto> markerDtos =
                List.of(
                        new ImportMarkerDto("Test Marker 1", 0, 0),
                        new ImportMarkerDto("Test Marker 2", -50, -100),
                        new ImportMarkerDto("Test Marker 3", Integer.MAX_VALUE, Integer.MIN_VALUE));
        dto.setmarkers(markerDtos);

        doAnswer(
                        invocation -> {
                            EventLocation location = invocation.getArgument(0);
                            location.id = id(18);
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        EventLocationResponseDTO result =
                eventLocationService.createEventLocation(dto, managerAuth);

        assertNotNull(result);
        assertEquals(3, result.markers().size());

        // Test boundary values are preserved
        assertEquals(0, result.markers().get(0).coordinate().xCoordinate());
        assertEquals(0, result.markers().get(0).coordinate().yCoordinate());
        assertEquals(-50, result.markers().get(1).coordinate().xCoordinate());
        assertEquals(-100, result.markers().get(1).coordinate().yCoordinate());
        assertEquals(Integer.MAX_VALUE, result.markers().get(2).coordinate().xCoordinate());
        assertEquals(Integer.MIN_VALUE, result.markers().get(2).coordinate().yCoordinate());
    }

    @Test
    void createEventLocation_WithAreas_Success() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Concert Hall");
        dto.setAddress("Music Street 1");
        dto.setCapacity(300);

        List<ImportAreaDto> areas =
                List.of(
                        new ImportAreaDto(
                                "Parkett",
                                List.of(
                                        new CoordinateDTO(1, 1),
                                        new CoordinateDTO(5, 1),
                                        new CoordinateDTO(5, 5))),
                        new ImportAreaDto(
                                "Balkon",
                                List.of(new CoordinateDTO(1, 10), new CoordinateDTO(5, 10))));
        dto.setAreas(areas);

        doAnswer(
                        invocation -> {
                            EventLocation location = invocation.getArgument(0);
                            location.id = id(19);
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        eventLocationService.createEventLocation(dto, managerAuth);

        ArgumentCaptor<EventLocation> locationCaptor = ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocationRepository, times(1)).persist(locationCaptor.capture());

        List<EventLocationArea> persistedAreas = locationCaptor.getValue().getAreas();
        assertEquals(2, persistedAreas.size());

        EventLocationArea parkett =
                persistedAreas.stream()
                        .filter(a -> "Parkett".equals(a.getName()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(3, parkett.getBoundary().size());
        assertEquals(1, parkett.getBoundary().get(0).xCoordinate());
        assertEquals(1, parkett.getBoundary().get(0).yCoordinate());

        EventLocationArea balkon =
                persistedAreas.stream()
                        .filter(a -> "Balkon".equals(a.getName()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(2, balkon.getBoundary().size());
    }

    @Test
    void createEventLocation_WithNullAreas_Success() {
        EventLocationRequestDTO dto = new EventLocationRequestDTO();
        dto.setName("Simple Hall");
        dto.setAddress("Simple Street 1");
        dto.setCapacity(100);
        dto.setAreas(null);

        doAnswer(
                        invocation -> {
                            EventLocation location = invocation.getArgument(0);
                            location.id = id(20);
                            return null;
                        })
                .when(eventLocationRepository)
                .persist(any(EventLocation.class));

        eventLocationService.createEventLocation(dto, managerAuth);

        ArgumentCaptor<EventLocation> locationCaptor = ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocationRepository, times(1)).persist(locationCaptor.capture());
        assertTrue(locationCaptor.getValue().getAreas().isEmpty());
    }
}
