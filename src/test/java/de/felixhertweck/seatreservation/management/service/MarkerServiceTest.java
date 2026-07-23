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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.management.dto.MakerRequestDTO;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.management.exception.MarkerNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class MarkerServiceTest {

    @InjectMock EventLocationMarkerRepository markerRepository;
    @InjectMock EventLocationRepository eventLocationRepository;

    @Inject MarkerService markerService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser regularAuth;
    private EventLocation eventLocation;
    private EventLocation otherLocation;
    private EventLocationMarker existingMarker;

    @BeforeEach
    void setUp() {
        Mockito.reset(markerRepository, eventLocationRepository);

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

        eventLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        eventLocation.id = id(1);
        otherLocation = new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = id(2);

        existingMarker = new EventLocationMarker("Main Entrance", 10, 20);
        existingMarker.id = id(10);
        existingMarker.setEventLocation(eventLocation);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(eventLocationRepository.findByIdOptional(otherLocation.id))
                .thenReturn(Optional.of(otherLocation));
    }

    @Test
    void createMarker_Success_AsManager() {
        MakerRequestDTO dto =
                new MakerRequestDTO(eventLocation.id, "Stage", new CoordinateDTO(5, 5));

        EventLocationMakerDTO result = markerService.createMarker(dto, managerAuth);

        assertNotNull(result);
        assertEquals("Stage", result.label());
        assertEquals(eventLocation.id, result.eventLocationId());
        verify(markerRepository, times(1)).persist(any(EventLocationMarker.class));
    }

    @Test
    void createMarker_Forbidden_NotManagerOfLocation() {
        MakerRequestDTO dto =
                new MakerRequestDTO(eventLocation.id, "Stage", new CoordinateDTO(5, 5));

        assertThrows(SecurityException.class, () -> markerService.createMarker(dto, regularAuth));
        verify(markerRepository, never()).persist(any(EventLocationMarker.class));
    }

    @Test
    void createMarker_InvalidInput_EmptyLabel() {
        MakerRequestDTO dto = new MakerRequestDTO(eventLocation.id, "  ", new CoordinateDTO(5, 5));

        assertThrows(
                IllegalArgumentException.class, () -> markerService.createMarker(dto, managerAuth));
        verify(markerRepository, never()).persist(any(EventLocationMarker.class));
    }

    @Test
    void createMarker_EventLocationNotFound() {
        when(eventLocationRepository.findByIdOptional(id(999))).thenReturn(Optional.empty());
        MakerRequestDTO dto = new MakerRequestDTO(id(999), "Stage", new CoordinateDTO(5, 5));

        assertThrows(
                EventLocationNotFoundException.class,
                () -> markerService.createMarker(dto, managerAuth));
    }

    @Test
    void findMarkersByLocation_Success() {
        when(markerRepository.findByEventLocation(eventLocation))
                .thenReturn(List.of(existingMarker));

        List<EventLocationMakerDTO> result =
                markerService.findMarkersByLocation(eventLocation.id, managerAuth);

        assertEquals(1, result.size());
        assertEquals("Main Entrance", result.getFirst().label());
    }

    @Test
    void findMarkersByLocation_Forbidden_NotOwner() {
        assertThrows(
                SecurityException.class,
                () -> markerService.findMarkersByLocation(otherLocation.id, managerAuth));
    }

    @Test
    void findMarkersByLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> markerService.findMarkersByLocation(id(999), managerAuth));
    }

    @Test
    void findMarkerByIdForManager_Success() {
        when(markerRepository.findByIdWithEventLocation(existingMarker.id))
                .thenReturn(Optional.of(existingMarker));

        EventLocationMakerDTO result =
                markerService.findMarkerByIdForManager(existingMarker.id, managerAuth);

        assertEquals("Main Entrance", result.label());
    }

    @Test
    void findMarkerByIdForManager_NotFound() {
        when(markerRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                MarkerNotFoundException.class,
                () -> markerService.findMarkerByIdForManager(id(999), managerAuth));
    }

    @Test
    void updateMarker_Success() {
        when(markerRepository.findByIdWithEventLocation(existingMarker.id))
                .thenReturn(Optional.of(existingMarker));
        MakerRequestDTO dto =
                new MakerRequestDTO(eventLocation.id, "Updated", new CoordinateDTO(1, 1));

        EventLocationMakerDTO result =
                markerService.updateMarker(existingMarker.id, dto, managerAuth);

        assertEquals("Updated", result.label());
        verify(markerRepository, times(1)).persist(existingMarker);
    }

    @Test
    void updateMarker_NotFound() {
        when(markerRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());
        MakerRequestDTO dto =
                new MakerRequestDTO(eventLocation.id, "Updated", new CoordinateDTO(1, 1));

        assertThrows(
                MarkerNotFoundException.class,
                () -> markerService.updateMarker(id(999), dto, managerAuth));
    }

    @Test
    void updateMarker_Forbidden_NotManagerOfNewLocation() {
        when(markerRepository.findByIdWithEventLocation(existingMarker.id))
                .thenReturn(Optional.of(existingMarker));
        MakerRequestDTO dto =
                new MakerRequestDTO(otherLocation.id, "Updated", new CoordinateDTO(1, 1));

        assertThrows(
                SecurityException.class,
                () -> markerService.updateMarker(existingMarker.id, dto, managerAuth));
    }

    @Test
    void updateMarker_InvalidInput_EmptyLabel() {
        when(markerRepository.findByIdWithEventLocation(existingMarker.id))
                .thenReturn(Optional.of(existingMarker));
        MakerRequestDTO dto = new MakerRequestDTO(eventLocation.id, "  ", new CoordinateDTO(1, 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> markerService.updateMarker(existingMarker.id, dto, managerAuth));
        verify(markerRepository, never()).persist(any(EventLocationMarker.class));
    }

    @Test
    void deleteMarkers_InvalidInput_EmptyIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> markerService.deleteMarkers(List.of(), managerAuth));
        verify(markerRepository, never()).delete(any(EventLocationMarker.class));
    }

    @Test
    void deleteMarkers_Success() {
        when(markerRepository.findByIdWithEventLocation(existingMarker.id))
                .thenReturn(Optional.of(existingMarker));

        markerService.deleteMarkers(List.of(existingMarker.id), managerAuth);

        verify(markerRepository, times(1)).delete(existingMarker);
    }

    @Test
    void deleteMarkers_NotFound() {
        when(markerRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                MarkerNotFoundException.class,
                () -> markerService.deleteMarkers(List.of(id(999)), managerAuth));
    }

    @Test
    void deleteMarkers_Forbidden() {
        EventLocationMarker markerInOtherLocation = new EventLocationMarker("X", 1, 1);
        markerInOtherLocation.id = id(20);
        markerInOtherLocation.setEventLocation(otherLocation);
        when(markerRepository.findByIdWithEventLocation(markerInOtherLocation.id))
                .thenReturn(Optional.of(markerInOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> markerService.deleteMarkers(List.of(markerInOtherLocation.id), managerAuth));
    }
}
