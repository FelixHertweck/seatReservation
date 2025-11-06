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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
                                "startTime", "2025-01-01T19:00:00Z",
                                "endTime", "2025-01-01T21:00:00Z",
                                "bookingDeadline", "2024-12-31T12:00:00Z",
                                "bookingStartTime", "2024-12-30T12:00:00Z",
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
                                "startTime", "2025-01-01T19:00:00Z",
                                "endTime", "2025-01-01T21:00:00Z",
                                "bookingDeadline", "2024-12-31T12:00:00Z",
                                "bookingStartTime", "2024-12-30T12:00:00Z",
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
                                "startTime", "2025-01-01T19:00:00Z",
                                "endTime", "2025-01-01T21:00:00Z",
                                "bookingDeadline", "2024-12-31T12:00:00Z",
                                "bookingStartTime", "2024-12-30T12:00:00Z",
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
        given().when()
                .queryParam("ids", testEvent.getId())
                .delete("/api/manager/events")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"ADMIN"})
    void testDeleteEventAsAdmin() {
        given().when()
                .queryParam("ids", testEvent.getId())
                .delete("/api/manager/events")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager2",
            roles = {"MANAGER"})
    void testDeleteEventAsOtherManagerForbidden() {
        given().when()
                .queryParam("ids", testEvent.getId())
                .delete("/api/manager/events")
                .then()
                .statusCode(403);
    }

    @Test
    void testDeleteEventUnauthorized() {
        given().when()
                .queryParam("ids", testEvent.getId())
                .delete("/api/manager/events")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleEvents() {
        // Create additional events for bulk delete test
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();

        var event2 = new Event();
        event2.setName("Test Event 2");
        event2.setDescription("Description 2");
        event2.setStartTime(java.time.Instant.now().plus(java.time.Duration.ofDays(2)));
        event2.setEndTime(java.time.Instant.now().plus(java.time.Duration.ofDays(2).plusHours(2)));
        event2.setBookingStartTime(java.time.Instant.now().minus(java.time.Duration.ofDays(7)));
        event2.setBookingDeadline(java.time.Instant.now().plus(java.time.Duration.ofDays(1)));
        event2.setEventLocation(testLocation);
        event2.setManager(manager);

        var event3 = new Event();
        event3.setName("Test Event 3");
        event3.setDescription("Description 3");
        event3.setStartTime(java.time.Instant.now().plus(java.time.Duration.ofDays(3)));
        event3.setEndTime(java.time.Instant.now().plus(java.time.Duration.ofDays(3).plusHours(2)));
        event3.setBookingStartTime(java.time.Instant.now().minus(java.time.Duration.ofDays(7)));
        event3.setBookingDeadline(java.time.Instant.now().plus(java.time.Duration.ofDays(2)));
        event3.setEventLocation(testLocation);
        event3.setManager(manager);

        seedAdditionalEvents(event2, event3);

        // Delete multiple events
        given().when()
                .queryParam("ids", testEvent.getId())
                .queryParam("ids", event2.getId())
                .queryParam("ids", event3.getId())
                .delete("/api/manager/events")
                .then()
                .statusCode(204);

        // Verify all were deleted
        given().when().get("/api/manager/events").then().statusCode(200).body("size()", is(0));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteMultipleEvents_PartialNotFound() {
        given().when()
                .queryParam("ids", testEvent.getId())
                .queryParam("ids", 999L)
                .delete("/api/manager/events")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteEvent_NotFound() {
        given().when().queryParam("ids", 999L).delete("/api/manager/events").then().statusCode(404);
    }

    @Transactional
    void seedAdditionalEvents(Event event2, Event event3) {
        eventRepository.persist(event2);
        eventRepository.persist(event3);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    @Transactional
    void testDeleteEventDeletesAllowances() {
        assertEquals(1, eventUserAllowanceRepository.count());

        given().when()
                .queryParam("ids", testEvent.getId())
                .delete("/api/manager/events")
                .then()
                .statusCode(204);

        assertEquals(0, eventRepository.count());
        assertEquals(0, eventUserAllowanceRepository.count());
    }
}
