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
package de.felixhertweck.seatreservation.reservation.resource;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Duration;
import java.time.Instant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.service.SeatCartService;
import de.felixhertweck.seatreservation.utils.CodeGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SeatCartResourceTest {

    private static final String USER_UID = "00000000-0000-0000-0000-000000000003";

    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventLocationAreaRepository eventLocationAreaRepository;
    @Inject EventLocationEntranceRepository eventLocationEntranceRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject SeatRepository seatRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject SeatCartService seatCartService;

    private Event testEvent;
    private Event eventWithoutAllowance;
    private Seat testSeat1;
    private Seat testSeat2;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        eventLocationAreaRepository.deleteAll();
        eventLocationEntranceRepository.deleteAll();
        eventLocationRepository.deleteAll();

        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        var testUser = userRepository.findByUsernameOptional("user").orElseThrow();
        var adminUser = userRepository.findByUsernameOptional("admin").orElseThrow();

        var location = new EventLocation();
        location.setName("Test Location for Seat Cart Test");
        location.setManager(manager);
        eventLocationRepository.persist(location);

        testEvent = new Event();
        testEvent.setName("Test Event for Seat Cart");
        testEvent.setEventLocation(location);
        testEvent.setStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        testEvent.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        eventRepository.persist(testEvent);

        // Exists, but the test user has no EventUserAllowance for it - used to prove the seat
        // cart can't be used to hold seats for events the user isn't otherwise allowed to book.
        eventWithoutAllowance = new Event();
        eventWithoutAllowance.setName("Event Without Allowance");
        eventWithoutAllowance.setEventLocation(location);
        eventWithoutAllowance.setStartTime(
                Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        eventWithoutAllowance.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        eventRepository.persist(eventWithoutAllowance);

        testSeat1 = new Seat("A1", "Row 1", location);
        testSeat2 = new Seat("A2", "Row 1", location);
        seatRepository.persist(testSeat1);
        seatRepository.persist(testSeat2);

        eventUserAllowanceRepository.persist(new EventUserAllowance(testUser, testEvent, 2));
        eventUserAllowanceRepository.persist(new EventUserAllowance(adminUser, testEvent, 2));

        var reservedSeatReservation =
                new Reservation(
                        testUser,
                        testEvent,
                        testSeat1,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());
        reservationRepository.persist(reservedSeatReservation);
    }

    @AfterEach
    @Transactional
    @SuppressWarnings("unused")
    void tearDown() {
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        eventLocationAreaRepository.deleteAll();
        eventLocationEntranceRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    /**
     * Simulates the frontend's one-time fetch of the event list, which is what mints the seat-cart
     * access grant for the current user (see {@code EventService.grantAccess}).
     */
    private void fetchEventsAsUser() {
        given().when().get("/api/user/events").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testAddSeatToCart_Success() {
        fetchEventsAsUser();

        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(200)
                .body("seatId", is(testSeat2.id.toString()))
                .body("expiresAt", notNullValue());
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testAddSeatToCart_NoAccessGrant_ReturnsConflict() {
        // Never fetched /api/user/events, so no access grant was ever minted.
        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testAddSeatToCart_NoAllowanceForEvent_ReturnsConflict() {
        // Fetching events mints a grant only for events the user has an allowance for.
        fetchEventsAsUser();

        given().when()
                .post("/api/user/seatcart/" + eventWithoutAllowance.id + "/" + testSeat2.id)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testAddSeatToCart_EventNotFound_ReturnsConflict() {
        fetchEventsAsUser();

        given().when()
                .post("/api/user/seatcart/" + id(9999) + "/" + testSeat2.id)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testAddSeatToCart_SeatAlreadyReserved_ReturnsConflict() {
        fetchEventsAsUser();

        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat1.id)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    @Transactional
    void testAddSeatToCart_HeldByAnotherUser_ReturnsConflict() {
        var adminUser = userRepository.findByUsernameOptional("admin").orElseThrow();
        seatCartService.grantAccess(testEvent.id, adminUser.id);
        seatCartService.addSeatToCart(testEvent.id, testSeat2.id, adminUser.id);

        fetchEventsAsUser();

        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(409);
    }

    @Test
    void testAddSeatToCart_Unauthorized() {
        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"MANAGER"})
    void testAddSeatToCart_Forbidden_WrongRole() {
        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testRemoveSeatFromCart_Success() {
        fetchEventsAsUser();

        given().when()
                .post("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(200);

        given().when()
                .delete("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(204);

        // Released, so no one but a placeholder ID should be reported as "holding" it.
        assertFalse(seatCartService.isHeldByAnotherUser(testEvent.id, testSeat2.id, id(9999)));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = USER_UID, type = ClaimType.STRING))
    void testRemoveSeatFromCart_NotHeld_NoOp() {
        given().when()
                .delete("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(204);
    }

    @Test
    void testRemoveSeatFromCart_Unauthorized() {
        given().when()
                .delete("/api/user/seatcart/" + testEvent.id + "/" + testSeat2.id)
                .then()
                .statusCode(401);
    }
}
