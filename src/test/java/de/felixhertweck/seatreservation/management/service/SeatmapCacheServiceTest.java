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
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.AreaRequestDTO;
import de.felixhertweck.seatreservation.management.dto.AreaResponseDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceResponseDTO;
import de.felixhertweck.seatreservation.management.dto.MakerRequestDTO;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SeatmapCacheServiceTest {

    @InjectMock SeatRepository seatRepository;
    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock EventLocationAreaRepository areaRepository;
    @InjectMock EventLocationMarkerRepository markerRepository;
    @InjectMock EventLocationEntranceRepository entranceRepository;

    @Inject SeatService seatService;
    @Inject AreaService areaService;
    @Inject MarkerService markerService;
    @Inject EntranceService entranceService;
    @Inject SeatmapCacheService seatmapCacheService;

    private EventLocation location;
    private EventLocation otherLocation;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser unauthorizedUserAuth;
    private UUID locationId;
    private UUID otherLocationId;

    @BeforeEach
    void setUp() {
        reset(
                seatRepository,
                eventLocationRepository,
                areaRepository,
                markerRepository,
                entranceRepository);

        locationId = UUID.randomUUID();
        location = new EventLocation("Main Hall", "Main St", null);
        location.id = locationId;

        otherLocationId = UUID.randomUUID();
        otherLocation = new EventLocation("Side Hall", "Side St", null);
        otherLocation.id = otherLocationId;

        UUID managerId = UUID.randomUUID();
        managerAuth = new AuthenticatedUser(managerId, Set.of(Roles.MANAGER));

        UUID unauthorizedId = UUID.randomUUID();
        unauthorizedUserAuth = new AuthenticatedUser(unauthorizedId, Set.of(Roles.MANAGER));

        when(eventLocationRepository.findByIdOptional(locationId))
                .thenReturn(Optional.of(location));
        when(eventLocationRepository.findById(locationId)).thenReturn(location);
        when(eventLocationRepository.isUserManager(locationId, managerId)).thenReturn(true);
        when(eventLocationRepository.isUserManager(locationId, unauthorizedId)).thenReturn(false);

        when(eventLocationRepository.findByIdOptional(otherLocationId))
                .thenReturn(Optional.of(otherLocation));
        when(eventLocationRepository.isUserManager(otherLocationId, managerId)).thenReturn(true);
    }

    @Test
    void testCacheHit_SecondCallDoesNotQueryRepository() {
        Seat seat = new Seat("A1", "", location);
        seat.id = UUID.randomUUID();
        when(seatRepository.findByEventLocation(location)).thenReturn(List.of(seat));

        // Call 1
        List<SeatDTO> result1 = seatService.findSeatsForManagerByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());

        // Call 2 - should be served from cache
        List<SeatDTO> result2 = seatService.findSeatsForManagerByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());

        // Repository findByEventLocation should only have been called once
        verify(seatRepository, times(1)).findByEventLocation(location);
    }

    @Test
    void testCacheInvalidation_InvalidateForcesNewRepositoryQuery() {
        Seat seat1 = new Seat("A1", "", location);
        seat1.id = UUID.randomUUID();
        when(seatRepository.findByEventLocation(location)).thenReturn(List.of(seat1));

        // Populate cache
        List<SeatDTO> result1 = seatService.findSeatsForManagerByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());
        verify(seatRepository, times(1)).findByEventLocation(location);

        // Invalidate cache for location
        seatmapCacheService.invalidateSeats(locationId);

        // Subsequent read should hit DB again
        List<SeatDTO> result2 = seatService.findSeatsForManagerByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());
        verify(seatRepository, times(2)).findByEventLocation(location);
    }

    @Test
    void testSecurityIsolation_UnauthorizedUserBlockedBeforeCache() {
        Seat seat = new Seat("A1", "", location);
        seat.id = UUID.randomUUID();
        when(seatRepository.findByEventLocation(location)).thenReturn(List.of(seat));

        // Authorized manager reads and populates cache
        List<SeatDTO> result = seatService.findSeatsForManagerByLocation(locationId, managerAuth);
        assertNotNull(result);

        // Unauthorized user attempts to read same locationId
        assertThrows(
                SecurityException.class,
                () -> seatService.findSeatsForManagerByLocation(locationId, unauthorizedUserAuth));
    }

    @Test
    void testAreasCacheHit_SecondCallDoesNotQueryRepository() {
        EventLocationArea area = new EventLocationArea("Block A");
        area.id = UUID.randomUUID();
        area.setEventLocation(location);
        when(areaRepository.findByEventLocation(location)).thenReturn(List.of(area));

        List<AreaResponseDTO> result1 = areaService.findAreasByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());

        List<AreaResponseDTO> result2 = areaService.findAreasByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());

        verify(areaRepository, times(1)).findByEventLocation(location);
    }

    @Test
    void testAreasCacheInvalidation_InvalidateForcesNewRepositoryQuery() {
        EventLocationArea area = new EventLocationArea("Block A");
        area.id = UUID.randomUUID();
        area.setEventLocation(location);
        when(areaRepository.findByEventLocation(location)).thenReturn(List.of(area));

        List<AreaResponseDTO> result1 = areaService.findAreasByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());
        verify(areaRepository, times(1)).findByEventLocation(location);

        seatmapCacheService.invalidateAreas(locationId);

        List<AreaResponseDTO> result2 = areaService.findAreasByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());
        verify(areaRepository, times(2)).findByEventLocation(location);
    }

    @Test
    void testMarkersCacheHit_SecondCallDoesNotQueryRepository() {
        EventLocationMarker marker = new EventLocationMarker("Exit", 1, 2);
        marker.id = UUID.randomUUID();
        marker.setEventLocation(location);
        when(markerRepository.findByEventLocation(location)).thenReturn(List.of(marker));

        List<EventLocationMakerDTO> result1 =
                markerService.findMarkersByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());

        List<EventLocationMakerDTO> result2 =
                markerService.findMarkersByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());

        verify(markerRepository, times(1)).findByEventLocation(location);
    }

    @Test
    void testMarkersCacheInvalidation_InvalidateForcesNewRepositoryQuery() {
        EventLocationMarker marker = new EventLocationMarker("Exit", 1, 2);
        marker.id = UUID.randomUUID();
        marker.setEventLocation(location);
        when(markerRepository.findByEventLocation(location)).thenReturn(List.of(marker));

        List<EventLocationMakerDTO> result1 =
                markerService.findMarkersByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());
        verify(markerRepository, times(1)).findByEventLocation(location);

        seatmapCacheService.invalidateMarkers(locationId);

        List<EventLocationMakerDTO> result2 =
                markerService.findMarkersByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());
        verify(markerRepository, times(2)).findByEventLocation(location);
    }

    @Test
    void testEntrancesCacheHit_SecondCallDoesNotQueryRepository() {
        EventLocationEntrance entrance = new EventLocationEntrance("Main Entrance");
        entrance.id = UUID.randomUUID();
        entrance.setEventLocation(location);
        when(entranceRepository.findByEventLocation(location)).thenReturn(List.of(entrance));

        List<EntranceResponseDTO> result1 =
                entranceService.findEntrancesByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());

        List<EntranceResponseDTO> result2 =
                entranceService.findEntrancesByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());

        verify(entranceRepository, times(1)).findByEventLocation(location);
    }

    @Test
    void testEntrancesCacheInvalidation_InvalidateForcesNewRepositoryQuery() {
        EventLocationEntrance entrance = new EventLocationEntrance("Main Entrance");
        entrance.id = UUID.randomUUID();
        entrance.setEventLocation(location);
        when(entranceRepository.findByEventLocation(location)).thenReturn(List.of(entrance));

        List<EntranceResponseDTO> result1 =
                entranceService.findEntrancesByLocation(locationId, managerAuth);
        assertEquals(1, result1.size());
        verify(entranceRepository, times(1)).findByEventLocation(location);

        seatmapCacheService.invalidateEntrances(locationId);

        List<EntranceResponseDTO> result2 =
                entranceService.findEntrancesByLocation(locationId, managerAuth);
        assertEquals(1, result2.size());
        verify(entranceRepository, times(2)).findByEventLocation(location);
    }

    @Test
    void testAreaMoveAcrossLocations_InvalidatesOldArea_NewArea_AndSeatsCache() {
        EventLocationArea area = new EventLocationArea("Block A");
        area.id = UUID.randomUUID();
        area.setEventLocation(location);

        Seat seat = new Seat("A1", "", location);
        seat.id = UUID.randomUUID();

        when(areaRepository.findByEventLocation(location)).thenReturn(List.of(area));
        when(areaRepository.findByEventLocation(otherLocation)).thenReturn(List.of());
        when(seatRepository.findByEventLocation(location)).thenReturn(List.of(seat));
        when(areaRepository.findByIdWithEventLocation(area.id)).thenReturn(Optional.of(area));
        when(seatRepository.countByArea(area)).thenReturn(0L);

        // Populate the area caches for both locations and the seats cache for the old location.
        assertEquals(1, areaService.findAreasByLocation(locationId, managerAuth).size());
        assertEquals(0, areaService.findAreasByLocation(otherLocationId, managerAuth).size());
        assertEquals(1, seatService.findSeatsForManagerByLocation(locationId, managerAuth).size());
        verify(areaRepository, times(1)).findByEventLocation(location);
        verify(areaRepository, times(1)).findByEventLocation(otherLocation);
        verify(seatRepository, times(1)).findByEventLocation(location);

        AreaRequestDTO dto = new AreaRequestDTO();
        dto.setEventLocationId(otherLocationId);
        dto.setName("Block A");
        dto.setBoundary(List.of());
        areaService.updateArea(area.id, dto, managerAuth);

        // Old location's areas, old location's seats (denormalized area name) and the new
        // location's areas must all be invalidated.
        areaService.findAreasByLocation(locationId, managerAuth);
        areaService.findAreasByLocation(otherLocationId, managerAuth);
        seatService.findSeatsForManagerByLocation(locationId, managerAuth);

        verify(areaRepository, times(2)).findByEventLocation(location);
        verify(areaRepository, times(2)).findByEventLocation(otherLocation);
        verify(seatRepository, times(2)).findByEventLocation(location);
    }

    @Test
    void testEntranceMoveAcrossLocations_InvalidatesOldEntrance_NewEntrance_AndSeatsCache() {
        EventLocationEntrance entrance = new EventLocationEntrance("Main Entrance");
        entrance.id = UUID.randomUUID();
        entrance.setEventLocation(location);

        Seat seat = new Seat("A1", "", location);
        seat.id = UUID.randomUUID();

        when(entranceRepository.findByEventLocation(location)).thenReturn(List.of(entrance));
        when(entranceRepository.findByEventLocation(otherLocation)).thenReturn(List.of());
        when(seatRepository.findByEventLocation(location)).thenReturn(List.of(seat));
        when(entranceRepository.findByIdWithEventLocation(entrance.id))
                .thenReturn(Optional.of(entrance));
        when(seatRepository.countByEntrance(entrance)).thenReturn(0L);

        // Populate the entrance caches for both locations and the seats cache for the old
        // location.
        assertEquals(1, entranceService.findEntrancesByLocation(locationId, managerAuth).size());
        assertEquals(
                0, entranceService.findEntrancesByLocation(otherLocationId, managerAuth).size());
        assertEquals(1, seatService.findSeatsForManagerByLocation(locationId, managerAuth).size());
        verify(entranceRepository, times(1)).findByEventLocation(location);
        verify(entranceRepository, times(1)).findByEventLocation(otherLocation);
        verify(seatRepository, times(1)).findByEventLocation(location);

        EntranceRequestDTO dto = new EntranceRequestDTO();
        dto.setEventLocationId(otherLocationId);
        dto.setName("Main Entrance");
        entranceService.updateEntrance(entrance.id, dto, managerAuth);

        // Old location's entrances, old location's seats (denormalized entrance name) and the new
        // location's entrances must all be invalidated.
        entranceService.findEntrancesByLocation(locationId, managerAuth);
        entranceService.findEntrancesByLocation(otherLocationId, managerAuth);
        seatService.findSeatsForManagerByLocation(locationId, managerAuth);

        verify(entranceRepository, times(2)).findByEventLocation(location);
        verify(entranceRepository, times(2)).findByEventLocation(otherLocation);
        verify(seatRepository, times(2)).findByEventLocation(location);
    }

    @Test
    void testMarkerMoveAcrossLocations_InvalidatesOldAndNewMarkerCache() {
        EventLocationMarker marker = new EventLocationMarker("Exit", 1, 2);
        marker.id = UUID.randomUUID();
        marker.setEventLocation(location);

        when(markerRepository.findByEventLocation(location)).thenReturn(List.of(marker));
        when(markerRepository.findByEventLocation(otherLocation)).thenReturn(List.of());
        when(markerRepository.findByIdWithEventLocation(marker.id)).thenReturn(Optional.of(marker));

        // Populate the marker caches for both locations.
        assertEquals(1, markerService.findMarkersByLocation(locationId, managerAuth).size());
        assertEquals(0, markerService.findMarkersByLocation(otherLocationId, managerAuth).size());
        verify(markerRepository, times(1)).findByEventLocation(location);
        verify(markerRepository, times(1)).findByEventLocation(otherLocation);

        MakerRequestDTO dto = new MakerRequestDTO(otherLocationId, "Exit", new CoordinateDTO(1, 2));
        markerService.updateMarker(marker.id, dto, managerAuth);

        // Both the old and the new location's marker caches must be invalidated.
        markerService.findMarkersByLocation(locationId, managerAuth);
        markerService.findMarkersByLocation(otherLocationId, managerAuth);

        verify(markerRepository, times(2)).findByEventLocation(location);
        verify(markerRepository, times(2)).findByEventLocation(otherLocation);
    }

    @Test
    void testSeatMoveAcrossLocations_InvalidatesOldAndNewSeatsCache() {
        Seat seat = new Seat("A1", "Row 1", location);
        seat.id = UUID.randomUUID();

        when(seatRepository.findByEventLocation(location)).thenReturn(List.of(seat));
        when(seatRepository.findByEventLocation(otherLocation)).thenReturn(List.of());
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));

        // Populate the seats cache for both locations.
        assertEquals(1, seatService.findSeatsForManagerByLocation(locationId, managerAuth).size());
        assertEquals(
                0, seatService.findSeatsForManagerByLocation(otherLocationId, managerAuth).size());
        verify(seatRepository, times(1)).findByEventLocation(location);
        verify(seatRepository, times(1)).findByEventLocation(otherLocation);

        SeatRequestDTO dto = new SeatRequestDTO("A1", "Row 1", otherLocationId, 1, 1, null, null);
        seatService.updateSeatForManager(seat.id, dto, managerAuth);

        // Both the old and the new location's seats caches must be invalidated.
        seatService.findSeatsForManagerByLocation(locationId, managerAuth);
        seatService.findSeatsForManagerByLocation(otherLocationId, managerAuth);

        verify(seatRepository, times(2)).findByEventLocation(location);
        verify(seatRepository, times(2)).findByEventLocation(otherLocation);
    }
}
