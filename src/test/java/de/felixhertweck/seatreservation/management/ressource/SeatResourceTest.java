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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SeatResourceTest {

    @Inject SeatRepository seatRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventLocationAreaRepository eventLocationAreaRepository;

    @Inject EventLocationEntranceRepository eventLocationEntranceRepository;
    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;

    private Seat testSeat;
    private EventLocation testLocation;
    private EventLocationArea areaParkett;
    private EventLocationArea areaBalkon;
    private EventLocationArea areaLoge;
    private EventLocationEntrance entranceA;
    private EventLocationEntrance entranceB;
    private EventLocationEntrance entranceC;

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

        testSeat = new Seat("A1", "R: 1", testLocation);
        seatRepository.persist(testSeat);

        areaParkett = new EventLocationArea("Parkett");
        areaParkett.setEventLocation(testLocation);
        eventLocationAreaRepository.persist(areaParkett);
        areaBalkon = new EventLocationArea("Balkon");
        areaBalkon.setEventLocation(testLocation);
        eventLocationAreaRepository.persist(areaBalkon);
        areaLoge = new EventLocationArea("Loge");
        areaLoge.setEventLocation(testLocation);
        eventLocationAreaRepository.persist(areaLoge);

        entranceA = new EventLocationEntrance("A");
        entranceA.setEventLocation(testLocation);
        eventLocationEntranceRepository.persist(entranceA);
        entranceB = new EventLocationEntrance("B");
        entranceB.setEventLocation(testLocation);
        eventLocationEntranceRepository.persist(entranceB);
        entranceC = new EventLocationEntrance("C");
        entranceC.setEventLocation(testLocation);
        eventLocationEntranceRepository.persist(entranceC);
    }

    @AfterEach
    @Transactional
    @SuppressWarnings("unused")
    void tearDown() {
        // It's safer to delete all created entities to clean up the state
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationAreaRepository.deleteAll();
        eventLocationEntranceRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetAllManagerSeatsForbidden() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(403);
    }

    @Test
    void testGetAllManagerSeatsUnauthorized() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testGetAllManagerSeats() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAllManagerSeatsMissingEventLocationId() {
        given().when().get("/api/manager/seats").then().statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testGetManagerSeatById() {
        given().when()
                .get("/api/manager/seats/" + testSeat.id)
                .then()
                .statusCode(200)
                .body("id", is(testSeat.id.toString()));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testGetManagerSeatByIdNotFound() {
        given().when().get("/api/manager/seats/" + id(999)).then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testCreateSeat() {
        given().contentType("application/json")
                .body(
                        new SeatRequestDTO(
                                "A2", "R: 2", testLocation.id, 1, 2, entranceA.id, areaParkett.id))
                .when()
                .post("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("seatNumber", is("A2"))
                .body("area", is("Parkett"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
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
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testUpdateManagerSeat() {
        given().contentType("application/json")
                .body(
                        new SeatRequestDTO(
                                "A3", "R: 2", testLocation.id, 1, 3, entranceA.id, areaBalkon.id))
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
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testUpdateManagerSeatEntranceAndRowArePersisted() {
        given().contentType("application/json")
                .body(
                        new SeatRequestDTO(
                                "A1", "R: 5", testLocation.id, 1, 1, entranceB.id, areaLoge.id))
                .when()
                .put("/api/manager/seats/" + testSeat.id)
                .then()
                .statusCode(200)
                .body("entrance", is("B"))
                .body("area", is("Loge"))
                .body("seatRow", is("R: 5"));

        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body(String.format("find { it.id == '%s' }.entrance", testSeat.id), is("B"))
                .body(String.format("find { it.id == '%s' }.area", testSeat.id), is("Loge"))
                .body(String.format("find { it.id == '%s' }.seatRow", testSeat.id), is("R: 5"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testCreateUpdateAndRetrieveSeatLifecycle() {
        String createdSeatId =
                given().contentType("application/json")
                        .body(
                                new SeatRequestDTO(
                                        "B1",
                                        "R: 7",
                                        testLocation.id,
                                        5,
                                        6,
                                        entranceA.id,
                                        areaParkett.id))
                        .when()
                        .post("/api/manager/seats")
                        .then()
                        .statusCode(200)
                        .body("seatNumber", is("B1"))
                        .extract()
                        .path("id");

        given().contentType("application/json")
                .body(
                        new SeatRequestDTO(
                                "B1", "R: 9", testLocation.id, 7, 8, entranceC.id, areaBalkon.id))
                .when()
                .put("/api/manager/seats/" + createdSeatId)
                .then()
                .statusCode(200)
                .body("seatRow", is("R: 9"))
                .body("entrance", is("C"))
                .body("area", is("Balkon"));

        given().when()
                .get("/api/manager/seats/" + createdSeatId)
                .then()
                .statusCode(200)
                .body("seatRow", is("R: 9"))
                .body("entrance", is("C"));

        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body(String.format("find { it.id == '%s' }.entrance", createdSeatId), is("C"))
                .body(String.format("find { it.id == '%s' }.seatRow", createdSeatId), is("R: 9"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testUpdateManagerSeatNotFound() {
        given().contentType("application/json")
                .body(
                        new SeatRequestDTO(
                                "A3", "R: 2", testLocation.id, 1, 3, entranceA.id, areaBalkon.id))
                .when()
                .put("/api/manager/seats/" + id(999))
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
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
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testDeleteManagerSeatNotFound() {
        given().when()
                .queryParam("ids", id(999).toString())
                .delete("/api/manager/seats")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testDeleteMultipleSeats() {
        // Create additional seats for bulk delete test
        var seat2 = new Seat("A2", "R: 1", testLocation);

        var seat3 = new Seat("A3", "R: 1", testLocation);

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
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000002",
                            type = ClaimType.STRING))
    void testDeleteMultipleSeats_PartialNotFound() {
        given().when()
                .queryParam("ids", testSeat.id)
                .queryParam("ids", id(999).toString())
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
