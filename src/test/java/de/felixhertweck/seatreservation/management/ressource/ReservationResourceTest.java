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
package de.felixhertweck.seatreservation.management.ressource;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ReservationResourceTest {

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject SeatRepository seatRepository;
    @Inject UserRepository userRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    private Reservation testReservation;
    private Event testEvent;
    private Seat testSeat;
    private Seat anotherSeat;
    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        testUser = userRepository.findByUsernameOptional("user").orElseThrow();

        var testLocation = new EventLocation();
        testLocation.setName("Test Location");
        testLocation.setAddress("Test Address");
        testLocation.setManager(manager);
        eventLocationRepository.persist(testLocation);

        testEvent = new Event();
        testEvent.setName("Test Event");
        testEvent.setStartTime(Instant.now().plusSeconds(2 * 24 * 60 * 60));
        testEvent.setEndTime(Instant.now().plusSeconds(2 * 24 * 60 * 60 + 2 * 60 * 60));
        testEvent.setEventLocation(testLocation);
        testEvent.setManager(manager);
        eventRepository.persist(testEvent);

        testSeat = new Seat();
        testSeat.setSeatNumber("A1");
        testSeat.setLocation(testLocation);
        seatRepository.persist(testSeat);

        anotherSeat = new Seat();
        anotherSeat.setSeatNumber("A2");
        anotherSeat.setLocation(testLocation);
        seatRepository.persist(anotherSeat);

        testReservation = new Reservation();
        testReservation.setEvent(testEvent);
        testReservation.setSeat(testSeat);
        testReservation.setUser(testUser);
        reservationRepository.persist(testReservation);

        var allowance = new EventUserAllowance(testUser, testEvent, 1);
        eventUserAllowanceRepository.persist(allowance);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetAllReservationsForbidden() {
        given().when().get("/api/manager/reservations").then().statusCode(403);
    }

    @Test
    void testGetAllReservationsUnauthorized() {
        given().when().get("/api/manager/reservations").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAllReservations() {
        given().when()
                .get("/api/manager/reservations")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetReservationById() {
        given().when()
                .get("/api/manager/reservations/" + testReservation.id)
                .then()
                .statusCode(200)
                .body("id", is(testReservation.id.intValue()));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetReservationByIdNotFound() {
        given().when().get("/api/manager/reservations/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateReservation() {
        given().contentType("application/json")
                .body(
                        Map.of(
                                "eventId",
                                testEvent.id,
                                "seatIds",
                                Set.of(anotherSeat.id),
                                "userId",
                                testUser.id,
                                "deductAllowance",
                                false))
                .when()
                .post("/api/manager/reservations")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateReservationInvalidData() {
        given().contentType("application/json")
                .body("{\"eventId\":999}")
                .when()
                .post("/api/manager/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteReservation() {
        given().when()
                .queryParam("ids", testReservation.id)
                .delete("/api/manager/reservations")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteReservationNotFound() {
        given().when()
                .queryParam("ids", 999L)
                .delete("/api/manager/reservations")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleReservations() {
        // Create additional reservations for bulk delete test
        var reservation2 = new Reservation();
        reservation2.setEvent(testEvent);
        reservation2.setSeat(anotherSeat);
        reservation2.setUser(testUser);

        var user2 = userRepository.findByUsernameOptional("user").orElseThrow();
        var seat3 = new Seat();
        seat3.setSeatNumber("A3");
        seat3.setLocation(testSeat.getLocation());

        var reservation3 = new Reservation();
        reservation3.setEvent(testEvent);
        reservation3.setSeat(seat3);
        reservation3.setUser(user2);

        seedAdditionalReservations(seat3, reservation2, reservation3);

        // Delete multiple reservations
        given().when()
                .queryParam("ids", testReservation.id)
                .queryParam("ids", reservation2.id)
                .queryParam("ids", reservation3.id)
                .delete("/api/manager/reservations")
                .then()
                .statusCode(204);

        // Verify all were deleted
        given().when()
                .get("/api/manager/reservations")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleReservations_PartialNotFound() {
        given().when()
                .queryParam("ids", testReservation.id)
                .queryParam("ids", 999L)
                .delete("/api/manager/reservations")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteReservation_Forbidden() {
        given().when()
                .queryParam("ids", testReservation.id)
                .delete("/api/manager/reservations")
                .then()
                .statusCode(403);
    }

    @Test
    void testDeleteReservation_Unauthorized() {
        given().when()
                .queryParam("ids", testReservation.id)
                .delete("/api/manager/reservations")
                .then()
                .statusCode(401);
    }

    @Transactional
    void seedAdditionalReservations(
            Seat seat3, Reservation reservation2, Reservation reservation3) {
        seatRepository.persist(seat3);
        reservationRepository.persist(reservation2);
        reservationRepository.persist(reservation3);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetReservationsByEventId() {
        given().when()
                .get("/api/manager/reservations/event/" + testEvent.id)
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetReservationsByEventIdNotFound() {
        given().when().get("/api/manager/reservations/event/999").then().statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetReservationsByEventIdForbidden() {
        given().when()
                .get("/api/manager/reservations/event/" + testEvent.id)
                .then()
                .statusCode(403);
    }

    @Test
    void testGetReservationsByEventIdUnauthorized() {
        given().when()
                .get("/api/manager/reservations/event/" + testEvent.id)
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testBlockSeats() {
        given().contentType("application/json")
                .body(Map.of("eventId", testEvent.id, "seatIds", new Long[] {anotherSeat.id}))
                .when()
                .post("/api/manager/reservations/block")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testExportReservationsToPdfForbidden() {
        given().when()
                .get("/api/manager/reservations/export/" + testEvent.id + "/pdf")
                .then()
                .statusCode(403);
    }

    @Test
    void testExportReservationsToPdfUnauthorized() {
        given().when()
                .get("/api/manager/reservations/export/" + testEvent.id + "/pdf")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testExportReservationsToPdf() {
        given().when()
                .get("/api/manager/reservations/export/" + testEvent.id + "/pdf")
                .then()
                .statusCode(200)
                .contentType("application/pdf");
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testExportReservationsToPdfEventNotFound() {
        given().when().get("/api/manager/reservations/export/999/pdf").then().statusCode(404);
    }
}
