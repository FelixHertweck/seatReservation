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

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
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

    private User user;
    private EventLocation locationA;
    private EventLocation locationB;
    private Event eventA;
    private Event eventB;

    @BeforeEach
    void setUp() {
        user = new User();
        user.id = 1L;
        user.setUsername("testuser");

        locationA = new EventLocation();
        locationA.id = 10L;
        locationA.setName("Location A");

        locationB = new EventLocation();
        locationB.id = 20L;
        locationB.setName("Location B");

        eventA = new Event();
        eventA.id = 100L;
        eventA.setName("Event A");
        eventA.setEventLocation(locationA);

        eventB = new Event();
        eventB.id = 200L;
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
        var reservations = new HashSet<Reservation>();
        reservations.add(reservation);
        user.setReservations(reservations);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance));

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
        var reservations = new HashSet<Reservation>();
        reservations.add(reservation);
        user.setReservations(reservations);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertEquals(locationA.id, result.getFirst().id());
    }

    @Test
    void getLocationsForCurrentUser_Empty() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(Collections.emptyList());
        user.setReservations(Collections.emptySet());

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
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance));
        user.setReservations(Collections.emptySet()); // No reservations

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
        var reservations = new HashSet<Reservation>();
        reservations.add(reservation);
        user.setReservations(reservations);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user))
                .thenReturn(Collections.emptyList()); // No allowances

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationB.id)));
    }

    @Test
    void getLocationsForCurrentUser_NoAllowanceNoReservation() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(Collections.emptyList());
        user.setReservations(Collections.emptySet());

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
        var reservations = new HashSet<Reservation>();
        reservations.add(reservationB);
        user.setReservations(reservations);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowanceA));

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
        var reservations = new HashSet<Reservation>();
        reservations.add(reservationB);
        user.setReservations(reservations);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowanceA));

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
        eventC.id = 300L;
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
        var reservations = new HashSet<Reservation>();
        reservations.add(reservationC);
        user.setReservations(reservations);

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowanceA));

        List<UserEventLocationResponseDTO> result =
                eventLocationService.getLocationsForCurrentUser("testuser");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(locationA.id)));
    }
}
