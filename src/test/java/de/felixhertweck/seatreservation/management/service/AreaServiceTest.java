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
import de.felixhertweck.seatreservation.management.dto.AreaRequestDTO;
import de.felixhertweck.seatreservation.management.dto.AreaResponseDTO;
import de.felixhertweck.seatreservation.management.exception.AreaInUseException;
import de.felixhertweck.seatreservation.management.exception.AreaNotFoundException;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AreaServiceTest {

    @InjectMock EventLocationAreaRepository areaRepository;
    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock SeatRepository seatRepository;

    @Inject AreaService areaService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser regularAuth;
    private EventLocation eventLocation;
    private EventLocation otherLocation;
    private EventLocation secondOwnedLocation;
    private EventLocationArea existingArea;

    @BeforeEach
    void setUp() {
        Mockito.reset(areaRepository, eventLocationRepository, seatRepository);

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

        secondOwnedLocation = new EventLocation("Zweite Halle", "Nebenstraße 2", managerUser, 80);
        secondOwnedLocation.id = id(3);

        existingArea = new EventLocationArea("Parkett");
        existingArea.id = id(10);
        existingArea.setEventLocation(eventLocation);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(eventLocationRepository.findByIdOptional(otherLocation.id))
                .thenReturn(Optional.of(otherLocation));
        when(eventLocationRepository.findByIdOptional(secondOwnedLocation.id))
                .thenReturn(Optional.of(secondOwnedLocation));
    }

    @Test
    void createArea_Success_AsManager() {
        AreaRequestDTO dto =
                new AreaRequestDTO(eventLocation.id, "Balkon", List.of(new CoordinateDTO(1, 1)));

        AreaResponseDTO result = areaService.createArea(dto, managerAuth);

        assertNotNull(result);
        assertEquals("Balkon", result.name());
        assertEquals(eventLocation.id, result.eventLocationId());
        verify(areaRepository, times(1)).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_Success_AsAdmin() {
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "Balkon", List.of());

        AreaResponseDTO result = areaService.createArea(dto, adminAuth);

        assertNotNull(result);
        verify(areaRepository, times(1)).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_Forbidden_NotManagerOfLocation() {
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "Balkon", List.of());

        assertThrows(SecurityException.class, () -> areaService.createArea(dto, regularAuth));
        verify(areaRepository, never()).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_InvalidInput_EmptyName() {
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "  ", List.of());

        assertThrows(
                IllegalArgumentException.class, () -> areaService.createArea(dto, managerAuth));
        verify(areaRepository, never()).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_EventLocationNotFound() {
        when(eventLocationRepository.findByIdOptional(id(999))).thenReturn(Optional.empty());
        AreaRequestDTO dto = new AreaRequestDTO(id(999), "Balkon", List.of());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> areaService.createArea(dto, managerAuth));
    }

    @Test
    void findAreasByLocation_Success_AsManager() {
        when(areaRepository.findByEventLocation(eventLocation)).thenReturn(List.of(existingArea));

        List<AreaResponseDTO> result =
                areaService.findAreasByLocation(eventLocation.id, managerAuth);

        assertEquals(1, result.size());
        assertEquals("Parkett", result.getFirst().name());
    }

    @Test
    void findAreasByLocation_Success_AsAdmin() {
        when(areaRepository.findByEventLocation(eventLocation)).thenReturn(List.of(existingArea));

        List<AreaResponseDTO> result = areaService.findAreasByLocation(eventLocation.id, adminAuth);

        assertEquals(1, result.size());
    }

    @Test
    void findAreasByLocation_Forbidden_NotOwner() {
        assertThrows(
                SecurityException.class,
                () -> areaService.findAreasByLocation(otherLocation.id, managerAuth));
    }

    @Test
    void findAreasByLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> areaService.findAreasByLocation(id(999), managerAuth));
    }

    @Test
    void findAreaByIdForManager_Success() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));

        AreaResponseDTO result = areaService.findAreaByIdForManager(existingArea.id, managerAuth);

        assertEquals("Parkett", result.name());
    }

    @Test
    void findAreaByIdForManager_NotFound() {
        when(areaRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                AreaNotFoundException.class,
                () -> areaService.findAreaByIdForManager(id(999), managerAuth));
    }

    @Test
    void findAreaByIdForManager_Forbidden() {
        EventLocationArea areaInOtherLocation = new EventLocationArea("X");
        areaInOtherLocation.id = id(20);
        areaInOtherLocation.setEventLocation(otherLocation);
        when(areaRepository.findByIdWithEventLocation(areaInOtherLocation.id))
                .thenReturn(Optional.of(areaInOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> areaService.findAreaByIdForManager(areaInOtherLocation.id, managerAuth));
    }

    @Test
    void updateArea_Success() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        AreaRequestDTO dto =
                new AreaRequestDTO(eventLocation.id, "Loge", List.of(new CoordinateDTO(2, 2)));

        AreaResponseDTO result = areaService.updateArea(existingArea.id, dto, managerAuth);

        assertEquals("Loge", result.name());
        verify(areaRepository, times(1)).persist(existingArea);
    }

    @Test
    void updateArea_NotFound() {
        when(areaRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "Loge", List.of());

        assertThrows(
                AreaNotFoundException.class,
                () -> areaService.updateArea(id(999), dto, managerAuth));
    }

    @Test
    void updateArea_Forbidden_NotManagerOfNewLocation() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        AreaRequestDTO dto = new AreaRequestDTO(otherLocation.id, "Loge", List.of());

        assertThrows(
                SecurityException.class,
                () -> areaService.updateArea(existingArea.id, dto, managerAuth));
    }

    @Test
    void updateArea_InvalidInput_EmptyName() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "  ", List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> areaService.updateArea(existingArea.id, dto, managerAuth));
        verify(areaRepository, never()).persist(any(EventLocationArea.class));
    }

    @Test
    void updateArea_Conflict_MoveToOtherLocationWhileReferencedBySeat() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(1L);

        AreaRequestDTO dto = new AreaRequestDTO();
        dto.setEventLocationId(secondOwnedLocation.id);
        dto.setName("Parkett");
        dto.setBoundary(List.of());

        assertThrows(
                AreaInUseException.class,
                () -> areaService.updateArea(existingArea.id, dto, managerAuth));
        verify(areaRepository, never()).persist(any(EventLocationArea.class));
        assertEquals(eventLocation.id, existingArea.getEventLocation().id);
    }

    @Test
    void updateArea_Success_MoveToOtherLocationWhenNotReferenced() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(0L);

        AreaRequestDTO dto = new AreaRequestDTO();
        dto.setEventLocationId(secondOwnedLocation.id);
        dto.setName("Parkett");
        dto.setBoundary(List.of());

        AreaResponseDTO result = areaService.updateArea(existingArea.id, dto, managerAuth);

        assertEquals(secondOwnedLocation.id, result.eventLocationId());
        verify(areaRepository, times(1)).persist(existingArea);
    }

    @Test
    void updateArea_Success_SameLocationWhileReferencedBySeat() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(5L);

        AreaRequestDTO dto = new AreaRequestDTO();
        dto.setEventLocationId(eventLocation.id);
        dto.setName("Parkett umbenannt");
        dto.setBoundary(List.of());

        AreaResponseDTO result = areaService.updateArea(existingArea.id, dto, managerAuth);

        assertEquals("Parkett umbenannt", result.name());
        verify(areaRepository, times(1)).persist(existingArea);
    }

    @Test
    void deleteAreas_InvalidInput_EmptyIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> areaService.deleteAreas(List.of(), managerAuth));
        verify(areaRepository, never()).delete(any(EventLocationArea.class));
    }

    @Test
    void deleteAreas_Success() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(0L);

        areaService.deleteAreas(List.of(existingArea.id), managerAuth);

        verify(areaRepository, times(1)).delete(existingArea);
    }

    @Test
    void deleteAreas_NotFound() {
        when(areaRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                AreaNotFoundException.class,
                () -> areaService.deleteAreas(List.of(id(999)), managerAuth));
    }

    @Test
    void deleteAreas_Forbidden() {
        EventLocationArea areaInOtherLocation = new EventLocationArea("X");
        areaInOtherLocation.id = id(20);
        areaInOtherLocation.setEventLocation(otherLocation);
        when(areaRepository.findByIdWithEventLocation(areaInOtherLocation.id))
                .thenReturn(Optional.of(areaInOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> areaService.deleteAreas(List.of(areaInOtherLocation.id), managerAuth));
    }

    @Test
    void deleteAreas_Conflict_ReferencedBySeat() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(1L);

        assertThrows(
                AreaInUseException.class,
                () -> areaService.deleteAreas(List.of(existingArea.id), managerAuth));
        verify(areaRepository, never()).delete(any(EventLocationArea.class));
    }
}
