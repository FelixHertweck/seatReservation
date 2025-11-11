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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.management.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.management.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.utils.CodeGenerator;
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
    @InjectMock EmailService emailService;

    @Inject ReservationService reservationService;

    private User adminUser;
    private User regularUser;
    private User managerUser;
    private Event event;
    private Seat seat;
    private Reservation reservation;
    private EventUserAllowance allowance;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        Mockito.reset(reservationRepository);

        Mockito.reset(eventRepository);
        Mockito.reset(seatRepository);
        Mockito.reset(userRepository);
        Mockito.reset(eventUserAllowanceRepository);
        Mockito.reset(emailService);

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

        EventLocation eventLocation =
                new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        eventLocation.id = 1L;

        event = new Event();
        event.id = 1L;
        event.setName("Konzert");
        event.setEventLocation(eventLocation);
        event.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        event.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(10).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        event.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(1).toSeconds()));
        event.setManager(managerUser);

        seat = new Seat("A1", eventLocation, "1", 1, 1, "A");
        seat.id = 1L;

        reservation =
                new Reservation(
                        regularUser,
                        event,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());
        reservation.id = 1L;

        allowance = new EventUserAllowance(regularUser, event, 1);
    }

    @Test
    void createReservation_Success_AsAdmin() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
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

        Set<ReservationResponseDTO> created = reservationService.createReservations(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));
        assertEquals(0, allowance.getReservationsAllowedCount()); // Allowance should be decremented
    }

    @Test
    void createReservation_Success_AsAdmin_NoAllowanceDeduction() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
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

        Set<ReservationResponseDTO> created = reservationService.createReservations(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository, never())
                .persist(any(EventUserAllowance.class)); // Verify allowance was not persisted
    }

    @Test
    void createReservation_Success_AsManager_NoAllowanceDeduction() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
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

        Set<ReservationResponseDTO> created =
                reservationService.createReservations(dto, managerUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository, never())
                .persist(any(EventUserAllowance.class)); // Verify allowance was not persisted
    }

    @Test
    void createReservation_Success_AsManager() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
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

        Set<ReservationResponseDTO> created =
                reservationService.createReservations(dto, managerUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));
        assertEquals(0, allowance.getReservationsAllowedCount()); // Allowance should be decremented
    }

    @Test
    void createReservation_Forbidden_AsUser() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
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
        ReservationRequestDTO dto = new ReservationRequestDTO();
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
        ReservationRequestDTO dto = new ReservationRequestDTO();
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

        ReservationResponseDTO found =
                reservationService.findReservationById(reservation.id, adminUser);

        assertNotNull(found);
        assertEquals(reservation.id, found.id());
    }

    @Test
    void findReservationById_Success_AsManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        ReservationResponseDTO found =
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
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        reservationService.deleteReservation(List.of(reservation.id), adminUser);

        verify(reservationRepository, times(1)).delete(reservation);
    }

    @Test
    void deleteReservation_Success_AsManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
    }

    @Test
    void deleteReservation_Forbidden() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                SecurityException.class,
                () -> reservationService.deleteReservation(List.of(reservation.id), regularUser));
        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    @Test
    void deleteReservation_Success_WithAllowanceIncrement() {
        // Set up allowance with initial count
        allowance.setReservationsAllowedCount(0);

        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
        verify(eventUserAllowanceRepository, times(1)).persist(allowance);
        assertEquals(1, allowance.getReservationsAllowedCount()); // Allowance should be incremented
    }

    @Test
    void deleteReservation_Success_NoAllowanceExists() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.empty());

        // Should not throw exception when no allowance exists
        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
        verify(eventUserAllowanceRepository, never())
                .persist(any(EventUserAllowance.class)); // Should not persist allowance
    }

    @Test
    void deleteReservation_Success_BlockedReservation_NoAllowanceIncrement() {
        // Create a blocked reservation
        Reservation blockedReservation =
                new Reservation(
                        regularUser,
                        event,
                        seat,
                        Instant.now(),
                        ReservationStatus.BLOCKED,
                        CodeGenerator.generateRandomCode());
        blockedReservation.id = 2L;

        when(reservationRepository.findByIdOptional(blockedReservation.id))
                .thenReturn(Optional.of(blockedReservation));

        reservationService.deleteReservation(List.of(blockedReservation.id), managerUser);

        verify(reservationRepository, times(1)).delete(blockedReservation);
        // Verify allowance was never updated for blocked reservations
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void deleteReservation_Success_MultipleReservations_WithAllowanceIncrement() {
        // Create multiple reservations for the same user and event
        Seat seat2 = new Seat("A2", event.getEventLocation(), "2", 1, 2, "");
        seat2.id = 2L;
        Reservation reservation2 =
                new Reservation(
                        regularUser,
                        event,
                        seat2,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());
        reservation2.id = 2L;

        // Set up allowance with initial count
        allowance.setReservationsAllowedCount(0);

        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.findByIdOptional(reservation2.id))
                .thenReturn(Optional.of(reservation2));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        reservationService.deleteReservation(List.of(reservation.id, reservation2.id), managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
        verify(reservationRepository, times(1)).delete(reservation2);
        // Allowance should be incremented twice (once for each reservation)
        verify(eventUserAllowanceRepository, times(2)).persist(allowance);
        assertEquals(2, allowance.getReservationsAllowedCount());
    }

    @Test
    void deleteReservation_Success_MixedStatus_OnlyReservedIncrementsAllowance() {
        // Create one reserved and one blocked reservation
        Reservation blockedReservation =
                new Reservation(
                        regularUser,
                        event,
                        seat,
                        Instant.now(),
                        ReservationStatus.BLOCKED,
                        CodeGenerator.generateRandomCode());
        blockedReservation.id = 2L;

        Seat seat2 = new Seat("A2", event.getEventLocation(), "2", 1, 2, "A");
        seat2.id = 3L;
        Reservation reservedReservation =
                new Reservation(
                        regularUser,
                        event,
                        seat2,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());
        reservedReservation.id = 3L;

        allowance.setReservationsAllowedCount(0);

        when(reservationRepository.findByIdOptional(blockedReservation.id))
                .thenReturn(Optional.of(blockedReservation));
        when(reservationRepository.findByIdOptional(reservedReservation.id))
                .thenReturn(Optional.of(reservedReservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        reservationService.deleteReservation(
                List.of(blockedReservation.id, reservedReservation.id), managerUser);

        verify(reservationRepository, times(1)).delete(blockedReservation);
        verify(reservationRepository, times(1)).delete(reservedReservation);
        // Allowance should be incremented only once (for the reserved reservation)
        verify(eventUserAllowanceRepository, times(1)).persist(allowance);
        assertEquals(1, allowance.getReservationsAllowedCount());
    }

    @Test
    void deleteReservation_Success_DifferentAllowanceCounts() {
        // Test with different starting allowance counts
        allowance.setReservationsAllowedCount(5);

        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
        verify(eventUserAllowanceRepository, times(1)).persist(allowance);
        assertEquals(6, allowance.getReservationsAllowedCount());
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
                () -> reservationService.deleteReservation(List.of(reservation.id), managerUser));
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

    @Test
    void exportReservationsToPdf_Forbidden() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        event.setManager(new User()); // Different manager

        assertThrows(
                SecurityException.class,
                () -> reservationService.exportReservationsToPdf(event.id, managerUser));
    }

    @Test
    void exportReservationsToPdf_EventNotFound() {
        when(eventRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(
                de.felixhertweck.seatreservation.common.exception.EventNotFoundException.class,
                () -> reservationService.exportReservationsToPdf(99L, adminUser));
    }
}
