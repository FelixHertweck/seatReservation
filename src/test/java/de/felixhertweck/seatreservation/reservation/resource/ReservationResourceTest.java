package de.felixhertweck.seatreservation.reservation.resource;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.*;
import de.felixhertweck.seatreservation.reservation.dto.ReservationsRequestCreateDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ReservationResourceTest {

    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject SeatRepository seatRepository;
    @Inject ReservationRepository reservationRepository;

    private Event testEvent;
    private Seat testSeat1;
    private Seat testSeat2;
    private Seat testSeat3;
    private Reservation testReservation;

    @BeforeEach
    @Transactional
    void setUp() {
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        var testUser = userRepository.findByUsernameOptional("user").orElseThrow();
        userRepository.findByUsernameOptional("admin").orElseThrow();

        var location = new EventLocation();
        location.setName("Test Location for Reservation Test");
        location.setManager(manager);
        eventLocationRepository.persist(location);

        testEvent = new Event();
        testEvent.setName("Test Event for Reservation");
        testEvent.setEventLocation(location);
        eventRepository.persist(testEvent);

        testSeat1 = new Seat("A1", location);
        testSeat2 = new Seat("A2", location);
        testSeat3 = new Seat("A3", location);
        seatRepository.persist(testSeat1);
        seatRepository.persist(testSeat2);
        seatRepository.persist(testSeat3);

        var allowance = new EventUserAllowance(testUser, testEvent, 2);
        eventUserAllowanceRepository.persist(allowance);

        testReservation = new Reservation(testUser, testEvent, testSeat1, LocalDateTime.now());
        reservationRepository.persist(testReservation);

        allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() - 1);
        eventUserAllowanceRepository.persist(allowance);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetMyReservations_Success() {
        given().when()
                .get("/api/user/reservations")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].seat.seatNumber", is("A1"));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testGetMyReservations_EmptyList() {
        given().when().get("/api/user/reservations").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void testGetMyReservations_Unauthorized() {
        given().when().get("/api/user/reservations").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetMyReservationById_Success() {
        given().when()
                .get("/api/user/reservations/" + testReservation.id)
                .then()
                .statusCode(200)
                .body("seat.seatNumber", is("A1"));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetMyReservationById_NotFound() {
        given().when().get("/api/user/reservations/9999").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testGetMyReservationById_Forbidden() {
        given().when().get("/api/user/reservations/" + testReservation.id).then().statusCode(403);
    }

    @Test
    void testGetMyReservationById_Unauthorized() {
        given().when().get("/api/user/reservations/" + testReservation.id).then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testCreateReservation_Success() {
        var request = new ReservationsRequestCreateDTO(testEvent.id, List.of(testSeat2.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].seat.seatNumber", is("A2"));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testCreateReservation_SeatAlreadyReserved() {
        var request = new ReservationsRequestCreateDTO(testEvent.id, List.of(testSeat1.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @Transactional
    void testCreateReservation_NoAllowance() {
        var request =
                new ReservationsRequestCreateDTO(testEvent.id, List.of(testSeat2.id, testSeat3.id));

        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(406); // NoSeatsAvailableException -> NOT_ACCEPTABLE
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    @Transactional
    void testCreateReservation_EventNotAllowed() {
        var otherEvent = new Event();
        otherEvent.setName("Other Event");
        otherEvent.setEventLocation(testEvent.getEventLocation());
        eventRepository.persist(otherEvent);

        var request = new ReservationsRequestCreateDTO(otherEvent.id, List.of(testSeat2.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testCreateReservation_InvalidRequest() {
        var request = new ReservationsRequestCreateDTO(testEvent.id, List.of());
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateReservation_Unauthorized() {
        var request = new ReservationsRequestCreateDTO(testEvent.id, List.of(testSeat2.id));
        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/user/reservations")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteReservation_Success() {
        given().when()
                .delete("/api/user/reservations/" + testReservation.id)
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testDeleteReservation_Forbidden() {
        given().when()
                .delete("/api/user/reservations/" + testReservation.id)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteReservation_NotFound() {
        given().when().delete("/api/user/reservations/9999").then().statusCode(404);
    }

    @Test
    void testDeleteReservation_Unauthorized() {
        given().when()
                .delete("/api/user/reservations/" + testReservation.id)
                .then()
                .statusCode(401);
    }
}
