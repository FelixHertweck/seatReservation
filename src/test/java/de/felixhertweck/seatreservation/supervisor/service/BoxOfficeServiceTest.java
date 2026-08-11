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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.email.service.EmailService.BoxOfficeConfirmationContent;
import de.felixhertweck.seatreservation.model.entity.BoxOfficeGuestInfo;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.BoxOfficeGuestInfoRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.service.CheckInTokenService;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeGuestReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.exception.BookingDeadlineNotPassedException;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class BoxOfficeServiceTest {

    @Inject BoxOfficeService boxOfficeService;

    @InjectMock ReservationRepository reservationRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;
    @InjectMock EmailService emailService;
    @InjectMock LiveViewService liveViewService;
    @InjectMock UserService userService;

    @InjectMock CheckInTokenService checkInTokenService;

    @InjectMock BoxOfficeGuestInfoRepository boxOfficeGuestInfoRepository;

    private static final UUID EVENT_ID = id(10);
    private static final UUID SUPERVISOR_ID = id(1);
    private static final UUID TARGET_USER_ID = id(2);
    private static final UUID BOXOFFICE_USER_ID = id(5);
    private static final UUID SEAT_ID = id(1);

    private Event pastDeadlineEvent;
    private User targetUser;
    private User boxofficeUser;
    private Seat seat;

    @BeforeEach
    void setup() {
        EventLocation location = new EventLocation();
        location.id = id(1);

        seat = new Seat("1", "Row 1", location);
        seat.id = SEAT_ID;

        pastDeadlineEvent = new Event();
        pastDeadlineEvent.id = EVENT_ID;
        pastDeadlineEvent.setBookingDeadline(Instant.now().minusSeconds(3600));

        targetUser = new User();
        targetUser.id = TARGET_USER_ID;
        targetUser.setUsername("targetuser");
        targetUser.setEmail("target@example.com");

        boxofficeUser = new User();
        boxofficeUser.id = BOXOFFICE_USER_ID;
        boxofficeUser.setUsername("boxoffice");

        when(eventRepository.isUserSupervisor(EVENT_ID, SUPERVISOR_ID)).thenReturn(true);
        when(eventRepository.findById(EVENT_ID)).thenReturn(pastDeadlineEvent);

        when(seatRepository.findByIds(Set.of(SEAT_ID).stream().toList())).thenReturn(List.of(seat));

        when(reservationRepository.findByEventIdAndSeatIds(eq(EVENT_ID), anyList()))
                .thenReturn(Collections.emptyList());
        when(userRepository.findByIdOptional(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(SUPERVISOR_ID)).thenReturn(null);
        when(userRepository.findByUsername("boxoffice")).thenReturn(boxofficeUser);

        when(emailService.sendBoxOfficeConfirmation(
                        any(User.class), anyList(), any(), any(), anyBoolean()))
                .thenReturn(
                        new BoxOfficeConfirmationContent(
                                "<html>email</html>", "<html>display</html>", new byte[0]));

        when(checkInTokenService.getOrCreateForUser(any(), any()))
                .thenAnswer(
                        inv ->
                                new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                                        inv.getArgument(0), inv.getArgument(1), "TOKEN_USER"));
        when(checkInTokenService.createFresh(any(), any()))
                .thenAnswer(
                        inv ->
                                new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                                        inv.getArgument(0),
                                        inv.getArgument(1),
                                        UUID.randomUUID().toString()));
    }

    private static AuthenticatedUser supervisorAuth() {
        return new AuthenticatedUser(SUPERVISOR_ID, Set.of(Roles.SUPERVISOR));
    }

    private BoxOfficeReservationRequestDTO knownUserRequest(boolean checkedIn) {
        BoxOfficeReservationRequestDTO dto = new BoxOfficeReservationRequestDTO();
        dto.setEventId(EVENT_ID);
        dto.setUserId(TARGET_USER_ID);
        dto.setSeatIds(Set.of(SEAT_ID));
        dto.setDeductAllowance(false);
        dto.setCheckedIn(checkedIn);
        return dto;
    }

    private BoxOfficeGuestReservationRequestDTO guestRequest(String email, boolean checkedIn) {
        BoxOfficeGuestReservationRequestDTO dto = new BoxOfficeGuestReservationRequestDTO();
        dto.setEventId(EVENT_ID);
        dto.setSeatIds(Set.of(SEAT_ID));
        dto.setGuestName("Jane Doe");
        dto.setGuestEmail(email);
        dto.setCheckedIn(checkedIn);
        return dto;
    }

    // -- known user path --

    @Test
    void reserveForKnownUser_beforeDeadline_throwsBookingDeadlineNotPassedException() {
        pastDeadlineEvent.setBookingDeadline(Instant.now().plusSeconds(3600));

        assertThrows(
                BookingDeadlineNotPassedException.class,
                () ->
                        boxOfficeService.reserveForKnownUser(
                                knownUserRequest(false), supervisorAuth()));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void reserveForKnownUser_nullDeadline_throwsBookingDeadlineNotPassedException() {
        pastDeadlineEvent.setBookingDeadline(null);

        assertThrows(
                BookingDeadlineNotPassedException.class,
                () ->
                        boxOfficeService.reserveForKnownUser(
                                knownUserRequest(false), supervisorAuth()));
    }

    @Test
    void reserveForKnownUser_notAuthorizedForEvent_throwsSecurityException() {
        AuthenticatedUser unrelated = new AuthenticatedUser(id(99), Set.of(Roles.SUPERVISOR));
        when(eventRepository.isUserSupervisor(EVENT_ID, id(99))).thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> boxOfficeService.reserveForKnownUser(knownUserRequest(false), unrelated));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void reserveForKnownUser_seatAlreadyReserved_throwsIllegalStateException() {
        Reservation existing = new Reservation();
        existing.setSeat(seat);
        when(reservationRepository.findByEventIdAndSeatIds(eq(EVENT_ID), anyList()))
                .thenReturn(List.of(existing));

        assertThrows(
                IllegalStateException.class,
                () ->
                        boxOfficeService.reserveForKnownUser(
                                knownUserRequest(false), supervisorAuth()));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void reserveForKnownUser_deductAllowanceWithoutAllowance_throwsIllegalArgumentException() {
        when(eventUserAllowanceRepository.findByUserAndEvent(targetUser, pastDeadlineEvent))
                .thenReturn(Optional.empty());

        BoxOfficeReservationRequestDTO dto = knownUserRequest(false);
        dto.setDeductAllowance(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> boxOfficeService.reserveForKnownUser(dto, supervisorAuth()));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void reserveForKnownUser_checkedInTrue_setsLiveStatusCheckedIn() {
        BoxOfficeReservationResponseDTO result =
                boxOfficeService.reserveForKnownUser(knownUserRequest(true), supervisorAuth());

        assertEquals(TARGET_USER_ID, result.reservationUserId());
        assertEquals(1, result.seats().size());
        assertEquals(ReservationLiveStatus.CHECKED_IN, result.seats().getFirst().liveStatus());
        assertEquals("<html>display</html>", result.confirmationHtml());

        verify(reservationRepository, times(1)).persistAll(anyList());
        // checkedIn=true at creation time -> no QR code needed, nothing left to scan.
        verify(emailService, times(1))
                .sendBoxOfficeConfirmation(
                        eq(targetUser), anyList(), eq("null null"), any(), eq(false));
        verify(liveViewService, times(1))
                .broadcastNewReservation(eq(EVENT_ID), any(Reservation.class), isNull());
    }

    @Test
    void reserveForKnownUser_checkedInFalse_leavesLiveStatusUnset() {
        BoxOfficeReservationResponseDTO result =
                boxOfficeService.reserveForKnownUser(knownUserRequest(false), supervisorAuth());

        assertNull(result.seats().getFirst().liveStatus());
    }

    // -- guest path --

    @Test
    void reserveForGuest_boxofficeUserMissing_throwsIllegalStateException() {
        when(userRepository.findByUsername("boxoffice")).thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () ->
                        boxOfficeService.reserveForGuest(
                                guestRequest("guest@example.com", false), supervisorAuth()));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void reserveForGuest_withEmail_sendsBoxOfficeConfirmationAndDoesNotPersistEmail() {
        BoxOfficeReservationResponseDTO result =
                boxOfficeService.reserveForGuest(
                        guestRequest("guest@example.com", false), supervisorAuth());

        assertEquals(BOXOFFICE_USER_ID, result.reservationUserId());
        assertEquals("<html>display</html>", result.confirmationHtml());

        // Guest confirmation uses the dedicated box office template/method, never the normal
        // reservation-confirmation email used by management/self-service.
        verify(emailService, times(1))
                .sendBoxOfficeConfirmation(
                        eq(boxofficeUser),
                        anyList(),
                        eq("Jane Doe"),
                        eq("guest@example.com"),
                        eq(true));

        // The guest email is only ever passed to EmailService -- it must never be written onto
        // the shared boxoffice User entity or persisted anywhere else.
        assertNull(boxofficeUser.getEmail());
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void reserveForGuest_withoutEmail_stillRendersConfirmationWithoutAdditionalAddress() {
        BoxOfficeReservationResponseDTO result =
                boxOfficeService.reserveForGuest(guestRequest(null, false), supervisorAuth());

        assertEquals("<html>display</html>", result.confirmationHtml());
        verify(emailService, times(1))
                .sendBoxOfficeConfirmation(
                        eq(boxofficeUser), anyList(), eq("Jane Doe"), isNull(), eq(true));
    }

    @Test
    void reserveForGuest_checkedInTrue_setsLiveStatusCheckedInAndOmitsQrCode() {
        BoxOfficeReservationResponseDTO result =
                boxOfficeService.reserveForGuest(guestRequest(null, true), supervisorAuth());

        assertEquals(ReservationLiveStatus.CHECKED_IN, result.seats().getFirst().liveStatus());
        verify(emailService, times(1))
                .sendBoxOfficeConfirmation(
                        eq(boxofficeUser), anyList(), eq("Jane Doe"), isNull(), eq(false));
    }

    @Test
    void reserveForGuest_persistsGuestNameLinkedToEachReservation() {
        boxOfficeService.reserveForGuest(guestRequest(null, false), supervisorAuth());

        ArgumentCaptor<BoxOfficeGuestInfo> captor =
                ArgumentCaptor.forClass(BoxOfficeGuestInfo.class);
        verify(boxOfficeGuestInfoRepository, times(1)).persist(captor.capture());
        assertEquals("Jane Doe", captor.getValue().getGuestName());
    }

    @Test
    void reserveForGuest_broadcastsNewReservationWithGuestName() {
        boxOfficeService.reserveForGuest(guestRequest(null, false), supervisorAuth());

        verify(liveViewService, times(1))
                .broadcastNewReservation(eq(EVENT_ID), any(Reservation.class), eq("Jane Doe"));
    }

    @Test
    void reserveForGuest_seatAlreadyReserved_throwsIllegalStateException() {
        Reservation existing = new Reservation();
        existing.setSeat(seat);
        when(reservationRepository.findByEventIdAndSeatIds(eq(EVENT_ID), anyList()))
                .thenReturn(List.of(existing));

        assertThrows(
                IllegalStateException.class,
                () ->
                        boxOfficeService.reserveForGuest(
                                guestRequest(null, false), supervisorAuth()));
        verify(reservationRepository, never()).persistAll(anyList());
    }

    @Test
    void reserveForGuest_multipleGuests_receiveDistinctTokens() {
        de.felixhertweck.seatreservation.model.entity.CheckInToken tokenGuest1 =
                new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                        boxofficeUser, pastDeadlineEvent, "TOKEN_GUEST_1");
        de.felixhertweck.seatreservation.model.entity.CheckInToken tokenGuest2 =
                new de.felixhertweck.seatreservation.model.entity.CheckInToken(
                        boxofficeUser, pastDeadlineEvent, "TOKEN_GUEST_2");

        when(checkInTokenService.createFresh(eq(boxofficeUser), eq(pastDeadlineEvent)))
                .thenReturn(tokenGuest1)
                .thenReturn(tokenGuest2);

        BoxOfficeReservationResponseDTO result1 =
                boxOfficeService.reserveForGuest(guestRequest(null, false), supervisorAuth());
        BoxOfficeReservationResponseDTO result2 =
                boxOfficeService.reserveForGuest(guestRequest(null, false), supervisorAuth());

        assertEquals("TOKEN_GUEST_1", result1.seats().getFirst().checkInToken());
        assertEquals("TOKEN_GUEST_2", result2.seats().getFirst().checkInToken());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                result1.seats().getFirst().checkInToken(),
                result2.seats().getFirst().checkInToken());
    }

    // -- user listing --

    @Test
    void getUsersForBoxOffice_excludesBoxofficeAccount() {
        LimitedUserInfoDTO real = new LimitedUserInfoDTO(TARGET_USER_ID, "targetuser", Set.of());
        LimitedUserInfoDTO boxoffice =
                new LimitedUserInfoDTO(BOXOFFICE_USER_ID, "boxoffice", Set.of());
        when(userService.getAllUsers()).thenReturn(List.of(real, boxoffice));

        List<LimitedUserInfoDTO> result = boxOfficeService.getUsersForBoxOffice();

        assertEquals(1, result.size());
        assertEquals("targetuser", result.getFirst().username());
    }
}
