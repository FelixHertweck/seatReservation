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

import java.util.ArrayList;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
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

    @Inject EventLocationAreaRepository eventLocationAreaRepository;

    @Inject EventLocationEntranceRepository eventLocationEntranceRepository;

    @Inject EventLocationMarkerRepository eventLocationMarkerRepository;

    @Inject EventRepository eventRepository;

    @Inject SeatRepository seatRepository;

    @Inject UserRepository userRepository;

    @Inject EmailSeatMapTokenRepository emailSeatMapTokenRepository;

    private EventLocation testLocation;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        // Clean up any leftover data from previous tests
        emailSeatMapTokenRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationAreaRepository.deleteAll();
        eventLocationEntranceRepository.deleteAll();
        eventLocationMarkerRepository.deleteAll();
        eventLocationRepository.deleteAll();

        var user = userRepository.findByUsernameOptional("manager").orElseThrow();
        testLocation = new EventLocation();
        testLocation.setName("Test Location");
        testLocation.setAddress("Test Address");
        testLocation.setManager(user);
        testLocation.setSeats(new ArrayList<>()); // Initialize seats list
        eventLocationRepository.persist(testLocation);
    }

    @AfterEach
    @Transactional
    @SuppressWarnings("unused")
    void tearDown() {
        emailSeatMapTokenRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationAreaRepository.deleteAll();
        eventLocationEntranceRepository.deleteAll();
        eventLocationMarkerRepository.deleteAll();
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

    /**
     * The embedded area/marker scaffolds carry no eventLocationId on create — the enclosing
     * location does not exist yet. A @NotNull on that field would reject this payload at the
     * validation layer, so this test pins the create path down through bean validation.
     *
     * <p>Note that {@code areas} in the response is derived from the seats (see {@link
     * de.felixhertweck.seatreservation.common.dto.AreaDTO#fromAreas}), so a freshly created area
     * without seats is intentionally not echoed back here.
     */
    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEventLocationWithEmbeddedAreasAndMarkers() {
        given().contentType("application/json")
                .body(
                        "{\"name\":\"Hall With Areas\",\"address\":\"123 Main"
                            + " St\",\"capacity\":100,"
                            + "\"areas\":[{\"name\":\"Parkett\",\"boundary\":[{\"xCoordinate\":1,\"yCoordinate\":1},"
                            + "{\"xCoordinate\":5,\"yCoordinate\":1}]}],"
                            + "\"markers\":[{\"label\":\"Bühne\",\"coordinate\":{\"xCoordinate\":3,\"yCoordinate\":9}}]}")
                .when()
                .post("/api/manager/eventlocations")
                .then()
                .statusCode(200)
                .body("name", is("Hall With Areas"))
                .body("markers[0].label", is("Bühne"));
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
                .queryParam("ids", testLocation.getId())
                .delete("/api/manager/eventlocations")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteEventLocationNotFound() {
        given().when()
                .queryParam("ids", 999L)
                .delete("/api/manager/eventlocations")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleEventLocations() {
        // Create additional locations for bulk delete test
        var user = userRepository.findByUsernameOptional("manager").orElseThrow();
        var location2 = new EventLocation();
        location2.setName("Test Location 2");
        location2.setAddress("Test Address 2");
        location2.setManager(user);
        location2.setSeats(new ArrayList<>());

        var location3 = new EventLocation();
        location3.setName("Test Location 3");
        location3.setAddress("Test Address 3");
        location3.setManager(user);
        location3.setSeats(new ArrayList<>());

        seedAdditionalLocations(location2, location3);

        // Delete multiple locations
        given().when()
                .queryParam("ids", testLocation.getId())
                .queryParam("ids", location2.getId())
                .queryParam("ids", location3.getId())
                .delete("/api/manager/eventlocations")
                .then()
                .statusCode(204);

        // Verify all were deleted
        given().when()
                .get("/api/manager/eventlocations")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleEventLocations_PartialNotFound() {
        given().when()
                .queryParam("ids", testLocation.getId())
                .queryParam("ids", 999L) // Non-existent ID
                .delete("/api/manager/eventlocations")
                .then()
                .statusCode(404); // Should fail if any ID is not found
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testDeleteEventLocation_Forbidden() {
        given().when()
                .queryParam("ids", testLocation.getId())
                .delete("/api/manager/eventlocations")
                .then()
                .statusCode(403);
    }

    @Test
    void testDeleteEventLocation_Unauthorized() {
        given().when()
                .queryParam("ids", testLocation.getId())
                .delete("/api/manager/eventlocations")
                .then()
                .statusCode(401);
    }

    @Transactional
    void seedAdditionalLocations(EventLocation location2, EventLocation location3) {
        eventLocationRepository.persist(location2);
        eventLocationRepository.persist(location3);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEventLocationWithSeats() {
        String requestBody =
                "{\"name\":\"New Location\",\"address\":\"123 Main"
                    + " St\",\"capacity\":100,\"seats\":[{\"seatNumber\":\"A1\",\"coordinate\":{\"xCoordinate\":1,\"yCoordinate\":1}},{\"seatNumber\":\"A2\",\"coordinate\":{\"xCoordinate\":1,\"yCoordinate\":2}}]}";

        given().contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/manager/eventlocations")
                .then()
                .statusCode(200)
                .body("name", is("New Location"))
                .body("seatIds.size()", is(2));
    }
}
