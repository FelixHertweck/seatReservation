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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.reservation.dto.UserReservationsRequestDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ReservationResourceTest {

    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject SeatRepository seatRepository;
    @Inject ReservationRepository reservationRepository;

    private Event testEvent;
    private Seat testSeat1;
    private Seat testSeat2;
    private Seat testSeat3;
    private Reservation testReservation;

    @BeforeEach
    @Transactional
    void setUp() {
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        var testUser = userRepository.findByUsernameOptional("user").orElseThrow();
        userRepository.findByUsernameOptional("admin").orElseThrow();

        var location = new EventLocation();
        location.setName("Test Location for Reservation Test");
        location.setManager(manager);
        eventLocationRepository.persist(location);

        testEvent = new Event();
        testEvent.setName("Test Event for Reservation");
        testEvent.setEventLocation(location);
        testEvent.setStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        testEvent.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        eventRepository.persist(testEvent);

        testSeat1 = new Seat("A1", "Row 1", location);
        testSeat2 = new Seat("A2", "Row 2", location);
        testSeat3 = new Seat("A3", "Row 3", location);
        seatRepository.persist(testSeat1);
        seatRepository.persist(testSeat2);
        seatRepository.persist(testSeat3);

        var allowance = new EventUserAllowance(testUser, testEvent, 2);
        eventUserAllowanceRepository.persist(allowance);

        testReservation =
                new Reservation(
                        testUser, testEvent, testSeat1, Instant.now(), ReservationStatus.RESERVED);
        reservationRepository.persist(testReservation);

        allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() - 1);
        eventUserAllowanceRepository.persist(allowance);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetMyReservations_Success() {
        given().when()
                .get("/api/user/reservations")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].seat.seatNumber", is("A1"));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testGetMyReservations_EmptyList() {
        given().when().get("/api/user/reservations").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void testGetMyReservations_Unauthorized() {
        given().when().get("/api/user/reservations").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetMyReservationById_Success() {
        given().when()
                .get("/api/user/reservations/" + testReservation.id)
                .then()
                .statusCode(200)
                .body("seat.seatNumber", is("A1"));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetMyReservationById_NotFound() {
        given().when().get("/api/user/reservations/9999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testGetMyReservationById_Forbidden() {
        given().when().get("/api/user/reservations/" + testReservation.id).then().statusCode(403);
    }

    @Test
    void testGetMyReservationById_Unauthorized() {
        given().when().get("/api/user/reservations/" + testReservation.id).then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testCreateReservation_Success() {
        var request = new UserReservationsRequestDTO(testEvent.id, Set.of(testSeat2.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].seat.seatNumber", is("A2"));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testCreateReservation_SeatAlreadyReserved() {
        var request = new UserReservationsRequestDTO(testEvent.id, Set.of(testSeat1.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @Transactional
    void testCreateReservation_NoAllowance() {
        var request =
                new UserReservationsRequestDTO(testEvent.id, Set.of(testSeat2.id, testSeat3.id));

        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(400); // NoSeatsAvailableException -> BAD_REQUEST
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @Transactional
    void testCreateReservation_EventNotAllowed() {
        var otherEvent = new Event();
        otherEvent.setName("Other Event");
        otherEvent.setEventLocation(testEvent.getEventLocation());
        otherEvent.setStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        otherEvent.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        eventRepository.persist(otherEvent);

        var request = new UserReservationsRequestDTO(otherEvent.id, Set.of(testSeat2.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testCreateReservation_InvalidRequest() {
        var request = new UserReservationsRequestDTO(testEvent.id, Set.of());
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateReservation_Unauthorized() {
        var request = new UserReservationsRequestDTO(testEvent.id, Set.of(testSeat2.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteReservation_Success() {
        given().when()
                .queryParam("ids", testReservation.id)
                .delete("/api/user/reservations")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testDeleteReservation_Forbidden() {
        given().when()
                .queryParam("ids", testReservation.id)
                .delete("/api/user/reservations")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteReservation_NotFound() {
        given().when()
                .queryParam("ids", 9999L)
                .delete("/api/user/reservations")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteReservation_Unauthorized() {
        given().when()
                .queryParam("ids", testReservation.id)
                .delete("/api/user/reservations")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteMultipleReservations_Success() {
        // Create additional reservations for bulk delete test
        var testUser = userRepository.findByUsernameOptional("user").orElseThrow();
        var reservation2 =
                new Reservation(
                        testUser, testEvent, testSeat2, Instant.now(), ReservationStatus.RESERVED);

        seedUserReservation(reservation2);

        // Delete multiple reservations
        given().when()
                .queryParam("ids", testReservation.id)
                .queryParam("ids", reservation2.id)
                .delete("/api/user/reservations")
                .then()
                .statusCode(204);

        // Verify all were deleted
        given().when().get("/api/user/reservations").then().statusCode(200).body("size()", is(0));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteMultipleReservations_PartialNotFound() {
        given().when()
                .queryParam("ids", testReservation.id)
                .queryParam("ids", 9999L)
                .delete("/api/user/reservations")
                .then()
                .statusCode(404);
    }

    @Transactional
    void seedUserReservation(Reservation reservation) {
        reservationRepository.persist(reservation);
    }
}
