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
package de.felixhertweck.seatreservation.supervisor.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInException;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInTokenNotFoundException;
import de.felixhertweck.seatreservation.supervisor.exception.EventMismatchException;
import de.felixhertweck.seatreservation.supervisor.exception.UserMismatchException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CheckInServiceTest {

    @Inject CheckInService checkInService;

    @InjectMock ReservationRepository reservationRepository;
    @InjectMock de.felixhertweck.seatreservation.model.repository.EventRepository eventRepository;

    @BeforeEach
    void setup() {
        // Authorize the test user (id 1) for any event in these tests
        when(eventRepository.isUserSupervisor(anyLong(), eq(1L))).thenReturn(true);
    }

    // Tests for processCheckIn(CheckInProcessRequestDTO) method

    @Test
    void testProcessCheckInByIds_successfulCheckIn() throws CheckInException {
        long reservationId1 = 1L;
        long reservationId2 = 2L;
        long userId = 1L;
        User user = new User();
        user.id = userId;

        // user already defined above
        long eventId = 10L;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation reservation1 = new Reservation();
        reservation1.id = reservationId1;
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);

        Reservation reservation2 = new Reservation();
        reservation2.id = reservationId2;
        reservation2.setUser(user);
        reservation2.setEvent(event);
        reservation2.setSeat(seat);

        when(reservationRepository.findByIdUserIdAndEventId(reservationId1, userId, eventId))
                .thenReturn(Optional.of(reservation1));
        when(reservationRepository.findByIdUserIdAndEventId(reservationId2, userId, eventId))
                .thenReturn(Optional.of(reservation2));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Arrays.asList(reservationId1, reservationId2),
                        Collections.emptyList());

        // current user is user with userId
        checkInService.processCheckIn(requestDTO, user);

        assertEquals(ReservationLiveStatus.CHECKED_IN, reservation1.getLiveStatus());
        assertEquals(ReservationLiveStatus.CHECKED_IN, reservation2.getLiveStatus());

        verify(reservationRepository, times(2)).persist(any(Reservation.class));
    }

    @Test
    void testProcessCheckInByIds_successfulCancel() throws CheckInException {
        long reservationId1 = 1L;
        long userId = 1L;
        long eventId = 10L;

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation reservation1 = new Reservation();
        reservation1.id = reservationId1;
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        when(reservationRepository.findByIdUserIdAndEventId(reservationId1, userId, eventId))
                .thenReturn(Optional.of(reservation1));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.emptyList(),
                        Collections.singletonList(reservationId1));

        checkInService.processCheckIn(requestDTO, user);

        assertEquals(ReservationLiveStatus.CANCELLED, reservation1.getLiveStatus());

        verify(reservationRepository, times(1)).persist(any(Reservation.class));
    }

    @Test
    void testProcessCheckInByIds_mixedCheckInAndCancel() throws CheckInException {
        long checkInId = 1L;
        long cancelId = 2L;
        long userId = 1L;
        long eventId = 10L;

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation checkInReservation = new Reservation();
        checkInReservation.id = checkInId;
        checkInReservation.setUser(user);
        checkInReservation.setEvent(event);
        checkInReservation.setSeat(seat);

        Reservation cancelReservation = new Reservation();
        cancelReservation.id = cancelId;
        cancelReservation.setUser(user);
        cancelReservation.setEvent(event);
        cancelReservation.setSeat(seat);

        when(reservationRepository.findByIdUserIdAndEventId(checkInId, userId, eventId))
                .thenReturn(Optional.of(checkInReservation));
        when(reservationRepository.findByIdUserIdAndEventId(cancelId, userId, eventId))
                .thenReturn(Optional.of(cancelReservation));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.singletonList(checkInId),
                        Collections.singletonList(cancelId));

        checkInService.processCheckIn(requestDTO, user);

        assertEquals(ReservationLiveStatus.CHECKED_IN, checkInReservation.getLiveStatus());
        assertEquals(ReservationLiveStatus.CANCELLED, cancelReservation.getLiveStatus());

        verify(reservationRepository, times(2)).persist(any(Reservation.class));
    }

    @Test
    void testProcessCheckInByIds_reservationNotFound() {
        long reservationId = 999L;
        long userId = 1L;
        long eventId = 10L;

        when(reservationRepository.findByIdUserIdAndEventId(reservationId, userId, eventId))
                .thenReturn(Optional.empty());

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.singletonList(reservationId),
                        Collections.emptyList());

        assertThrows(CheckInException.class, () -> checkInService.processCheckIn(requestDTO));

        verify(reservationRepository, never()).persist(any(Reservation.class));
    }

    @Test
    void testProcessCheckInByIds_emptyLists() throws CheckInException {
        long userId = 1L;
        long eventId = 10L;
        User user = new User();
        user.id = userId;

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId, userId, Collections.emptyList(), Collections.emptyList());

        checkInService.processCheckIn(requestDTO, user);

        verify(reservationRepository, never()).persist(any(Reservation.class));
    }

    // Tests for getReservationInfos(userId, eventId, tokens) method

    @Test
    void testGetReservationInfos_successfulCheckIn()
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        long userId = 1L;
        long eventId = 10L;
        String token1 = "token1";
        String token2 = "token2";

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation reservation1 = new Reservation();
        reservation1.setCheckInCode(token1);
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setStatus(ReservationStatus.RESERVED);

        Reservation reservation2 = new Reservation();
        reservation2.setCheckInCode(token2);
        reservation2.setUser(user);
        reservation2.setEvent(event);
        reservation2.setSeat(seat);
        reservation2.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findByCheckInCode(token1)).thenReturn(Optional.of(reservation1));
        when(reservationRepository.findByCheckInCode(token2)).thenReturn(Optional.of(reservation2));

        CheckInInfoResponseDTO result =
                checkInService.getReservationInfos(
                        user, userId, eventId, Arrays.asList(token1, token2));

        assertNotNull(result);
        assertEquals(2, result.reservations().size());
    }

    @Test
    void testGetReservationInfos_emptyTokenList()
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        long userId = 1L;
        long eventId = 10L;
        User user = new User();
        user.id = userId;

        CheckInInfoResponseDTO result =
                checkInService.getReservationInfos(user, userId, eventId, Collections.emptyList());

        assertNotNull(result);
        assertEquals(0, result.reservations().size());
    }

    @Test
    void testProcessCheckInByTokens_successfulCancel() throws CheckInException {
        long reservationId = 1L;

        User user = new User();
        user.id = 1L;
        Event event = new Event();
        event.id = 10L;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation reservation1 = new Reservation();
        reservation1.id = reservationId;
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        long userId = 1L;
        long eventId = 10L;

        when(reservationRepository.findByIdUserIdAndEventId(reservationId, userId, eventId))
                .thenReturn(Optional.of(reservation1));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.emptyList(),
                        Collections.singletonList(reservationId));

        checkInService.processCheckIn(requestDTO, user);

        assertEquals(ReservationLiveStatus.CANCELLED, reservation1.getLiveStatus());

        verify(reservationRepository, times(1)).persist(any(Reservation.class));
    }

    @Test
    void testGetReservationInfos_userMismatchException() {
        long userId = 1L;
        long eventId = 10L;
        String token1 = "token1";

        User otherUser = new User();
        otherUser.id = 2L;
        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;
        Reservation reservation1 = new Reservation();
        reservation1.setCheckInCode(token1);
        reservation1.setUser(otherUser);
        reservation1.setEvent(event);
        reservation1.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findByCheckInCode(token1)).thenReturn(Optional.of(reservation1));

        assertThrows(
                UserMismatchException.class,
                () ->
                        checkInService.getReservationInfos(
                                user, userId, eventId, Collections.singletonList(token1)));
    }

    @Test
    void testGetReservationInfos_eventMismatchException() {
        long userId = 1L;
        long eventId = 10L;
        String token1 = "token1";

        User user = new User();
        user.id = userId;
        Event otherEvent = new Event();
        otherEvent.id = 11L;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation reservation1 = new Reservation();
        reservation1.setCheckInCode(token1);
        reservation1.setUser(user);
        reservation1.setEvent(otherEvent);
        reservation1.setSeat(seat);
        reservation1.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findByCheckInCode(token1)).thenReturn(Optional.of(reservation1));

        assertThrows(
                EventMismatchException.class,
                () ->
                        checkInService.getReservationInfos(
                                userId, eventId, Collections.singletonList(token1)));
    }

    @Test
    void testGetReservationInfos_tokenNotFound() {
        long userId = 1L;
        long eventId = 10L;
        String token1 = "token1";

        when(reservationRepository.findByCheckInCode(token1)).thenReturn(Optional.empty());

        assertThrows(
                CheckInTokenNotFoundException.class,
                () ->
                        checkInService.getReservationInfos(
                                userId, eventId, Collections.singletonList(token1)));
    }

    @Test
    void testGetReservationInfos_multipleTokens()
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        long userId = 1L;
        long eventId = 10L;
        String token1 = "token1";
        String token2 = "token2";

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setLocation(location);

        Reservation reservation1 = new Reservation();
        reservation1.setCheckInCode(token1);
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setStatus(ReservationStatus.RESERVED);

        Reservation reservation2 = new Reservation();
        reservation2.setCheckInCode(token2);
        reservation2.setUser(user);
        reservation2.setEvent(event);
        reservation2.setSeat(seat);
        reservation2.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findByCheckInCode(token1)).thenReturn(Optional.of(reservation1));
        when(reservationRepository.findByCheckInCode(token2)).thenReturn(Optional.of(reservation2));

        CheckInInfoResponseDTO result =
                checkInService.getReservationInfos(userId, eventId, Arrays.asList(token1, token2));

        assertNotNull(result);
        assertEquals(2, result.reservations().size());
    }

    @Test
    void testGetUsernamesWithReservations_SupervisorUnauthorized_Throws() {
        long eventId = 20L;
        User user = new User();
        user.id = 1L;
        user.setRoles(Set.of(Roles.SUPERVISOR));
        when(eventRepository.isUserSupervisor(eq(eventId), eq(1L))).thenReturn(false);
        assertThrows(
                SecurityException.class,
                () -> checkInService.getUsernamesWithReservations(user, eventId));
    }

    @Test
    void testGetUsernamesWithReservations_AdminAllowed() {
        long eventId = 10L;
        User admin = new User();
        admin.id = 2L;
        admin.setRoles(Set.of(Roles.ADMIN));

        // create reservations for event
        User uname1 = new User();
        uname1.setUsername("user1");
        Reservation r1 = new Reservation();
        r1.setUser(uname1);
        r1.setEvent(new Event());
        r1.getEvent().id = eventId;

        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<Reservation> q =
                (io.quarkus.hibernate.orm.panache.PanacheQuery<Reservation>)
                        mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(q.stream()).thenReturn(Stream.of(r1));
        when(reservationRepository.find("event.id", eventId)).thenReturn(q);

        List<String> usernames = checkInService.getUsernamesWithReservations(admin, eventId);
        assertEquals(1, usernames.size());
    }

    @Test
    void testGetAllEventsForSupervisor_filtersProperly() {
        User supervisor = new User();
        supervisor.id = 1L;
        Event e1 = new Event();
        e1.id = 10L;
        Event e2 = new Event();
        e2.id = 20L;
        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<Event> eq =
                (io.quarkus.hibernate.orm.panache.PanacheQuery<Event>)
                        mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eq.stream()).thenReturn(Stream.of(e1, e2));
        when(eventRepository.findAll()).thenReturn(eq);
        when(eventRepository.isUserSupervisor(eq(10L), eq(1L))).thenReturn(true);
        when(eventRepository.isUserSupervisor(eq(20L), eq(1L))).thenReturn(false);
        List<de.felixhertweck.seatreservation.supervisor.dto.SupervisorEventResponseDTO> events =
                checkInService.getAllEventsForSupervisor(supervisor);
        assertEquals(1, events.size());
    }
}
