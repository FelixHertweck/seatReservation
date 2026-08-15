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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.CheckInTokenRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import de.felixhertweck.seatreservation.supervisor.exception.BookingDeadlineNotPassedException;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInException;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInTokenNotFoundException;
import de.felixhertweck.seatreservation.supervisor.exception.EventMismatchException;
import de.felixhertweck.seatreservation.supervisor.exception.UserMismatchException;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CheckInServiceTest {

    @Inject CheckInService checkInService;

    @InjectMock ReservationRepository reservationRepository;
    @InjectMock CheckInTokenRepository checkInTokenRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock UserRepository userRepository;

    @BeforeEach
    void setup() {
        // Authorize the test user (id 1) for any event in these tests
        when(eventRepository.isUserSupervisor(any(UUID.class), eq(id(1)))).thenReturn(true);
        // Default event used by loadEvent()/assertBookingDeadlinePassed(): deadline already
        // passed, so existing process-check-in tests keep working without per-test stubbing.
        Event defaultEvent = new Event();
        defaultEvent.setBookingDeadline(Instant.now().minusSeconds(3600));
        when(eventRepository.findById(any(UUID.class))).thenReturn(defaultEvent);
    }

    private static AuthenticatedUser auth(User user) {
        return new AuthenticatedUser(user.id, user.getRoles());
    }

    // Tests for processCheckIn(CheckInProcessRequestDTO) method

    @Test
    void testProcessCheckInByIds_successfulCheckIn() throws CheckInException {
        UUID reservationId1 = id(1);
        UUID reservationId2 = id(2);
        UUID userId = id(1);
        User user = new User();
        user.id = userId;

        // user already defined above
        UUID eventId = id(10);
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat seat = new Seat("", "", location);
        seat.id = id(1);

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

        when(reservationRepository.findAllByIdUserIdAndEventId(
                        Arrays.asList(reservationId1, reservationId2), userId, eventId))
                .thenReturn(Arrays.asList(reservation1, reservation2));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Arrays.asList(reservationId1, reservationId2),
                        Collections.emptyList());

        // current user is user with userId
        checkInService.processCheckIn(requestDTO, auth(user));

        assertEquals(ReservationLiveStatus.CHECKED_IN, reservation1.getLiveStatus());
        assertEquals(ReservationLiveStatus.CHECKED_IN, reservation2.getLiveStatus());

        verify(reservationRepository, times(1)).persistAll(anyList());
    }

    @Test
    void testProcessCheckInByIds_successfulCancel() throws CheckInException {
        UUID reservationId1 = id(1);
        UUID userId = id(1);
        UUID eventId = id(10);

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat seat = new Seat("", "", location);
        seat.id = id(1);

        Reservation reservation1 = new Reservation();
        reservation1.id = reservationId1;
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        when(reservationRepository.findAllByIdUserIdAndEventId(
                        Collections.singletonList(reservationId1), userId, eventId))
                .thenReturn(Collections.singletonList(reservation1));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.emptyList(),
                        Collections.singletonList(reservationId1));

        checkInService.processCheckIn(requestDTO, auth(user));

        assertEquals(ReservationLiveStatus.CANCELLED, reservation1.getLiveStatus());

        verify(reservationRepository, times(1)).persistAll(anyList());
    }

    @Test
    void testProcessCheckInByIds_mixedCheckInAndCancel() throws CheckInException {
        UUID checkInId = id(1);
        UUID cancelId = id(2);
        UUID userId = id(1);
        UUID eventId = id(10);

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat seat = new Seat("", "", location);
        seat.id = id(1);

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

        when(reservationRepository.findAllByIdUserIdAndEventId(
                        Collections.singletonList(checkInId), userId, eventId))
                .thenReturn(Collections.singletonList(checkInReservation));
        when(reservationRepository.findAllByIdUserIdAndEventId(
                        Collections.singletonList(cancelId), userId, eventId))
                .thenReturn(Collections.singletonList(cancelReservation));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.singletonList(checkInId),
                        Collections.singletonList(cancelId));

        checkInService.processCheckIn(requestDTO, auth(user));

        assertEquals(ReservationLiveStatus.CHECKED_IN, checkInReservation.getLiveStatus());
        assertEquals(ReservationLiveStatus.CANCELLED, cancelReservation.getLiveStatus());

        verify(reservationRepository, times(2)).persistAll(anyList());
    }

    @Test
    void testProcessCheckInByIds_reservationNotFound() {
        UUID reservationId = id(999);
        UUID userId = id(1);
        UUID eventId = id(10);

        when(reservationRepository.findAllByIdUserIdAndEventId(
                        Collections.singletonList(reservationId), userId, eventId))
                .thenReturn(Collections.emptyList());

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
        UUID userId = id(1);
        UUID eventId = id(10);
        User user = new User();
        user.id = userId;

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId, userId, Collections.emptyList(), Collections.emptyList());

        checkInService.processCheckIn(requestDTO, auth(user));

        verify(reservationRepository, never()).persist(any(Reservation.class));
    }

    @Test
    void testProcessCheckInByIds_beforeDeadline_throwsBookingDeadlineNotPassedException() {
        UUID userId = id(1);
        UUID eventId = id(10);
        User user = new User();
        user.id = userId;

        Event event = new Event();
        event.id = eventId;
        event.setBookingDeadline(Instant.now().plusSeconds(3600));
        when(eventRepository.findById(eventId)).thenReturn(event);

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId, userId, Collections.singletonList(id(1)), Collections.emptyList());

        assertThrows(
                BookingDeadlineNotPassedException.class,
                () -> checkInService.processCheckIn(requestDTO, auth(user)));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void testProcessCheckInByIds_nullDeadline_throwsBookingDeadlineNotPassedException() {
        UUID userId = id(1);
        UUID eventId = id(10);
        User user = new User();
        user.id = userId;

        Event event = new Event();
        event.id = eventId;
        event.setBookingDeadline(null);
        when(eventRepository.findById(eventId)).thenReturn(event);

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId, userId, Collections.singletonList(id(1)), Collections.emptyList());

        assertThrows(
                BookingDeadlineNotPassedException.class,
                () -> checkInService.processCheckIn(requestDTO, auth(user)));
    }

    @Test
    void testGetReservationInfos_successfulCheckIn()
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        UUID userId = id(1);
        UUID eventId = id(10);
        String tokenStr = "token1";

        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;

        CheckInToken token = new CheckInToken(user, event, tokenStr);

        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat seat = new Seat("", "", location);
        seat.id = id(1);

        Reservation reservation1 = new Reservation();
        reservation1.setCheckInToken(token);
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setStatus(ReservationStatus.RESERVED);

        Reservation reservation2 = new Reservation();
        reservation2.setCheckInToken(token);
        reservation2.setUser(user);
        reservation2.setEvent(event);
        reservation2.setSeat(seat);
        reservation2.setStatus(ReservationStatus.RESERVED);

        when(checkInTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));
        when(reservationRepository.findByCheckInToken(token))
                .thenReturn(Arrays.asList(reservation1, reservation2));
        when(userRepository.findById(userId)).thenReturn(user);

        CheckInInfoResponseDTO result =
                checkInService.getReservationInfos(auth(user), userId, eventId, tokenStr);

        assertNotNull(result);
        assertEquals(2, result.reservations().size());
    }

    @Test
    void testGetReservationInfos_emptyToken()
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        UUID userId = id(1);
        UUID eventId = id(10);
        User user = new User();
        user.id = userId;
        when(userRepository.findById(userId)).thenReturn(user);

        CheckInInfoResponseDTO result =
                checkInService.getReservationInfos(auth(user), userId, eventId, "");

        assertNotNull(result);
        assertEquals(0, result.reservations().size());
    }

    @Test
    void testProcessCheckInByTokens_successfulCancel() throws CheckInException {
        UUID reservationId = id(1);

        User user = new User();
        user.id = id(1);
        Event event = new Event();
        event.id = id(10);

        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat seat = new Seat("", "", location);
        seat.id = id(1);

        Reservation reservation1 = new Reservation();
        reservation1.id = reservationId;
        reservation1.setUser(user);
        reservation1.setEvent(event);
        reservation1.setSeat(seat);
        reservation1.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        UUID userId = id(1);
        UUID eventId = id(10);

        when(reservationRepository.findAllByIdUserIdAndEventId(
                        Collections.singletonList(reservationId), userId, eventId))
                .thenReturn(Collections.singletonList(reservation1));

        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        eventId,
                        userId,
                        Collections.emptyList(),
                        Collections.singletonList(reservationId));

        checkInService.processCheckIn(requestDTO, auth(user));

        assertEquals(ReservationLiveStatus.CANCELLED, reservation1.getLiveStatus());

        verify(reservationRepository, times(1)).persistAll(anyList());
    }

    @Test
    void testGetReservationInfos_userMismatchException() {
        UUID userId = id(1);
        UUID eventId = id(10);
        String tokenStr = "token1";

        User otherUser = new User();
        otherUser.id = id(2);
        User user = new User();
        user.id = userId;
        Event event = new Event();
        event.id = eventId;
        CheckInToken token = new CheckInToken(otherUser, event, tokenStr);

        when(checkInTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(
                UserMismatchException.class,
                () -> checkInService.getReservationInfos(auth(user), userId, eventId, tokenStr));
    }

    @Test
    void testGetReservationInfos_eventMismatchException() {
        UUID userId = id(1);
        UUID eventId = id(10);
        String tokenStr = "token1";

        User user = new User();
        user.id = userId;
        Event otherEvent = new Event();
        otherEvent.id = id(11);
        CheckInToken token = new CheckInToken(user, otherEvent, tokenStr);

        when(checkInTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(
                EventMismatchException.class,
                () -> checkInService.getReservationInfos(userId, eventId, tokenStr));
    }

    @Test
    void testGetReservationInfos_tokenNotFound() {
        UUID userId = id(1);
        UUID eventId = id(10);
        String tokenStr = "token1";

        when(checkInTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        assertThrows(
                CheckInTokenNotFoundException.class,
                () -> checkInService.getReservationInfos(userId, eventId, tokenStr));
    }

    @Test
    void testGetUsernamesWithReservations_SupervisorUnauthorized_Throws() {
        UUID eventId = id(20);
        User user = new User();
        user.id = id(1);
        user.setRoles(Set.of(Roles.SUPERVISOR));
        when(eventRepository.isUserSupervisor(eq(eventId), eq(id(1)))).thenReturn(false);
        assertThrows(
                AccessDeniedException.class,
                () -> checkInService.getUsernamesWithReservations(auth(user), eventId));
    }

    @Test
    void testGetUsernamesWithReservations_AdminAllowed() {
        UUID eventId = id(10);
        User admin = new User();
        admin.id = id(2);
        admin.setRoles(Set.of(Roles.ADMIN));

        when(reservationRepository.findDistinctUsernamesByEventId(eventId))
                .thenReturn(List.of("user1"));

        List<String> usernames = checkInService.getUsernamesWithReservations(auth(admin), eventId);
        assertEquals(1, usernames.size());
        assertEquals("user1", usernames.get(0));
    }

    @Test
    void testGetAllEventsForSupervisor_filtersProperly() {
        User supervisor = new User();
        supervisor.id = id(1);
        Event e1 = new Event();
        e1.id = id(10);
        Event e2 = new Event();
        e2.id = id(20);
        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<Event> eq =
                (io.quarkus.hibernate.orm.panache.PanacheQuery<Event>)
                        mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eq.stream()).thenReturn(Stream.of(e1, e2));
        when(eq.list()).thenReturn(List.of(e1, e2));
        when(eventRepository.findAll()).thenReturn(eq);
        when(userRepository.getReference(supervisor.id)).thenReturn(supervisor);
        when(eventRepository.findAuthorizedEvents(supervisor)).thenReturn(List.of(e1));

        List<de.felixhertweck.seatreservation.supervisor.dto.SupervisorEventResponseDTO> events =
                checkInService.getAllEventsForSupervisor(auth(supervisor));
        assertEquals(1, events.size());
    }
}
