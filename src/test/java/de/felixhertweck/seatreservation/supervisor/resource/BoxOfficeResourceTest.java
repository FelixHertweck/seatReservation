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
package de.felixhertweck.seatreservation.supervisor.resource;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.email.service.ReservationEmailContent.BoxOfficeConfirmationContent;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.BoxOfficeGuestInfoRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.service.CheckInTokenService;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeGuestReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.service.LiveViewService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class BoxOfficeResourceTest {

    @InjectMock UserRepository userRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock ReservationRepository reservationRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock LiveViewService liveViewService;
    @InjectMock EmailService emailService;
    @InjectMock CheckInTokenService checkInTokenService;
    @InjectMock BoxOfficeGuestInfoRepository boxOfficeGuestInfoRepository;

    private static final String SUPERVISOR_UID = "00000000-0000-0000-0000-000000000001";
    private static final String USER_UID = "00000000-0000-0000-0000-000000000006";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        User supervisorUser = new User();
        supervisorUser.id = id(1);
        supervisorUser.setUsername("testUser");
        supervisorUser.setRoles(Set.of(Roles.SUPERVISOR));
        when(userRepository.findById(id(1))).thenReturn(supervisorUser);

        User targetUser = new User();
        targetUser.id = id(2);
        targetUser.setUsername("targetuser");
        targetUser.setEmail("target@example.com");
        when(userRepository.findByIdOptional(id(2))).thenReturn(Optional.of(targetUser));

        User boxofficeUser = new User();
        boxofficeUser.id = id(5);
        boxofficeUser.setUsername("boxoffice");
        when(userRepository.findByUsername("boxoffice")).thenReturn(boxofficeUser);
        when(userRepository.listAll())
                .thenReturn(List.of(supervisorUser, targetUser, boxofficeUser));

        EventLocation location = new EventLocation();
        location.id = id(1);
        Seat seat = new Seat("1", "Row 1", location);
        seat.id = id(1);

        Event pastDeadlineEvent = new Event();
        pastDeadlineEvent.id = id(10);
        pastDeadlineEvent.setBookingDeadline(Instant.now().minusSeconds(3600));

        Event futureDeadlineEvent = new Event();
        futureDeadlineEvent.id = id(11);
        futureDeadlineEvent.setBookingDeadline(Instant.now().plusSeconds(3600));

        when(eventRepository.findById(id(10))).thenReturn(pastDeadlineEvent);
        when(eventRepository.findById(id(11))).thenReturn(futureDeadlineEvent);
        when(eventRepository.isUserSupervisor(id(10), id(1))).thenReturn(true);
        when(eventRepository.isUserSupervisor(id(11), id(1))).thenReturn(true);
        when(eventRepository.isUserSupervisor(id(10), id(6))).thenReturn(false);

        when(seatRepository.findByIds(Set.of(id(1)).stream().toList())).thenReturn(List.of(seat));

        when(reservationRepository.findByEventIdAndSeatIds(eq(id(10)), anyList()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findByEventIdAndSeatIds(eq(id(11)), anyList()))
                .thenReturn(Collections.emptyList());

        when(emailService.sendBoxOfficeConfirmation(
                        any(User.class), anyList(), any(), any(), anyBoolean()))
                .thenReturn(
                        new BoxOfficeConfirmationContent(
                                "<html>email</html>", "<html>display</html>", new byte[0]));

        when(checkInTokenService.getOrCreateForUser(any(), any()))
                .thenAnswer(
                        inv ->
                                new CheckInToken(
                                        inv.getArgument(0), inv.getArgument(1), "TOKEN_USER"));
        when(checkInTokenService.createFresh(any(), any()))
                .thenAnswer(
                        inv ->
                                new CheckInToken(
                                        inv.getArgument(0),
                                        inv.getArgument(1),
                                        UUID.randomUUID().toString()));
    }

    @Test
    @TestSecurity(user = "user", roles = Roles.USER)
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void createReservationForKnownUser_forbiddenForPlainUserRole() {
        BoxOfficeReservationRequestDTO dto = new BoxOfficeReservationRequestDTO();
        dto.setEventId(id(10));
        dto.setUserId(id(2));
        dto.setSeatIds(Set.of(id(1)));

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    @JwtSecurity(claims = @Claim(key = "uid", value = SUPERVISOR_UID, type = ClaimType.STRING))
    void createReservationForKnownUser_success() {
        BoxOfficeReservationRequestDTO dto = new BoxOfficeReservationRequestDTO();
        dto.setEventId(id(10));
        dto.setUserId(id(2));
        dto.setSeatIds(Set.of(id(1)));
        dto.setDeductAllowance(false);
        dto.setCheckedIn(true);

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations")
                .then()
                .statusCode(200)
                .body("reservationUserId", org.hamcrest.Matchers.is(id(2).toString()))
                .body("seats[0].liveStatus", org.hamcrest.Matchers.is("CHECKED_IN"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    @JwtSecurity(claims = @Claim(key = "uid", value = SUPERVISOR_UID, type = ClaimType.STRING))
    void createReservationForKnownUser_beforeDeadline_badRequest() {
        BoxOfficeReservationRequestDTO dto = new BoxOfficeReservationRequestDTO();
        dto.setEventId(id(11));
        dto.setUserId(id(2));
        dto.setSeatIds(Set.of(id(1)));

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000006",
                            type = ClaimType.STRING))
    void createReservationForKnownUser_notAuthorizedForEvent_forbidden() {
        BoxOfficeReservationRequestDTO dto = new BoxOfficeReservationRequestDTO();
        dto.setEventId(id(10));
        dto.setUserId(id(2));
        dto.setSeatIds(Set.of(id(1)));

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    @JwtSecurity(claims = @Claim(key = "uid", value = SUPERVISOR_UID, type = ClaimType.STRING))
    void createReservationForGuest_success() {
        BoxOfficeGuestReservationRequestDTO dto = new BoxOfficeGuestReservationRequestDTO();
        dto.setEventId(id(10));
        dto.setSeatIds(Set.of(id(1)));
        dto.setGuestName("Jane Doe");
        dto.setGuestEmail(null);
        dto.setCheckedIn(false);

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations/guest")
                .then()
                .statusCode(200)
                .body("reservationUserId", org.hamcrest.Matchers.is(id(5).toString()))
                .body("confirmationHtml", org.hamcrest.Matchers.is("<html>display</html>"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    @JwtSecurity(claims = @Claim(key = "uid", value = SUPERVISOR_UID, type = ClaimType.STRING))
    void createReservationForGuest_missingName_badRequest() {
        BoxOfficeGuestReservationRequestDTO dto = new BoxOfficeGuestReservationRequestDTO();
        dto.setEventId(id(10));
        dto.setSeatIds(Set.of(id(1)));
        dto.setCheckedIn(false);

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations/guest")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    @JwtSecurity(claims = @Claim(key = "uid", value = SUPERVISOR_UID, type = ClaimType.STRING))
    void createReservationForGuest_emailSurvivesSanitizationUnmangled() {
        // Regression test: guestEmail must be annotated @NoHtmlSanitize, otherwise the global
        // XssSanitizingDeserializer (registered for every String field app-wide) runs it through
        // the OWASP HTML sanitizer, which -- like every other email field in this codebase already
        // guards against -- can mangle a plain "@" in ordinary text.
        String rawEmail = "guest+box.office@example.com";
        BoxOfficeGuestReservationRequestDTO dto = new BoxOfficeGuestReservationRequestDTO();
        dto.setEventId(id(10));
        dto.setSeatIds(Set.of(id(1)));
        dto.setGuestName("Jane Doe");
        dto.setGuestEmail(rawEmail);
        dto.setCheckedIn(false);

        given().contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/supervisor/boxoffice/reservations/guest")
                .then()
                .statusCode(200);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1))
                .sendBoxOfficeConfirmation(
                        any(User.class),
                        anyList(),
                        eq("Jane Doe"),
                        emailCaptor.capture(),
                        eq(true));
        assertEquals(rawEmail, emailCaptor.getValue());
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.MANAGER)
    @JwtSecurity(claims = @Claim(key = "uid", value = SUPERVISOR_UID, type = ClaimType.STRING))
    void getUsers_allowedForManagerRole() {
        given().when().get("/api/supervisor/boxoffice/users").then().statusCode(200);
    }
}
