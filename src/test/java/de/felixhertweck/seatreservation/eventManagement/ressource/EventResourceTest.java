package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
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

    @Inject UserRepository userRepository;

    private Event testEvent;
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

        testEvent = new Event();
        testEvent.setName("Test Event");
        testEvent.setEventLocation(testLocation);
        testEvent.setManager(user);
        eventRepository.persist(testEvent);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
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
}
