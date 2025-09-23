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

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventResponseDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventServiceTest {

    @Inject EventService eventService;

    @InjectMock UserRepository userRepository;
    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;
    @InjectMock ReservationRepository reservationRepository;

    private User user;
    private Event event1;
    private Event event2;
    private EventUserAllowance allowance1;
    private Reservation reservation1;
    private Reservation reservation2;

    @BeforeEach
    void setUp() {
        user = new User();
        user.id = 1L;
        user.setUsername("testuser");

        var location = new EventLocation();
        location.id = 1L;

        event1 = new Event();
        event1.id = 1L;
        event1.setEventLocation(location);
        event1.setName("Event 1");

        event2 = new Event();
        event2.id = 2L;
        event2.setEventLocation(location);
        event2.setName("Event 2");

        allowance1 = new EventUserAllowance();
        allowance1.setUser(user);
        allowance1.setEvent(event1);
        allowance1.setReservationsAllowedCount(5);

        reservation1 = new Reservation();
        reservation1.setUser(user);
        reservation1.setEvent(event1);

        reservation2 = new Reservation();
        reservation2.setUser(user);
        reservation2.setEvent(event2);
    }

    @Test
    void getEventsForCurrentUser_Success_OnlyAllowances() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance1));
        when(reservationRepository.findByUser(user)).thenReturn(Collections.emptyList());

        List<UserEventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(event1.id, result.getFirst().id());
        assertEquals(5, result.getFirst().reservationsAllowed());
    }

    @Test
    void getEventsForCurrentUser_Success_OnlyReservations() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(reservationRepository.findByUser(user)).thenReturn(List.of(reservation2));

        List<UserEventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(event2.id, result.getFirst().id());
        assertEquals(
                0, result.getFirst().reservationsAllowed()); // Should be 0 for reservations only
    }

    @Test
    void getEventsForCurrentUser_Success_AllowancesAndDifferentReservations() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance1));
        when(reservationRepository.findByUser(user)).thenReturn(List.of(reservation2));

        List<UserEventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertFalse(result.isEmpty());
        assertEquals(2, result.size());

        // Check allowance event
        assertTrue(
                result.stream()
                        .anyMatch(
                                dto ->
                                        dto.id().equals(event1.id)
                                                && dto.reservationsAllowed() == 5));
        // Check reservation event
        assertTrue(
                result.stream()
                        .anyMatch(
                                dto ->
                                        dto.id().equals(event2.id)
                                                && dto.reservationsAllowed() == 0));
    }

    @Test
    void getEventsForCurrentUser_Success_AllowancesAndSameEventReservations() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance1));
        when(reservationRepository.findByUser(user))
                .thenReturn(List.of(reservation1)); // reservation for event1

        List<UserEventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(event1.id, result.getFirst().id());
        assertEquals(
                5, result.getFirst().reservationsAllowed()); // Allowance should take precedence
    }

    @Test
    void getEventsForCurrentUser_UserNotFoundException() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> eventService.getEventsForCurrentUser("unknownuser"));
    }

    @Test
    void getEventsForCurrentUser_Success_NoEvents() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(reservationRepository.findByUser(user)).thenReturn(Collections.emptyList());

        List<UserEventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertTrue(result.isEmpty());
    }
}
