package de.felixhertweck.seatreservation.eventManagement.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.core.SecurityContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.eventManagement.dto.DetailedReservationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.security.Roles;
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
    @InjectMock SecurityContext securityContext;

    @Inject ReservationService reservationService;

    private User adminUser;
    private User regularUser;
    private User managerUser;
    private Event event;
    private Seat seat;
    private Reservation reservation;
    private EventUserAllowance allowance;
    private EventUserAllowance managerAllowance;

    @BeforeEach
    void setUp() {
        Mockito.reset(
                reservationRepository,
                eventRepository,
                seatRepository,
                userRepository,
                eventUserAllowanceRepository,
                securityContext);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        "hash",
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN));
        adminUser.id = 1L;

        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        "hash",
                        "Regular",
                        "User",
                        Set.of(Roles.USER));
        regularUser.id = 2L;

        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        "hash",
                        "Event",
                        "Manager",
                        Set.of(Roles.MANAGER));
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

        reservation = new Reservation(regularUser, event, seat, LocalDateTime.now());
        reservation.id = 1L;

        allowance = new EventUserAllowance(regularUser, event, 1);
        managerAllowance = new EventUserAllowance(managerUser, event, 10);
    }

    @Test
    void createReservation_Success_AsAdmin() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatId(seat.id);
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(true);

        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.singleResult()).thenReturn(allowance);
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> adminAllowanceQuery = mock(PanacheQuery.class);
        when(adminAllowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", adminUser, event))
                .thenReturn(adminAllowanceQuery);

        doAnswer(
                        inv -> {
                            Reservation res = inv.getArgument(0);
                            res.id = 99L;
                            return null;
                        })
                .when(reservationRepository)
                .persist(any(Reservation.class));

        DetailedReservationResponseDTO created =
                reservationService.createReservation(dto, adminUser);

        assertNotNull(created);
        assertEquals(regularUser.id, created.user().id());
        verify(reservationRepository).persist(any(Reservation.class));
        verify(eventUserAllowanceRepository).persist(allowance);
        assertEquals(0, allowance.getReservationsAllowedCount());
    }

    @Test
    void createReservation_Forbidden_AsUser() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatId(seat.id);
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(false);

        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        assertThrows(
                SecurityException.class,
                () -> reservationService.createReservation(dto, regularUser));
    }

    @Test
    void createReservation_NoAllowance() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatId(seat.id);
        dto.setUserId(regularUser.id);

        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(true);

        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.singleResult()).thenThrow(new NoResultException());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> adminAllowanceQuery = mock(PanacheQuery.class);
        when(adminAllowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", adminUser, event))
                .thenReturn(adminAllowanceQuery);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(dto, adminUser));
    }

    @Test
    void findReservationById_Success_AsAdmin() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(true);

        DetailedReservationResponseDTO found =
                reservationService.findReservationById(reservation.id, adminUser);

        assertNotNull(found);
        assertEquals(reservation.id, found.id());
    }

    @Test
    void findReservationById_Success_AsManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.of(managerAllowance));
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", managerUser, event))
                .thenReturn(allowanceQuery);

        DetailedReservationResponseDTO found =
                reservationService.findReservationById(reservation.id, managerUser);

        assertNotNull(found);
        assertEquals(reservation.id, found.id());
    }

    @Test
    void findReservationById_Forbidden() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.empty()); // No allowance
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        assertThrows(
                SecurityException.class,
                () -> reservationService.findReservationById(reservation.id, regularUser));
    }

    @Test
    void deleteReservation_Success_AsAdmin() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(true);

        reservationService.deleteReservation(reservation.id, adminUser);

        verify(reservationRepository, times(1)).delete(reservation);
    }

    @Test
    void deleteReservation_Success_AsManager() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(securityContext.isUserInRole(Roles.MANAGER)).thenReturn(true);
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.of(managerAllowance));
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", managerUser, event))
                .thenReturn(allowanceQuery);

        reservationService.deleteReservation(reservation.id, managerUser);

        verify(reservationRepository, times(1)).delete(reservation);
    }

    @Test
    void deleteReservation_Forbidden() {
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(securityContext.isUserInRole(Roles.MANAGER)).thenReturn(false);
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> reservationService.deleteReservation(reservation.id, regularUser));
        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    @Test
    void findAllReservations_Success_AsAdmin() {
        when(reservationRepository.listAll()).thenReturn(List.of(reservation));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(true);

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
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(false);
        when(securityContext.isUserInRole(Roles.MANAGER)).thenReturn(true);
        managerUser.setEventAllowances(Set.of(managerAllowance));

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
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(false);
        when(securityContext.isUserInRole(Roles.MANAGER)).thenReturn(true);

        var result = reservationService.findAllReservations(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAllReservations_ForbiddenException_OtherRoles() {
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(false);
        when(securityContext.isUserInRole(Roles.MANAGER)).thenReturn(false);
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
    void updateReservation_Success_AsAdmin() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setEventId(event.id);
        dto.setSeatId(seat.id);
        dto.setUserId(regularUser.id);

        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        when(eventRepository.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(seatRepository.findByIdOptional(seat.id)).thenReturn(Optional.of(seat));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(true);
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", adminUser, event))
                .thenReturn(allowanceQuery);

        DetailedReservationResponseDTO updated =
                reservationService.updateReservation(reservation.id, dto, adminUser);

        assertNotNull(updated);
        assertEquals(regularUser.id, updated.user().id());
        verify(reservationRepository).persist(reservation);
    }

    @Test
    void updateReservation_NotFoundException_ReservationNotFound() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        when(reservationRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.updateReservation(99L, dto, adminUser));
    }

    @Test
    void updateReservation_ForbiddenException_NotAllowed() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        when(reservationRepository.findByIdOptional(reservation.id))
                .thenReturn(Optional.of(reservation));
        when(securityContext.isUserInRole(Roles.ADMIN)).thenReturn(false);
        when(securityContext.isUserInRole(Roles.MANAGER)).thenReturn(false);
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> allowanceQuery = mock(PanacheQuery.class);
        when(allowanceQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.find("user = ?1 and event = ?2", regularUser, event))
                .thenReturn(allowanceQuery);

        assertThrows(
                SecurityException.class,
                () -> reservationService.updateReservation(reservation.id, dto, regularUser));
    }
}
