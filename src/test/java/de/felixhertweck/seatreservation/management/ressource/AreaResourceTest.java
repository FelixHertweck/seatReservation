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

import de.felixhertweck.seatreservation.management.dto.AreaRequestDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AreaResourceTest {

    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject EmailSeatMapTokenRepository emailSeatMapTokenRepository;
    @Inject UserRepository userRepository;

    private EventLocation testLocation;
    private EventLocationArea testArea;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        cleanUpDatabase();

        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();

        testLocation = new EventLocation();
        testLocation.setName("Test Location for Area Test");
        testLocation.setAddress("123 Test Street");
        testLocation.setManager(manager);
        eventLocationRepository.persist(testLocation);

        testArea = new EventLocationArea("Parkett");
        testArea.setEventLocation(testLocation);
        areaRepository.persist(testArea);
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
        areaRepository.deleteAll();
        entranceRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAreasByEventLocation() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/areas")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Parkett"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAreasByEventLocationMissingParam() {
        given().when().get("/api/manager/areas").then().statusCode(400);
    }

    @Test
    void testGetAreasByEventLocationUnauthorized() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/areas")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetAreasByEventLocationForbidden() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/areas")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAreaById() {
        given().when()
                .get("/api/manager/areas/" + testArea.id)
                .then()
                .statusCode(200)
                .body("name", is("Parkett"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAreaByIdNotFound() {
        given().when().get("/api/manager/areas/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateArea() {
        given().contentType("application/json")
                .body(new AreaRequestDTO(testLocation.id, "Balkon", null))
                .when()
                .post("/api/manager/areas")
                .then()
                .statusCode(200)
                .body("name", is("Balkon"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateAreaInvalidData() {
        given().contentType("application/json")
                .body("{\"name\":\"\"}")
                .when()
                .post("/api/manager/areas")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateArea() {
        given().contentType("application/json")
                .body(new AreaRequestDTO(testLocation.id, "Loge", null))
                .when()
                .put("/api/manager/areas/" + testArea.id)
                .then()
                .statusCode(200)
                .body("name", is("Loge"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateAreaNotFound() {
        given().contentType("application/json")
                .body(new AreaRequestDTO(testLocation.id, "Loge", null))
                .when()
                .put("/api/manager/areas/999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteArea() {
        given().when()
                .queryParam("ids", testArea.id)
                .delete("/api/manager/areas")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteAreaNotFound() {
        given().when().queryParam("ids", 999L).delete("/api/manager/areas").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteAreaConflictWhenReferencedBySeat() {
        Seat seat = new Seat("A1", "Row 1", testLocation);
        seat.setArea(testArea);
        persistSeat(seat);

        given().when()
                .queryParam("ids", testArea.id)
                .delete("/api/manager/areas")
                .then()
                .statusCode(409);
    }

    @Transactional
    void persistSeat(Seat seat) {
        seatRepository.persist(seat);
    }
}
