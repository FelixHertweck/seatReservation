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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationResponseDTO;
import de.felixhertweck.seatreservation.utils.CodeGenerator;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventLocationServiceTest {

    @Inject EventLocationService eventLocationService;

    @InjectMock UserRepository userRepository;

    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    @InjectMock ReservationRepository reservationRepository;

    @InjectMock EventLocationRepository eventLocationRepository;

    private User user;
    private EventLocation locationA;
    private EventLocation locationB;
    private Event eventA;
    private Event eventB;

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

        eventA = new Event();
        eventA.id = id(100);
        eventA.setName("Event A");
        eventA.setEventLocation(locationA);

        eventB = new Event();
        eventB.id = id(200);
        eventB.setName("Event B");
        eventB.setEventLocation(locationB);
    }

    @Test
    void getLocationsForCurrentUser_Success_FromAllowanceAndReservation() {
        // Allowance provides Location A
        var allowance = new EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(eventA);
        allowance.setReservationsAllowedCount(3);

        // Reservation provides Location B
        var seat = new Seat("S1", "Row 1", locationB);
        var reservation =
                new Reservation(
                        user,
                        eventB,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowance));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(reservation));

        io.quarkus.hibernate.orm.panache.PanacheQuery query =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventLocationRepository.find(anyString(), any(java.util.Set.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(locationA));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_Deduplicates_Locations() {
        // Same location from allowance and reservation should appear once
        var allowance = new EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(eventA);
        allowance.setReservationsAllowedCount(3);

        var seat = new Seat("S1", "Row 1", locationA);
        var reservation =
                new Reservation(
                        user,
                        eventA,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowance));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(reservation));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertEquals(locationA.id, result.getFirst().id());
    }

    @Test
    void getLocationsForCurrentUser_Empty() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList());

        List<UserEventLocationResponseDTO> result =
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
        // Allowance provides Location A
        var allowance = new EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(eventA);
        allowance.setReservationsAllowedCount(3);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowance));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList()); // No reservations

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
    }

    @Test
    void getLocationsForCurrentUser_Success_OnlyFromReservation() {
        // Reservation provides Location B
        var seat = new Seat("S1", "Row 1", locationB);
        var reservation =
                new Reservation(
                        user,
                        eventB,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList()); // No allowances
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(reservation));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_NoAllowanceNoReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList());

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLocationsForCurrentUser_OneLocationWithAllowance_OneLocationWithReservation() {
        // Allowance provides Location A
        var allowanceA = new EventUserAllowance();
        allowanceA.setUser(user);
        allowanceA.setEvent(eventA);
        allowanceA.setReservationsAllowedCount(3);

        // Reservation provides Location B
        var seatB = new Seat("S1", "Row 1", locationB);
        var reservationB =
                new Reservation(
                        user,
                        eventB,
                        seatB,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowanceA));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(reservationB));

        io.quarkus.hibernate.orm.panache.PanacheQuery query =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventLocationRepository.find(anyString(), any(java.util.Set.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(locationA));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_TwoDifferentLocations_OneAllowanceOneReservation() {
        // Event A for Location A with Allowance
        var allowanceA = new EventUserAllowance();
        allowanceA.setUser(user);
        allowanceA.setEvent(eventA);
        allowanceA.setReservationsAllowedCount(3);

        // Event B for Location B with Reservation
        var seatB = new Seat("S1", "Row 1", locationB);
        var reservationB =
                new Reservation(
                        user,
                        eventB,
                        seatB,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowanceA));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(reservationB));

        io.quarkus.hibernate.orm.panache.PanacheQuery query =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventLocationRepository.find(anyString(), any(java.util.Set.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(locationA));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_OneLocationTwoEvents_OneAllowanceOneReservation() {
        // Create a new eventC for locationA
        Event eventC = new Event();
        eventC.id = id(300);
        eventC.setName("Event C");
        eventC.setEventLocation(locationA);

        // Allowance for eventA (Location A)
        var allowanceA = new EventUserAllowance();
        allowanceA.setUser(user);
        allowanceA.setEvent(eventA);
        allowanceA.setReservationsAllowedCount(3);

        // Reservation for eventC (Location A)
        var seatC = new Seat("S2", "Row 2", locationA);
        var reservationC =
                new Reservation(
                        user,
                        eventC,
                        seatC,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowanceA));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(reservationC));

        io.quarkus.hibernate.orm.panache.PanacheQuery query =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventLocationRepository.find(anyString(), any(java.util.Set.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(locationB));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
    }

    @Test
    void getLocationsForCurrentUser_GroupsSeatsIntoAreas() {
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
        // Areas hang off the location in JPA; the response is derived from them, not from the
        // seats.
        parkett.setEventLocation(locationA);
        locationA.setAreas(new ArrayList<>(List.of(parkett)));

        var allowance = new EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(eventA);
        allowance.setReservationsAllowedCount(3);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUserWithEventAndLocation(user))
                .thenReturn(List.of(allowance));
        when(reservationRepository.findByUserWithEventAndLocation(user))
                .thenReturn(Collections.emptyList());

        io.quarkus.hibernate.orm.panache.PanacheQuery query =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventLocationRepository.find(anyString(), any(java.util.Set.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(locationA, locationB));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        UserEventLocationResponseDTO locationDto = result.getFirst();
        assertEquals(1, locationDto.areas().size());
        assertEquals("Parkett", locationDto.areas().getFirst().name());
        assertEquals(2, locationDto.areas().getFirst().seatIds().size());
    }
}
