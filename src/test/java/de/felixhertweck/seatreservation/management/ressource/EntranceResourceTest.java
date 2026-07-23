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

import de.felixhertweck.seatreservation.management.dto.EntranceRequestDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
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
public class EntranceResourceTest {

    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject EmailSeatMapTokenRepository emailSeatMapTokenRepository;
    @Inject UserRepository userRepository;

    private EventLocation testLocation;
    private EventLocationEntrance testEntrance;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        cleanUpDatabase();

        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();

        testLocation = new EventLocation();
        testLocation.setName("Test Location for Entrance Test");
        testLocation.setAddress("123 Test Street");
        testLocation.setManager(manager);
        eventLocationRepository.persist(testLocation);

        testEntrance = new EventLocationEntrance("A");
        testEntrance.setEventLocation(testLocation);
        entranceRepository.persist(testEntrance);
    }

    @AfterEach
    @Transactional
    @SuppressWarnings("unused")
    void tearDown() {
        cleanUpDatabase();
    }

    // Deletes in FK-safe order; this database is shared across all @QuarkusTest classes in the
    // run, so leftover rows from another test class' locations must be cleared too.
    private void cleanUpDatabase() {
        emailSeatMapTokenRepository.deleteAll();
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        entranceRepository.deleteAll();
        areaRepository.deleteAll();
        eventLocationRepository.deleteAll();
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
    void testGetEntrancesByEventLocation() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/entrances")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("A"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetEntrancesByEventLocationMissingParam() {
        given().when().get("/api/manager/entrances").then().statusCode(400);
    }

    @Test
    void testGetEntrancesByEventLocationUnauthorized() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/entrances")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetEntrancesByEventLocationForbidden() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/entrances")
                .then()
                .statusCode(403);
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
    void testGetEntranceById() {
        given().when()
                .get("/api/manager/entrances/" + testEntrance.id)
                .then()
                .statusCode(200)
                .body("name", is("A"));
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
    void testGetEntranceByIdNotFound() {
        given().when().get("/api/manager/entrances/" + id(999)).then().statusCode(404);
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
    void testCreateEntrance() {
        given().contentType("application/json")
                .body(new EntranceRequestDTO(testLocation.id, "B"))
                .when()
                .post("/api/manager/entrances")
                .then()
                .statusCode(200)
                .body("name", is("B"));
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
    void testCreateEntranceInvalidData() {
        given().contentType("application/json")
                .body("{\"name\":\"\"}")
                .when()
                .post("/api/manager/entrances")
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
    void testUpdateEntrance() {
        given().contentType("application/json")
                .body(new EntranceRequestDTO(testLocation.id, "C"))
                .when()
                .put("/api/manager/entrances/" + testEntrance.id)
                .then()
                .statusCode(200)
                .body("name", is("C"));
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
    void testUpdateEntranceNotFound() {
        given().contentType("application/json")
                .body(new EntranceRequestDTO(testLocation.id, "C"))
                .when()
                .put("/api/manager/entrances/" + id(999))
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
    void testDeleteEntrance() {
        given().when()
                .queryParam("ids", testEntrance.id)
                .delete("/api/manager/entrances")
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
    void testDeleteEntranceNotFound() {
        given().when()
                .queryParam("ids", id(999).toString())
                .delete("/api/manager/entrances")
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
    void testDeleteEntranceConflictWhenReferencedBySeat() {
        Seat seat = new Seat("A1", "Row 1", testLocation);
        seat.setEntrance(testEntrance);
        persistSeat(seat);

        given().when()
                .queryParam("ids", testEntrance.id)
                .delete("/api/manager/entrances")
                .then()
                .statusCode(409);
    }

    @Transactional
    void persistSeat(Seat seat) {
        seatRepository.persist(seat);
    }
}
