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

import de.felixhertweck.seatreservation.management.dto.EntranceRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceResponseDTO;
import de.felixhertweck.seatreservation.management.exception.EntranceInUseException;
import de.felixhertweck.seatreservation.management.exception.EntranceNotFoundException;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class EntranceServiceTest {

    @InjectMock EventLocationEntranceRepository entranceRepository;
    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock SeatRepository seatRepository;

    @Inject EntranceService entranceService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser regularAuth;
    private EventLocation eventLocation;
    private EventLocation otherLocation;
    private EventLocation secondOwnedLocation;
    private EventLocationEntrance existingEntrance;

    @BeforeEach
    void setUp() {
        Mockito.reset(entranceRepository, eventLocationRepository, seatRepository);

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

        existingEntrance = new EventLocationEntrance("A");
        existingEntrance.id = id(10);
        existingEntrance.setEventLocation(eventLocation);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(eventLocationRepository.findByIdOptional(otherLocation.id))
                .thenReturn(Optional.of(otherLocation));
        when(eventLocationRepository.findByIdOptional(secondOwnedLocation.id))
                .thenReturn(Optional.of(secondOwnedLocation));
    }

    @Test
    void createEntrance_Success_AsManager() {
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "B");

        EntranceResponseDTO result = entranceService.createEntrance(dto, managerAuth);

        assertNotNull(result);
        assertEquals("B", result.name());
        assertEquals(eventLocation.id, result.eventLocationId());
        verify(entranceRepository, times(1)).persist(any(EventLocationEntrance.class));
    }

    @Test
    void createEntrance_Success_AsAdmin() {
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "B");

        EntranceResponseDTO result = entranceService.createEntrance(dto, adminAuth);

        assertNotNull(result);
        verify(entranceRepository, times(1)).persist(any(EventLocationEntrance.class));
    }

    @Test
    void createEntrance_Forbidden_NotManagerOfLocation() {
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "B");

        assertThrows(
                SecurityException.class, () -> entranceService.createEntrance(dto, regularAuth));
        verify(entranceRepository, never()).persist(any(EventLocationEntrance.class));
    }

    @Test
    void createEntrance_InvalidInput_EmptyName() {
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "  ");

        assertThrows(
                IllegalArgumentException.class,
                () -> entranceService.createEntrance(dto, managerAuth));
        verify(entranceRepository, never()).persist(any(EventLocationEntrance.class));
    }

    @Test
    void createEntrance_EventLocationNotFound() {
        when(eventLocationRepository.findByIdOptional(id(999))).thenReturn(Optional.empty());
        EntranceRequestDTO dto = new EntranceRequestDTO(id(999), "B");

        assertThrows(
                EventLocationNotFoundException.class,
                () -> entranceService.createEntrance(dto, managerAuth));
    }

    @Test
    void findEntrancesByLocation_Success_AsManager() {
        when(entranceRepository.findByEventLocation(eventLocation))
                .thenReturn(List.of(existingEntrance));

        List<EntranceResponseDTO> result =
                entranceService.findEntrancesByLocation(eventLocation.id, managerAuth);

        assertEquals(1, result.size());
        assertEquals("A", result.getFirst().name());
    }

    @Test
    void findEntrancesByLocation_Forbidden_NotOwner() {
        assertThrows(
                SecurityException.class,
                () -> entranceService.findEntrancesByLocation(otherLocation.id, managerAuth));
    }

    @Test
    void findEntrancesByLocation_NotFound() {
        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EventLocationNotFoundException.class,
                () -> entranceService.findEntrancesByLocation(id(999), managerAuth));
    }

    @Test
    void findEntranceByIdForManager_Success() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));

        EntranceResponseDTO result =
                entranceService.findEntranceByIdForManager(existingEntrance.id, managerAuth);

        assertEquals("A", result.name());
    }

    @Test
    void findEntranceByIdForManager_NotFound() {
        when(entranceRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EntranceNotFoundException.class,
                () -> entranceService.findEntranceByIdForManager(id(999), managerAuth));
    }

    @Test
    void findEntranceByIdForManager_Forbidden() {
        EventLocationEntrance entranceInOtherLocation = new EventLocationEntrance("X");
        entranceInOtherLocation.id = id(20);
        entranceInOtherLocation.setEventLocation(otherLocation);
        when(entranceRepository.findByIdWithEventLocation(entranceInOtherLocation.id))
                .thenReturn(Optional.of(entranceInOtherLocation));

        assertThrows(
                SecurityException.class,
                () ->
                        entranceService.findEntranceByIdForManager(
                                entranceInOtherLocation.id, managerAuth));
    }

    @Test
    void updateEntrance_Success() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "C");

        EntranceResponseDTO result =
                entranceService.updateEntrance(existingEntrance.id, dto, managerAuth);

        assertEquals("C", result.name());
        verify(entranceRepository, times(1)).persist(existingEntrance);
    }

    @Test
    void updateEntrance_NotFound() {
        when(entranceRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "C");

        assertThrows(
                EntranceNotFoundException.class,
                () -> entranceService.updateEntrance(id(999), dto, managerAuth));
    }

    @Test
    void updateEntrance_Forbidden_NotManagerOfNewLocation() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        EntranceRequestDTO dto = new EntranceRequestDTO(otherLocation.id, "C");

        assertThrows(
                SecurityException.class,
                () -> entranceService.updateEntrance(existingEntrance.id, dto, managerAuth));
    }

    @Test
    void updateEntrance_InvalidInput_EmptyName() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        EntranceRequestDTO dto = new EntranceRequestDTO(eventLocation.id, "  ");

        assertThrows(
                IllegalArgumentException.class,
                () -> entranceService.updateEntrance(existingEntrance.id, dto, managerAuth));
        verify(entranceRepository, never()).persist(any(EventLocationEntrance.class));
    }

    @Test
    void updateEntrance_Conflict_MoveToOtherLocationWhileReferencedBySeat() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        when(seatRepository.countByEntrance(existingEntrance)).thenReturn(1L);

        EntranceRequestDTO dto = new EntranceRequestDTO();
        dto.setEventLocationId(secondOwnedLocation.id);
        dto.setName("A");

        assertThrows(
                EntranceInUseException.class,
                () -> entranceService.updateEntrance(existingEntrance.id, dto, managerAuth));
        verify(entranceRepository, never()).persist(any(EventLocationEntrance.class));
        assertEquals(eventLocation.id, existingEntrance.getEventLocation().id);
    }

    @Test
    void updateEntrance_Success_MoveToOtherLocationWhenNotReferenced() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        when(seatRepository.countByEntrance(existingEntrance)).thenReturn(0L);

        EntranceRequestDTO dto = new EntranceRequestDTO();
        dto.setEventLocationId(secondOwnedLocation.id);
        dto.setName("A");

        EntranceResponseDTO result =
                entranceService.updateEntrance(existingEntrance.id, dto, managerAuth);

        assertEquals(secondOwnedLocation.id, result.eventLocationId());
        verify(entranceRepository, times(1)).persist(existingEntrance);
    }

    @Test
    void updateEntrance_Success_SameLocationWhileReferencedBySeat() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        when(seatRepository.countByEntrance(existingEntrance)).thenReturn(5L);

        EntranceRequestDTO dto = new EntranceRequestDTO();
        dto.setEventLocationId(eventLocation.id);
        dto.setName("A umbenannt");

        EntranceResponseDTO result =
                entranceService.updateEntrance(existingEntrance.id, dto, managerAuth);

        assertEquals("A umbenannt", result.name());
        verify(entranceRepository, times(1)).persist(existingEntrance);
    }

    @Test
    void deleteEntrances_InvalidInput_EmptyIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> entranceService.deleteEntrances(List.of(), managerAuth));
        verify(entranceRepository, never()).delete(any(EventLocationEntrance.class));
    }

    @Test
    void deleteEntrances_Success() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        when(seatRepository.countByEntrance(existingEntrance)).thenReturn(0L);

        entranceService.deleteEntrances(List.of(existingEntrance.id), managerAuth);

        verify(entranceRepository, times(1)).delete(existingEntrance);
    }

    @Test
    void deleteEntrances_NotFound() {
        when(entranceRepository.findByIdWithEventLocation(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EntranceNotFoundException.class,
                () -> entranceService.deleteEntrances(List.of(id(999)), managerAuth));
    }

    @Test
    void deleteEntrances_Conflict_ReferencedBySeat() {
        when(entranceRepository.findByIdWithEventLocation(existingEntrance.id))
                .thenReturn(Optional.of(existingEntrance));
        when(seatRepository.countByEntrance(existingEntrance)).thenReturn(1L);

        assertThrows(
                EntranceInUseException.class,
                () -> entranceService.deleteEntrances(List.of(existingEntrance.id), managerAuth));
        verify(entranceRepository, never()).delete(any(EventLocationEntrance.class));
    }
}
