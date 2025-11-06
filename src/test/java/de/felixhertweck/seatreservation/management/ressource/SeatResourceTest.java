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

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SeatResourceTest {

    @Inject SeatRepository seatRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    private Seat testSeat;
    private EventLocation testLocation;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();

        testLocation = new EventLocation();
        testLocation.setName("Test Location for Seat Test");
        testLocation.setAddress("123 Test Street");
        testLocation.setManager(manager);
        eventLocationRepository.persist(testLocation);

        var testEvent = new Event();
        testEvent.setName("Test Event for Seat Test");
        testEvent.setEventLocation(testLocation);
        eventRepository.persist(testEvent);

        var testAllowance = new EventUserAllowance(manager, testEvent, 100);
        eventUserAllowanceRepository.persist(testAllowance);

        testSeat = new Seat();
        testSeat.setSeatNumber("A1");
        testSeat.setSeatRow("R: 1");
        testSeat.setLocation(testLocation);
        seatRepository.persist(testSeat);
    }

    @AfterEach
    @Transactional
    @SuppressWarnings("unused")
    void tearDown() {
        // It's safer to delete all created entities to clean up the state
        seatRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetAllManagerSeatsForbidden() {
        given().when().get("/api/manager/seats").then().statusCode(403);
    }

    @Test
    void testGetAllManagerSeatsUnauthorized() {
        given().when().get("/api/manager/seats").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAllManagerSeats() {
        given().when().get("/api/manager/seats").then().statusCode(200).body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetManagerSeatById() {
        given().when()
                .get("/api/manager/seats/" + testSeat.id)
                .then()
                .statusCode(200)
                .body("id", is(testSeat.id.intValue()));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetManagerSeatByIdNotFound() {
        given().when().get("/api/manager/seats/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateSeat() {
        given().contentType("application/json")
                .body(new SeatRequestDTO("A2", "R: 2", testLocation.id, 1, 2))
                .when()
                .post("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("seatNumber", is("A2"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateSeatInvalidData() {
        given().contentType("application/json")
                .body("{\"seatNumber\":\"\"}")
                .when()
                .post("/api/manager/seats")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateManagerSeat() {
        given().contentType("application/json")
                .body(new SeatRequestDTO("A3", "R: 2", testLocation.id, 1, 3))
                .when()
                .put("/api/manager/seats/" + testSeat.id)
                .then()
                .statusCode(200)
                .body("seatNumber", is("A3"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateManagerSeatNotFound() {
        given().contentType("application/json")
                .body(new SeatRequestDTO("A3", "R: 2", testLocation.id, 1, 3))
                .when()
                .put("/api/manager/seats/999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteManagerSeat() {
        given().when()
                .queryParam("ids", testSeat.id)
                .delete("/api/manager/seats")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteManagerSeatNotFound() {
        given().when().queryParam("ids", 999L).delete("/api/manager/seats").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleSeats() {
        // Create additional seats for bulk delete test
        var seat2 = new Seat();
        seat2.setSeatNumber("A2");
        seat2.setSeatRow("R: 1");
        seat2.setLocation(testLocation);

        var seat3 = new Seat();
        seat3.setSeatNumber("A3");
        seat3.setSeatRow("R: 1");
        seat3.setLocation(testLocation);

        seedAdditionalSeats(seat2, seat3);

        // Delete multiple seats
        given().when()
                .queryParam("ids", testSeat.id)
                .queryParam("ids", seat2.id)
                .queryParam("ids", seat3.id)
                .delete("/api/manager/seats")
                .then()
                .statusCode(204);

        // Verify all were deleted
        given().when().get("/api/manager/seats").then().statusCode(200).body("size()", is(0));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleSeats_PartialNotFound() {
        given().when()
                .queryParam("ids", testSeat.id)
                .queryParam("ids", 999L)
                .delete("/api/manager/seats")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteSeat_Forbidden() {
        given().when()
                .queryParam("ids", testSeat.id)
                .delete("/api/manager/seats")
                .then()
                .statusCode(403);
    }

    @Test
    void testDeleteSeat_Unauthorized() {
        given().when()
                .queryParam("ids", testSeat.id)
                .delete("/api/manager/seats")
                .then()
                .statusCode(401);
    }

    @Transactional
    void seedAdditionalSeats(Seat seat2, Seat seat3) {
        seatRepository.persist(seat2);
        seatRepository.persist(seat3);
    }
}
