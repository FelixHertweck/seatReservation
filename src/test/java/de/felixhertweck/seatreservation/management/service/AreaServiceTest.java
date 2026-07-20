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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        otherLocation = new EventLocation("Other Hall", "Other Address", regularUser, 50);
        otherLocation.id = 2L;

        secondOwnedLocation = new EventLocation("Zweite Halle", "Nebenstraße 2", managerUser, 80);
        secondOwnedLocation.id = 3L;

        existingArea = new EventLocationArea("Parkett");
        existingArea.id = 10L;
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

        AreaResponseDTO result = areaService.createArea(dto, managerUser);

        assertNotNull(result);
        assertEquals("Balkon", result.name());
        assertEquals(eventLocation.id, result.eventLocationId());
        verify(areaRepository, times(1)).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_Success_AsAdmin() {
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "Balkon", List.of());

        AreaResponseDTO result = areaService.createArea(dto, adminUser);

        assertNotNull(result);
        verify(areaRepository, times(1)).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_Forbidden_NotManagerOfLocation() {
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "Balkon", List.of());

        assertThrows(SecurityException.class, () -> areaService.createArea(dto, regularUser));
        verify(areaRepository, never()).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_InvalidInput_EmptyName() {
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "  ", List.of());

        assertThrows(
                IllegalArgumentException.class, () -> areaService.createArea(dto, managerUser));
        verify(areaRepository, never()).persist(any(EventLocationArea.class));
    }

    @Test
    void createArea_EventLocationNotFound() {
        when(eventLocationRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        AreaRequestDTO dto = new AreaRequestDTO(999L, "Balkon", List.of());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> areaService.createArea(dto, managerUser));
    }

    @Test
    void findAreasByLocation_Success_AsManager() {
        when(areaRepository.findByEventLocation(eventLocation)).thenReturn(List.of(existingArea));

        List<AreaResponseDTO> result =
                areaService.findAreasByLocation(eventLocation.id, managerUser);

        assertEquals(1, result.size());
        assertEquals("Parkett", result.getFirst().name());
    }

    @Test
    void findAreasByLocation_Success_AsAdmin() {
        when(areaRepository.findByEventLocation(eventLocation)).thenReturn(List.of(existingArea));

        List<AreaResponseDTO> result = areaService.findAreasByLocation(eventLocation.id, adminUser);

        assertEquals(1, result.size());
    }

    @Test
    void findAreasByLocation_Forbidden_NotOwner() {
        assertThrows(
                SecurityException.class,
                () -> areaService.findAreasByLocation(otherLocation.id, managerUser));
    }

    @Test
    void findAreasByLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> areaService.findAreasByLocation(999L, managerUser));
    }

    @Test
    void findAreaByIdForManager_Success() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));

        AreaResponseDTO result = areaService.findAreaByIdForManager(existingArea.id, managerUser);

        assertEquals("Parkett", result.name());
    }

    @Test
    void findAreaByIdForManager_NotFound() {
        when(areaRepository.findByIdWithEventLocation(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                AreaNotFoundException.class,
                () -> areaService.findAreaByIdForManager(999L, managerUser));
    }

    @Test
    void findAreaByIdForManager_Forbidden() {
        EventLocationArea areaInOtherLocation = new EventLocationArea("X");
        areaInOtherLocation.id = 20L;
        areaInOtherLocation.setEventLocation(otherLocation);
        when(areaRepository.findByIdWithEventLocation(areaInOtherLocation.id))
                .thenReturn(Optional.of(areaInOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> areaService.findAreaByIdForManager(areaInOtherLocation.id, managerUser));
    }

    @Test
    void updateArea_Success() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        AreaRequestDTO dto =
                new AreaRequestDTO(eventLocation.id, "Loge", List.of(new CoordinateDTO(2, 2)));

        AreaResponseDTO result = areaService.updateArea(existingArea.id, dto, managerUser);

        assertEquals("Loge", result.name());
        verify(areaRepository, times(1)).persist(existingArea);
    }

    @Test
    void updateArea_NotFound() {
        when(areaRepository.findByIdWithEventLocation(anyLong())).thenReturn(Optional.empty());
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "Loge", List.of());

        assertThrows(
                AreaNotFoundException.class, () -> areaService.updateArea(999L, dto, managerUser));
    }

    @Test
    void updateArea_Forbidden_NotManagerOfNewLocation() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        AreaRequestDTO dto = new AreaRequestDTO(otherLocation.id, "Loge", List.of());

        assertThrows(
                SecurityException.class,
                () -> areaService.updateArea(existingArea.id, dto, managerUser));
    }

    @Test
    void updateArea_InvalidInput_EmptyName() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        AreaRequestDTO dto = new AreaRequestDTO(eventLocation.id, "  ", List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> areaService.updateArea(existingArea.id, dto, managerUser));
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
                () -> areaService.updateArea(existingArea.id, dto, managerUser));
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

        AreaResponseDTO result = areaService.updateArea(existingArea.id, dto, managerUser);

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

        AreaResponseDTO result = areaService.updateArea(existingArea.id, dto, managerUser);

        assertEquals("Parkett umbenannt", result.name());
        verify(areaRepository, times(1)).persist(existingArea);
    }

    @Test
    void deleteAreas_InvalidInput_EmptyIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> areaService.deleteAreas(List.of(), managerUser));
        verify(areaRepository, never()).delete(any(EventLocationArea.class));
    }

    @Test
    void deleteAreas_Success() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(0L);

        areaService.deleteAreas(List.of(existingArea.id), managerUser);

        verify(areaRepository, times(1)).delete(existingArea);
    }

    @Test
    void deleteAreas_NotFound() {
        when(areaRepository.findByIdWithEventLocation(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                AreaNotFoundException.class,
                () -> areaService.deleteAreas(List.of(999L), managerUser));
    }

    @Test
    void deleteAreas_Forbidden() {
        EventLocationArea areaInOtherLocation = new EventLocationArea("X");
        areaInOtherLocation.id = 20L;
        areaInOtherLocation.setEventLocation(otherLocation);
        when(areaRepository.findByIdWithEventLocation(areaInOtherLocation.id))
                .thenReturn(Optional.of(areaInOtherLocation));

        assertThrows(
                SecurityException.class,
                () -> areaService.deleteAreas(List.of(areaInOtherLocation.id), managerUser));
    }

    @Test
    void deleteAreas_Conflict_ReferencedBySeat() {
        when(areaRepository.findByIdWithEventLocation(existingArea.id))
                .thenReturn(Optional.of(existingArea));
        when(seatRepository.countByArea(existingArea)).thenReturn(1L);

        assertThrows(
                AreaInUseException.class,
                () -> areaService.deleteAreas(List.of(existingArea.id), managerUser));
        verify(areaRepository, never()).delete(any(EventLocationArea.class));
    }
}
