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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.management.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.reservation.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.ReservationsRequestCreateDTO;
import de.felixhertweck.seatreservation.reservation.exception.EventBookingClosedException;
import de.felixhertweck.seatreservation.reservation.exception.NoSeatsAvailableException;
import de.felixhertweck.seatreservation.reservation.exception.SeatAlreadyReservedException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReservationServiceTest {

    @Inject ReservationService reservationService;

    @InjectMock ReservationRepository reservationRepository;

    @InjectMock EventRepository eventRepository;

    @InjectMock SeatRepository seatRepository;

    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    private User currentUser;
    private User otherUser;
    private Event event;
    private Seat seat1;
    private Seat seat2;
    private Reservation reservation;
    private EventUserAllowance allowance;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.id = 1L;
        currentUser.setUsername("testuser");
        currentUser.setEmail("user@example.com");
        currentUser.setEmailVerified(true);

        otherUser = new User();
        otherUser.id = 2L;
        otherUser.setUsername("otheruser");

        var location = new EventLocation();
        location.id = 1L;
        location.setName("Test Location");
        location.setAddress("Test Address");

        event = new Event();
        event.id = 1L;
        event.setName("Test Event for Reservation");
        event.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(1).toSeconds()));
        event.setStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        event.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        event.setEventLocation(location);

        seat1 = new Seat();
        seat1.id = 1L;
        seat1.setLocation(location);

        seat2 = new Seat();
        seat2.id = 2L;
        seat2.setLocation(location);

        reservation =
                new Reservation(
                        currentUser, event, seat1, Instant.now(), ReservationStatus.RESERVED);
        reservation.id = 1L;

        allowance = new EventUserAllowance();
        allowance.setEvent(event);
        allowance.setUser(currentUser);
        allowance.setReservationsAllowedCount(2);
    }

    @Test
    void findReservationsByUser_Success() {
        when(reservationRepository.findByUser(currentUser)).thenReturn(List.of(reservation));

        List<ReservationResponseDTO> result =
                reservationService.findReservationsByUser(currentUser);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(reservation.id, result.getFirst().id());
    }

    @Test
    void findReservationsByUser_Success_NoReservations() {
        when(reservationRepository.findByUser(currentUser)).thenReturn(Collections.emptyList());

        List<ReservationResponseDTO> result =
                reservationService.findReservationsByUser(currentUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void findReservationByIdForUser_Success() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.of(reservation));

        ReservationResponseDTO result =
                reservationService.findReservationByIdForUser(1L, currentUser);

        assertNotNull(result);
        assertEquals(reservation.id, result.id());
    }

    @Test
    void findReservationByIdForUser_NotFoundException() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

        assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.findReservationByIdForUser(1L, currentUser));
    }

    @Test
    void findReservationByIdForUser_ForbiddenException() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.of(reservation));

        assertThrows(
                SecurityException.class,
                () -> reservationService.findReservationByIdForUser(1L, otherUser));
    }

    @Test
    void createReservationForUser_Success() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat1.id));

        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat1.id)).thenReturn(Optional.of(seat1));
        when(eventUserAllowanceRepository.findByUser(currentUser)).thenReturn(List.of(allowance));
        when(reservationRepository.findByEventId(event.id)).thenReturn(Collections.emptyList());
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        List<ReservationResponseDTO> result =
                reservationService.createReservationForUser(dto, currentUser);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void createReservationForUser_IllegalStateException_EmailNotVerified() {
        currentUser.setEmailVerified(false);
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat1.id));

        var exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> reservationService.createReservationForUser(dto, currentUser));

        assertEquals(
                "User must have a verified email address to create a reservation.",
                exception.getMessage());
    }

    @Test
    void createReservationForUser_IllegalArgumentException_NoSeatIds() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Collections.emptySet());

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void createReservationForUser_NotFoundException_EventNotFound() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(99L);
        dto.setSeatIds(Set.of(seat1.id));

        when(eventRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void createReservationForUser_NotFoundException_SeatNotFound() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(99L));

        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void createReservationForUser_ForbiddenException_NoAllowance() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat1.id));

        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat1.id)).thenReturn(Optional.of(seat1));
        when(eventUserAllowanceRepository.findByUser(currentUser))
                .thenReturn(Collections.emptyList());

        assertThrows(
                EventNotFoundException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void createReservationForUser_NoSeatsAvailableException_LimitReached() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat1.id, seat2.id, 3L)); // 3 seats, but only 2 allowed

        allowance.setReservationsAllowedCount(2);

        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat1.id)).thenReturn(Optional.of(seat1));
        when(seatRepository.findByIdOptional(seat2.id)).thenReturn(Optional.of(seat2));
        when(seatRepository.findByIdOptional(3L)).thenReturn(Optional.of(new Seat()));
        when(eventUserAllowanceRepository.findByUser(currentUser)).thenReturn(List.of(allowance));

        assertThrows(
                NoSeatsAvailableException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void createReservationForUser_EventBookingClosedException() {
        event.setBookingDeadline(Instant.now().minusSeconds(Duration.ofDays(1).toSeconds()));
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat1.id));

        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat1.id)).thenReturn(Optional.of(seat1));
        when(eventUserAllowanceRepository.findByUser(currentUser)).thenReturn(List.of(allowance));

        assertThrows(
                EventBookingClosedException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void createReservationForUser_SeatAlreadyReservedException() {
        ReservationsRequestCreateDTO dto = new ReservationsRequestCreateDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat1.id));

        Reservation existingReservation =
                new Reservation(otherUser, event, seat1, Instant.now(), ReservationStatus.RESERVED);
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat1.id)).thenReturn(Optional.of(seat1));
        when(eventUserAllowanceRepository.findByUser(currentUser)).thenReturn(List.of(allowance));
        when(reservationRepository.findByEventId(event.id))
                .thenReturn(List.of(existingReservation));

        assertThrows(
                SeatAlreadyReservedException.class,
                () -> reservationService.createReservationForUser(dto, currentUser));
    }

    @Test
    void deleteReservationForUser_Success() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.of(reservation));
        when(eventUserAllowanceRepository.findByUser(currentUser)).thenReturn(List.of(allowance));

        assertDoesNotThrow(() -> reservationService.deleteReservationForUser(1L, currentUser));
    }

    @Test
    void deleteReservationForUser_NotFoundException() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

        assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.deleteReservationForUser(1L, currentUser));
    }

    @Test
    void deleteReservationForUser_ForbiddenException_NotOwner() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.of(reservation));

        assertThrows(
                SecurityException.class,
                () -> reservationService.deleteReservationForUser(1L, otherUser));
    }

    @Test
    void deleteReservationForUser_ForbiddenException_NoAllowance() {
        when(reservationRepository.findByIdOptional(1L)).thenReturn(Optional.of(reservation));
        when(eventUserAllowanceRepository.findByUser(currentUser))
                .thenReturn(Collections.emptyList());

        assertThrows(
                SecurityException.class,
                () -> reservationService.deleteReservationForUser(1L, currentUser));
    }
}
