package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

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
        testSeat.setLocation(testLocation);
        seatRepository.persist(testSeat);
    }

    @AfterEach
    @Transactional
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
                .body(
                        Map.of(
                                "seatNumber",
                                "A2",
                                "eventLocationId",
                                testLocation.id,
                                "xCoordinate",
                                1,
                                "yCoordinate",
                                2))
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
                .body(
                        Map.of(
                                "seatNumber",
                                "A3",
                                "eventLocationId",
                                testLocation.id,
                                "xCoordinate",
                                1,
                                "yCoordinate",
                                3))
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
                .body(
                        Map.of(
                                "seatNumber",
                                "A3",
                                "eventLocationId",
                                testLocation.id,
                                "xCoordinate",
                                1,
                                "yCoordinate",
                                3))
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
        given().when().delete("/api/manager/seats/" + testSeat.id).then().statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testDeleteManagerSeatNotFound() {
        given().when().delete("/api/manager/seats/999").then().statusCode(404);
    }
}
