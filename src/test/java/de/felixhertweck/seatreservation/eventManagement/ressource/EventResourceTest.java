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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class EventResourceTest {

    @Inject EventRepository eventRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    @Inject UserRepository userRepository;

    private Event testEvent;
    private EventLocation testLocation;

    @BeforeEach
    @Transactional
    void setUp() {
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        userRepository
                .findByUsernameOptional("manager2")
                .orElseGet(
                        () -> {
                            var u = new User();
                            u.setUsername("manager2");
                            u.setPasswordHash("password");
                            u.setRoles(Set.of("MANAGER"));
                            u.setEmail("manager2@example.com");
                            u.setTags(Collections.emptySet());
                            userRepository.persist(u);
                            return u;
                        });

        testLocation = new EventLocation();
        testLocation.setName("Test Location");
        testLocation.setAddress("Test Address");
        testLocation.setManager(manager);
        eventLocationRepository.persist(testLocation);

        testEvent = new Event();
        testEvent.setName("Test Event");
        testEvent.setEventLocation(testLocation);
        testEvent.setManager(manager);
        eventRepository.persist(testEvent);

        var allowance = new EventUserAllowance(manager, testEvent, 5);
        eventUserAllowanceRepository.persist(allowance);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
        userRepository.delete("username", "manager2");
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetEventsByCurrentManagerForbidden() {
        given().when().get("/api/manager/events").then().statusCode(403);
    }

    @Test
    void testGetEventsByCurrentManagerUnauthorized() {
        given().when().get("/api/manager/events").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetEventsByCurrentManager() {
        given().when().get("/api/manager/events").then().statusCode(200).body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetEventByIdForManager() {
        given().when()
                .get("/api/manager/events/" + testEvent.getId())
                .then()
                .statusCode(200)
                .body("name", is(testEvent.getName()));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"ADMIN"})
    void testGetEventByIdForAdmin() {
        given().when()
                .get("/api/manager/events/" + testEvent.getId())
                .then()
                .statusCode(200)
                .body("name", is(testEvent.getName()));
    }

    @Test
    @TestSecurity(
            user = "manager2",
            roles = {"MANAGER"})
    void testGetEventByIdForOtherManagerForbidden() {
        given().when().get("/api/manager/events/" + testEvent.getId()).then().statusCode(403);
    }

    @Test
    void testGetEventByIdUnauthorized() {
        given().when().get("/api/manager/events/" + testEvent.getId()).then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEvent() {
        given().contentType("application/json")
                .body(
                        Map.of(
                                "name", "New Event",
                                "description", "Description",
                                "startTime", "2025-01-01T19:00:00",
                                "endTime", "2025-01-01T21:00:00",
                                "bookingDeadline", "2024-12-31T12:00:00",
                                "eventLocationId", testLocation.getId()))
                .when()
                .post("/api/manager/events")
                .then()
                .statusCode(200)
                .body("name", is("New Event"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testCreateEventInvalidData() {
        given().contentType("application/json")
                .body("{\"name\":\"\"}")
                .when()
                .post("/api/manager/events")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateEvent() {
        given().contentType("application/json")
                .body(
                        Map.of(
                                "name", "Updated Event",
                                "description", "Description",
                                "startTime", "2025-01-01T19:00:00",
                                "endTime", "2025-01-01T21:00:00",
                                "bookingDeadline", "2024-12-31T12:00:00",
                                "eventLocationId", testLocation.getId()))
                .when()
                .put("/api/manager/events/" + testEvent.getId())
                .then()
                .statusCode(200)
                .body("name", is("Updated Event"));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateEventInvalidData() {
        given().contentType("application/json")
                .body("{\"name\":\"\"}")
                .when()
                .put("/api/manager/events/" + testEvent.getId())
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testUpdateEventNotFound() {
        given().contentType("application/json")
                .body(
                        Map.of(
                                "name", "Updated Event",
                                "description", "Description",
                                "startTime", "2025-01-01T19:00:00",
                                "endTime", "2025-01-01T21:00:00",
                                "bookingDeadline", "2024-12-31T12:00:00",
                                "eventLocationId", testLocation.getId()))
                .when()
                .put("/api/manager/events/999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteEventAsManager() {
        given().when().delete("/api/manager/events/" + testEvent.getId()).then().statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"ADMIN"})
    void testDeleteEventAsAdmin() {
        given().when().delete("/api/manager/events/" + testEvent.getId()).then().statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager2",
            roles = {"MANAGER"})
    void testDeleteEventAsOtherManagerForbidden() {
        given().when().delete("/api/manager/events/" + testEvent.getId()).then().statusCode(403);
    }

    @Test
    void testDeleteEventUnauthorized() {
        given().when().delete("/api/manager/events/" + testEvent.getId()).then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @Transactional
    void testDeleteEventDeletesAllowances() {
        assertEquals(1, eventUserAllowanceRepository.count());

        given().when().delete("/api/manager/events/" + testEvent.getId()).then().statusCode(204);

        assertEquals(0, eventRepository.count());
        assertEquals(0, eventUserAllowanceRepository.count());
    }
}
