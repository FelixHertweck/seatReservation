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
package de.felixhertweck.seatreservation.eventManagement.ressource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class EventLocationResourceTest {

    @Inject EventLocationRepository eventLocationRepository;

    @Inject EventRepository eventRepository;

    @Inject SeatRepository seatRepository;

    @Inject UserRepository userRepository;

    private EventLocation testLocation;

    @BeforeEach
    @Transactional
    void setUp() {
        var user = userRepository.findByUsernameOptional("manager").orElseThrow();
        testLocation = new EventLocation();
        testLocation.setName("Test Location");
        testLocation.setAddress("Test Address");
        testLocation.setManager(user);
        eventLocationRepository.persist(testLocation);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetEventLocationsByCurrentManagerForbidden() {
        given().when().get("/api/manager/eventlocations").then().statusCode(403);
    }

    @Test
    void testGetEventLocationsByCurrentManagerUnauthorized() {
        given().when().get("/api/manager/eventlocations").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetEventLocationsByCurrentManager() {
        given().when()
                .get("/api/manager/eventlocations")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEventLocation() {
        given().contentType("application/json")
                .body("{\"name\":\"New Location\",\"address\":\"123 Main St\",\"capacity\":100}")
                .when()
                .post("/api/manager/eventlocations")
                .then()
                .statusCode(200)
                .body("name", is("New Location"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEventLocationInvalidData() {
        given().contentType("application/json")
                .body("{\"name\":\"\"}")
                .when()
                .post("/api/manager/eventlocations")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateEventLocation() {
        given().contentType("application/json")
                .body(
                        "{\"name\":\"Updated Location\",\"address\":\"456 Main"
                                + " St\",\"capacity\":150}")
                .when()
                .put("/api/manager/eventlocations/" + testLocation.getId())
                .then()
                .statusCode(200)
                .body("name", is("Updated Location"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateEventLocationInvalidData() {
        given().contentType("application/json")
                .body("{\"name\":\"\"}")
                .when()
                .put("/api/manager/eventlocations/" + testLocation.getId())
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateEventLocationNotFound() {
        given().contentType("application/json")
                .body(
                        "{\"name\":\"Updated Location\",\"address\":\"456 Main"
                                + " St\",\"capacity\":150}")
                .when()
                .put("/api/manager/eventlocations/999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteEventLocation() {
        given().when()
                .delete("/api/manager/eventlocations/" + testLocation.getId())
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteEventLocationNotFound() {
        given().when().delete("/api/manager/eventlocations/999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEventLocationWithSeats() {
        String requestBody =
                "{\"name\":\"New Location\",\"address\":\"123 Main"
                    + " St\",\"capacity\":100,\"seats\":[{\"seatNumber\":\"A1\",\"xCoordinate\":1,\"yCoordinate\":1},{\"seatNumber\":\"A2\",\"xCoordinate\":1,\"yCoordinate\":2}]}";

        given().contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/manager/eventlocations")
                .then()
                .statusCode(200)
                .body("name", is("New Location"))
                .body("seats.size()", is(2));
    }
}
