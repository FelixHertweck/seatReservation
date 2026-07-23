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

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.management.dto.MakerRequestDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
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
public class MarkerResourceTest {

    @Inject EventLocationMarkerRepository markerRepository;
    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject EmailSeatMapTokenRepository emailSeatMapTokenRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject UserRepository userRepository;

    private EventLocation testLocation;
    private EventLocationMarker testMarker;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        cleanUpDatabase();

        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();

        testLocation = new EventLocation();
        testLocation.setName("Test Location for Marker Test");
        testLocation.setAddress("123 Test Street");
        testLocation.setManager(manager);
        eventLocationRepository.persist(testLocation);

        testMarker = new EventLocationMarker("Main Entrance", 10, 20);
        testMarker.setEventLocation(testLocation);
        markerRepository.persist(testMarker);
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
        markerRepository.deleteAll();
        areaRepository.deleteAll();
        entranceRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testGetMarkersByEventLocation() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/markers")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].label", is("Main Entrance"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetMarkersByEventLocationMissingParam() {
        given().when().get("/api/manager/markers").then().statusCode(400);
    }

    @Test
    void testGetMarkersByEventLocationUnauthorized() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/markers")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetMarkersByEventLocationForbidden() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/markers")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testGetMarkerById() {
        given().when()
                .get("/api/manager/markers/" + testMarker.id)
                .then()
                .statusCode(200)
                .body("label", is("Main Entrance"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testGetMarkerByIdNotFound() {
        given().when().get("/api/manager/markers/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testCreateMarker() {
        given().contentType("application/json")
                .body(new MakerRequestDTO(testLocation.id, "Stage", new CoordinateDTO(5, 5)))
                .when()
                .post("/api/manager/markers")
                .then()
                .statusCode(200)
                .body("label", is("Stage"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testCreateMarkerInvalidData() {
        given().contentType("application/json")
                .body("{\"label\":\"\"}")
                .when()
                .post("/api/manager/markers")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testUpdateMarker() {
        given().contentType("application/json")
                .body(new MakerRequestDTO(testLocation.id, "Updated", new CoordinateDTO(1, 1)))
                .when()
                .put("/api/manager/markers/" + testMarker.id)
                .then()
                .statusCode(200)
                .body("label", is("Updated"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testUpdateMarkerNotFound() {
        given().contentType("application/json")
                .body(new MakerRequestDTO(testLocation.id, "Updated", new CoordinateDTO(1, 1)))
                .when()
                .put("/api/manager/markers/999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testDeleteMarker() {
        given().when()
                .queryParam("ids", testMarker.id)
                .delete("/api/manager/markers")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @JwtSecurity(claims = @Claim(key = "uid", value = "2", type = ClaimType.LONG))
    void testDeleteMarkerNotFound() {
        given().when()
                .queryParam("ids", 999L)
                .delete("/api/manager/markers")
                .then()
                .statusCode(404);
    }
}
