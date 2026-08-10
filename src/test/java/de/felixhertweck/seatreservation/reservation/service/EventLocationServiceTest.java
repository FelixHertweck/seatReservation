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
package de.felixhertweck.seatreservation.reservation.service;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationSummaryDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventLocationServiceTest {

    @Inject EventLocationService eventLocationService;

    @InjectMock UserRepository userRepository;

    @InjectMock EventLocationRepository eventLocationRepository;

    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    @InjectMock ReservationRepository reservationRepository;

    private User user;
    private EventLocation locationA;
    private EventLocation locationB;

    @BeforeEach
    void setUp() {
        user = new User();
        user.id = id(1);
        user.setUsername("testuser");

        locationA = new EventLocation();
        locationA.id = id(10);
        locationA.setName("Location A");

        locationB = new EventLocation();
        locationB.id = id(20);
        locationB.setName("Location B");
    }

    @Test
    void getLocationsForCurrentUser_Success_FromAllowanceAndReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationB));

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_Deduplicates_Locations() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertEquals(locationA.id, result.getFirst().id());
    }

    @Test
    void getLocationsForCurrentUser_Empty() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList());

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLocationsForCurrentUser_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);
        assertThrows(
                UserNotFoundException.class,
                () -> eventLocationService.getLocationsForCurrentUser("unknown"));
    }

    @Test
    void getLocationsForCurrentUser_Success_OnlyFromAllowance() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList()); // No reservations

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
    }

    @Test
    void getLocationsForCurrentUser_Success_OnlyFromReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList()); // No allowances
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationB));

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_NoAllowanceNoReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList());

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLocationsForCurrentUser_OneLocationWithAllowance_OneLocationWithReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationB));

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_TwoDifferentLocations_OneAllowanceOneReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationB));

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_OneLocationTwoEvents_OneAllowanceOneReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));

        List<UserEventLocationSummaryDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
    }

    @Test
    void getLocationByIdForCurrentUser_GroupsSeatsIntoAreas() {
        // Location A has two seats sharing an area, exposed via the allowance
        var parkett = new EventLocationArea("Parkett");
        parkett.id = id(1);
        var seat1 = new Seat("S1", "Row 1", locationA);
        seat1.id = id(1);
        seat1.setArea(parkett);
        var seat2 = new Seat("S2", "Row 1", locationA);
        seat2.id = id(2);
        seat2.setArea(parkett);
        locationA.setSeats(List.of(seat1, seat2));
        parkett.setEventLocation(locationA);
        locationA.setAreas(new ArrayList<>(List.of(parkett)));

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(List.of(locationA));
        when(reservationRepository.findDistinctEventLocationsByUser(user))
                .thenReturn(Collections.emptyList());
        when(eventLocationRepository.findByIdOptional(locationA.id))
                .thenReturn(Optional.of(locationA));

        UserEventLocationResponseDTO locationDto =
                eventLocationService.getLocationByIdForCurrentUser(locationA.id, "testuser");

        assertEquals(1, locationDto.areas().size());
        assertEquals("Parkett", locationDto.areas().getFirst().name());
        assertEquals(2, locationDto.areas().getFirst().seatIds().size());
    }
}
