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

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.management.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.management.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
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
import de.felixhertweck.seatreservation.reservation.service.CheckInTokenService;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class ReservationServiceTest {

    @InjectMock ReservationRepository reservationRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;
    @InjectMock EmailService emailService;

    @InjectMock CheckInTokenService checkInTokenService;

    @Inject ReservationService reservationService;

    private User adminUser;
    private User regularUser;
    private User managerUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser regularAuth;
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
        Mockito.reset(checkInTokenService);
        when(checkInTokenService.getOrCreateForUser(any(), any()))
                .thenAnswer(
                        inv ->
                                new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                                        inv.getArgument(0), inv.getArgument(1), "CODE123"));

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

        adminAuth = new AuthenticatedUser(adminUser.id, adminUser.getRoles());
        managerAuth = new AuthenticatedUser(managerUser.id, managerUser.getRoles());
        regularAuth = new AuthenticatedUser(regularUser.id, regularUser.getRoles());
        when(userRepository.getReference(managerUser.id)).thenReturn(managerUser);
        when(userRepository.getReference(regularUser.id)).thenReturn(regularUser);

        EventLocation eventLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser);
        eventLocation.id = id(1);

        event = new Event();
        event.id = id(1);
        event.setName("Konzert");
        event.setEventLocation(eventLocation);
        event.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        event.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(10).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        event.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(1).toSeconds()));
        event.setManager(managerUser);

        when(eventRepository.isUserManager(any(UUID.class), any(UUID.class)))
                .thenAnswer(
                        invocation -> {
                            UUID eventId = invocation.getArgument(0);
                            UUID userId = invocation.getArgument(1);
                            return event != null
                                    && event.id.equals(eventId)
                                    && event.getManagers() != null
                                    && event.getManagers().stream()
                                            .anyMatch(u -> u.id != null && u.id.equals(userId));
                        });

        seat =
                new Seat(
                        "A1",
                        eventLocation,
                        "1",
                        1,
                        1,
                        new EventLocationEntrance("A"),
                        new EventLocationArea("Parkett"));
        seat.id = id(1);

        reservation =
                new Reservation(
                        regularUser,
                        event,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                                regularUser, event, "CODE123"));
        reservation.id = id(1);

        allowance = new EventUserAllowance(regularUser, event, 1);
    }

    private void mockSeatFind(Collection<UUID> seatIds, List<Seat> seats) {
        when(seatRepository.findByIds(seatIds.stream().toList())).thenReturn(seats);
    }

    private void mockReservationFind(Collection<UUID> ids, List<Reservation> reservations) {
        when(reservationRepository.findByIds(ids.stream().toList())).thenReturn(reservations);
    }

    @Test
    void createReservation_Success_AsAdmin() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatIds(Set.of(seat.id));
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        mockSeatFind(dto.getSeatIds(), List.of(seat));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        doAnswer(
                        inv -> {
                            List<Reservation> reservations = inv.getArgument(0);
                            reservations.forEach(res -> res.id = id(99));
                            return null;
                        })
                .when(reservationRepository)
                .persistAll(anyList());

        Set<ReservationResponseDTO> created = reservationService.createReservations(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persistAll(anyList());
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
        mockSeatFind(dto.getSeatIds(), List.of(seat));
        // No allowance setup needed as it should be skipped

        doAnswer(
                        inv -> {
                            List<Reservation> reservations = inv.getArgument(0);
                            reservations.forEach(res -> res.id = id(99));
                            return null;
                        })
                .when(reservationRepository)
                .persistAll(anyList());

        Set<ReservationResponseDTO> created = reservationService.createReservations(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persistAll(anyList());
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
        mockSeatFind(dto.getSeatIds(), List.of(seat));
        // No allowance setup needed as it should be skipped

        doAnswer(
                        inv -> {
                            List<Reservation> reservations = inv.getArgument(0);
                            reservations.forEach(res -> res.id = id(99));
                            return null;
                        })
                .when(reservationRepository)
                .persistAll(anyList());

        Set<ReservationResponseDTO> created =
                reservationService.createReservations(dto, managerUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persistAll(anyList());
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
        mockSeatFind(dto.getSeatIds(), List.of(seat));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        Set<ReservationResponseDTO> created =
                reservationService.createReservations(dto, managerUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.iterator().next().user().id());
        verify(reservationRepository).persistAll(anyList());
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
        mockSeatFind(dto.getSeatIds(), List.of(seat));

        assertThrows(
                AccessDeniedException.class,
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
        mockSeatFind(dto.getSeatIds(), List.of(seat));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.empty());

        assertThrows(
                ValidationException.class,
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
        mockSeatFind(dto.getSeatIds(), List.of(seat));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.of(allowance));

        assertThrows(
                ValidationException.class,
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
        otherManager.id = id(4);
        otherManager.setRoles(Set.of(Roles.MANAGER));
        reservation.getEvent().setManager(otherManager);

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.findReservationById(reservation.id, managerUser));
    }

    @Test
    void deleteReservation_Success_AsAdmin() {
        mockReservationFind(List.of(reservation.id), List.of(reservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.empty());

        reservationService.deleteReservation(List.of(reservation.id), adminUser);

        verify(reservationRepository, times(1)).deleteByIds(List.of(reservation.id));
    }

    @Test
    void deleteReservation_Success_AsManager() {
        mockReservationFind(List.of(reservation.id), List.of(reservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.empty());

        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).deleteByIds(List.of(reservation.id));
    }

    @Test
    void deleteReservation_Forbidden() {
        mockReservationFind(List.of(reservation.id), List.of(reservation));
        List<UUID> ids = List.of(reservation.id);

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.deleteReservation(ids, regularUser));
        verify(reservationRepository, never()).deleteByIds(any());
    }

    @Test
    void deleteReservation_Success_WithAllowanceIncrement() {
        // Set up allowance with initial count
        allowance.setReservationsAllowedCount(0);

        mockReservationFind(List.of(reservation.id), List.of(reservation));
        when(eventUserAllowanceRepository.findByEventAndUserIds(event, Set.of(regularUser.id)))
                .thenReturn(List.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(Iterable.class));

        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).deleteByIds(List.of(reservation.id));
        verify(eventUserAllowanceRepository, times(1)).persist(List.of(allowance));
        assertEquals(1, allowance.getReservationsAllowedCount()); // Allowance should be incremented
    }

    @Test
    void deleteReservation_Success_NoAllowanceExists() {
        mockReservationFind(List.of(reservation.id), List.of(reservation));
        when(eventUserAllowanceRepository.findByUserAndEvent(regularUser, event))
                .thenReturn(Optional.empty());

        // Should not throw exception when no allowance exists
        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).deleteByIds(List.of(reservation.id));
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
                        (de.felixhertweck.seatreservation.model.entity.CheckInToken) null);
        blockedReservation.id = id(2);

        mockReservationFind(List.of(blockedReservation.id), List.of(blockedReservation));

        reservationService.deleteReservation(List.of(blockedReservation.id), managerUser);

        verify(reservationRepository, times(1)).deleteByIds(List.of(blockedReservation.id));
        // Verify allowance was never updated for blocked reservations
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void deleteReservation_Success_MultipleReservations_WithAllowanceIncrement() {
        // Create multiple reservations for the same user and event
        Seat seat2 = new Seat("A2", event.getEventLocation(), "2", 1, 2, null, null);
        seat2.id = id(2);
        Reservation reservation2 =
                new Reservation(
                        regularUser,
                        event,
                        seat2,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                                regularUser, event, "CODE123"));
        reservation2.id = id(2);

        // Set up allowance with initial count
        allowance.setReservationsAllowedCount(0);

        mockReservationFind(
                List.of(reservation.id, reservation2.id), List.of(reservation, reservation2));
        when(eventUserAllowanceRepository.findByEventAndUserIds(event, Set.of(regularUser.id)))
                .thenReturn(List.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(Iterable.class));

        reservationService.deleteReservation(List.of(reservation.id, reservation2.id), managerUser);

        verify(reservationRepository, times(1))
                .deleteByIds(List.of(reservation.id, reservation2.id));
        // Allowance should be incremented twice, but persisted in a single batch
        verify(eventUserAllowanceRepository, times(1)).persist(List.of(allowance));
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
                        (de.felixhertweck.seatreservation.model.entity.CheckInToken) null);
        blockedReservation.id = id(2);

        Seat seat2 =
                new Seat(
                        "A2",
                        event.getEventLocation(),
                        "2",
                        1,
                        2,
                        new EventLocationEntrance("A"),
                        new EventLocationArea("Balkon"));
        seat2.id = id(3);
        Reservation reservedReservation =
                new Reservation(
                        regularUser,
                        event,
                        seat2,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                                regularUser, event, "CODE123"));
        reservedReservation.id = id(3);

        allowance.setReservationsAllowedCount(0);

        mockReservationFind(
                List.of(blockedReservation.id, reservedReservation.id),
                List.of(blockedReservation, reservedReservation));
        when(eventUserAllowanceRepository.findByEventAndUserIds(event, Set.of(regularUser.id)))
                .thenReturn(List.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(Iterable.class));

        reservationService.deleteReservation(
                List.of(blockedReservation.id, reservedReservation.id), managerUser);

        verify(reservationRepository, times(1))
                .deleteByIds(List.of(blockedReservation.id, reservedReservation.id));
        // Allowance should be incremented only once (for the reserved reservation)
        verify(eventUserAllowanceRepository, times(1)).persist(List.of(allowance));
        assertEquals(1, allowance.getReservationsAllowedCount());
    }

    @Test
    void deleteReservation_Success_DifferentAllowanceCounts() {
        // Test with different starting allowance counts
        allowance.setReservationsAllowedCount(5);

        mockReservationFind(List.of(reservation.id), List.of(reservation));
        when(eventUserAllowanceRepository.findByEventAndUserIds(event, Set.of(regularUser.id)))
                .thenReturn(List.of(allowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(Iterable.class));

        reservationService.deleteReservation(List.of(reservation.id), managerUser);

        verify(reservationRepository, times(1)).deleteByIds(List.of(reservation.id));
        verify(eventUserAllowanceRepository, times(1)).persist(List.of(allowance));
        assertEquals(6, allowance.getReservationsAllowedCount());
    }

    @Test
    void findReservationById_NotFoundException() {
        UUID notFoundId = id(99);
        when(reservationRepository.findByIdOptional(notFoundId)).thenReturn(Optional.empty());

        assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.findReservationById(notFoundId, adminUser));
    }

    @Test
    void deleteReservation_Forbidden_NotManager() {
        mockReservationFind(List.of(reservation.id), List.of(reservation));
        User otherManager = new User();
        otherManager.id = id(4);
        otherManager.setRoles(Set.of(Roles.MANAGER));
        reservation.getEvent().setManager(otherManager);
        List<UUID> ids = List.of(reservation.id);

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.deleteReservation(ids, managerUser));
    }

    @Test
    void blockSeats_Success() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        mockSeatFind(List.of(seat.id), List.of(seat));
        when(reservationRepository.findByEventIdAndSeatIds(event.id, List.of(seat.id)))
                .thenReturn(Collections.emptyList());

        reservationService.blockSeats(event.id, List.of(seat.id), managerUser);

        verify(reservationRepository).persist(any(Iterable.class));
    }

    @Test
    void blockSeats_Forbidden() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        List<UUID> seatIds = List.of(seat.id);

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.blockSeats(event.id, seatIds, regularUser));
    }

    @Test
    void blockSeats_SeatAlreadyReserved() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        mockSeatFind(List.of(seat.id), List.of(seat));
        when(reservationRepository.findByEventIdAndSeatIds(event.id, List.of(seat.id)))
                .thenReturn(List.of(reservation));
        List<UUID> seatIds = List.of(seat.id);

        assertThrows(
                ValidationException.class,
                () -> reservationService.blockSeats(event.id, seatIds, managerUser));
    }

    @Test
    void exportReservationsToPdf_Forbidden() {
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        event.setManager(new User()); // Different manager

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.exportReservationsToPdf(event.id, managerUser));
    }

    @Test
    void exportReservationsToPdf_EventNotFound() {
        when(eventRepository.findByIdOptional(id(99))).thenReturn(Optional.empty());

        assertThrows(
                de.felixhertweck.seatreservation.common.exception.EventNotFoundException.class,
                () -> reservationService.exportReservationsToPdf(id(99), adminUser));
    }
}
