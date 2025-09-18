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
package de.felixhertweck.seatreservation.eventManagement.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.eventManagement.dto.ManagerReservationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ManagerReservationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class ReservationServiceTest {

    @InjectMock ReservationRepository reservationRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    @Inject ReservationService reservationService;

    private User adminUser;
    private User regularUser;
    private User managerUser;
    private Event event;
    private Seat seat;
    private Reservation reservation;
    private EventUserAllowance allowance;

    @BeforeEach
    void setUp() {
        Mockito.reset(reservationRepository);

        Mockito.reset(eventRepository);
        Mockito.reset(seatRepository);
        Mockito.reset(userRepository);
        Mockito.reset(eventUserAllowanceRepository);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        "hash",
                        "salt",
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN),
                        Set.of());
        adminUser.id = 1L;

        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        "hash",
                        "salt",
                        "Regular",
                        "User",
                        Set.of(Roles.USER),
                        Set.of());
        regularUser.id = 2L;

        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        "hash",
                        "salt",
                        "Event",
                        "Manager",
                        Set.of(Roles.MANAGER),
                        Set.of());
        managerUser.id = 3L;

        EventLocation eventLocation =
                new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        eventLocation.id = 1L;

        event = new Event();
        event.id = 1L;
        event.setName("Konzert");
        event.setEventLocation(eventLocation);
        event.setStartTime(LocalDateTime.now().plusDays(10));
        event.setEndTime(LocalDateTime.now().plusDays(10).plusHours(2));
        event.setBookingDeadline(LocalDateTime.now().plusDays(1));
        event.setManager(managerUser);

        seat = new Seat("A1", eventLocation, 1, 1);
        seat.id = 1L;

        reservation =
                new Reservation(
                        regularUser, event, seat, LocalDateTime.now(), ReservationStatus.RESERVED);
        reservation.id = 1L;

        allowance = new EventUserAllowance(regularUser, event, 1);
        new EventUserAllowance(managerUser, event, 10);
    }

    @Test
    void createReservation_Success_AsAdmin() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.singleResult()).thenReturn(allowance);
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        doAnswer(
                        inv -> {
                            Reservation res = inv.getArgument(0);
                            res.id = 99L;
                            return null;
                        })
                .when(reservationRepository)
                .persist(any(Reservation.class));

        Set<ManagerReservationResponseDTO> created =
                reservationService.createReservations(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));
        assertEquals(0, allowance.getReservationsAllowedCount()); // Allowance should be decremented
    }

    @Test
    void createReservation_Success_AsAdmin_NoAllowanceDeduction() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);
        dto.setDeductAllowance(false); // Set to false to skip deduction

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        // No allowance setup needed as it should be skipped

        doAnswer(
                        inv -> {
                            Reservation res = inv.getArgument(0);
                            res.id = 99L;
                            return null;
                        })
                .when(reservationRepository)
                .persist(any(Reservation.class));

        Set<ManagerReservationResponseDTO> created =
                reservationService.createReservations(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository, never())
                .persist(any(EventUserAllowance.class)); // Verify allowance was not persisted
    }

    @Test
    void createReservation_Success_AsManager_NoAllowanceDeduction() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);
        dto.setDeductAllowance(false); // Set to false to skip deduction

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        // No allowance setup needed as it should be skipped

        doAnswer(
                        inv -> {
                            Reservation res = inv.getArgument(0);
                            res.id = 99L;
                            return null;
                        })
                .when(reservationRepository)
                .persist(any(Reservation.class));

        Set<ManagerReservationResponseDTO> created =
                reservationService.createReservations(dto, managerUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository, never())
                .persist(any(EventUserAllowance.class)); // Verify allowance was not persisted
    }

    @Test
    void createReservation_Success_AsManager() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.singleResult()).thenReturn(allowance);
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        Set<ManagerReservationResponseDTO> created =
                reservationService.createReservations(dto, managerUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));
        assertEquals(0, allowance.getReservationsAllowedCount()); // Allowance should be decremented
    }

    @Test
    void createReservation_Forbidden_AsUser() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));

        assertThrows(
                SecurityException.class,
                () -> reservationService.createReservations(dto, regularUser));
    }

    @Test
    void createReservation_NoAllowance() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.singleResult()).thenThrow(new NoResultException());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservations(dto, adminUser));
    }

    @Test
    void createReservation_AllowanceZero() {
        ManagerReservationRequestDTO dto = new ManagerReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);
        allowance.setReservationsAllowedCount(0);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.singleResult()).thenReturn(allowance);
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservations(dto, managerUser));
    }

    @Test
    void findReservationById_Success_AsAdmin() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        ManagerReservationResponseDTO found =
                reservationService.findReservationById(reservation.id, adminUser);

        assertNotNull(found);
        assertEquals(reservation.id, found.id());
    }

    @Test
    void findReservationById_Success_AsManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        ManagerReservationResponseDTO found =
                reservationService.findReservationById(reservation.id, managerUser);

        assertNotNull(found);
        assertEquals(reservation.id, found.id());
    }

    @Test
    void findReservationById_Forbidden() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        User otherManager = new User();
        otherManager.id = 4L;
        otherManager.setRoles(Set.of(Roles.MANAGER));
        reservation.getEvent().setManager(otherManager);

        assertThrows(
                SecurityException.class,
                () -> reservationService.findReservationById(reservation.id, managerUser));
    }

    @Test
    void deleteReservation_Success_AsAdmin() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(reservation.id, adminUser);

        verify(reservationRepository, times(1)).delete(reservation);
    }

    @Test
    void deleteReservation_Success_AsManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(reservation.id, managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
    }

    @Test
    void deleteReservation_Forbidden() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                SecurityException.class,
                () -> reservationService.deleteReservation(reservation.id, regularUser));
        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    @Test
    void findAllReservations_Success_AsAdmin() {
        when(reservationRepository.listAll()).thenReturn(List.of(reservation));

        var result = reservationService.findAllReservations(adminUser);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(reservationRepository).listAll();
    }

    @Test
    void findAllReservations_Success_AsManager() {
        @SuppressWarnings("unchecked")
        PanacheQuery<Reservation> reservationQuery = mock(PanacheQuery.class);
        when(reservationQuery.list()).thenReturn(List.of(reservation));
        when(reservationRepository.find("event.manager", managerUser)).thenReturn(reservationQuery);

        var result = reservationService.findAllReservations(managerUser);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void findAllReservations_Success_NoAllowedEventsForManager() {
        @SuppressWarnings("unchecked")
        PanacheQuery<Reservation> reservationQuery = mock(PanacheQuery.class);
        when(reservationQuery.list()).thenReturn(Collections.emptyList());
        when(reservationRepository.find("event.manager", managerUser)).thenReturn(reservationQuery);

        var result = reservationService.findAllReservations(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAllReservations_ForbiddenException_OtherRoles() {
        @SuppressWarnings("unchecked")
        PanacheQuery<Reservation> reservationQuery = mock(PanacheQuery.class);
        when(reservationQuery.list()).thenReturn(Collections.emptyList());
        when(reservationRepository.find("event.manager", regularUser)).thenReturn(reservationQuery);

        var result = reservationService.findAllReservations(regularUser);
        assertTrue(result.isEmpty());
    }

    @Test
    void findReservationById_NotFoundException() {
        when(reservationRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.findReservationById(99L, adminUser));
    }

    @Test
    void deleteReservation_Forbidden_NotManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        User otherManager = new User();
        otherManager.id = 4L;
        otherManager.setRoles(Set.of(Roles.MANAGER));
        reservation.getEvent().setManager(otherManager);

        assertThrows(
                SecurityException.class,
                () -> reservationService.deleteReservation(reservation.id, managerUser));
    }

    @Test
    void blockSeats_Success() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        when(reservationRepository.findByEventId(event.id)).thenReturn(Collections.emptyList());

        reservationService.blockSeats(event.id, List.of(seat.id), managerUser);

        verify(reservationRepository).persist(any(Iterable.class));
    }

    @Test
    void blockSeats_Forbidden() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));

        assertThrows(
                SecurityException.class,
                () -> reservationService.blockSeats(event.id, List.of(seat.id), regularUser));
    }

    @Test
    void blockSeats_SeatAlreadyReserved() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        when(reservationRepository.findByEventId(event.id)).thenReturn(List.of(reservation));

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.blockSeats(event.id, List.of(seat.id), managerUser));
    }
}
