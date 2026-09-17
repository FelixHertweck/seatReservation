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

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.CheckInTokenRepository;
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
public class SeatResourceTest {

    @Inject SeatRepository seatRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventLocationAreaRepository eventLocationAreaRepository;
    @Inject EventLocationEntranceRepository eventLocationEntranceRepository;
    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject CheckInTokenRepository checkInTokenRepository;
    @Inject EmailSeatMapTokenRepository emailSeatMapTokenRepository;

    private Seat testSeat;
    private EventLocation testLocation;
    private EventLocationArea areaParkett;
    private EventLocationEntrance entranceA;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        emailSeatMapTokenRepository.deleteAll();

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

        areaParkett = new EventLocationArea("Parkett");
        areaParkett.setEventLocation(testLocation);
        eventLocationAreaRepository.persist(areaParkett);

        entranceA = new EventLocationEntrance("A");
        entranceA.setEventLocation(testLocation);
        eventLocationEntranceRepository.persist(entranceA);

        testSeat = new Seat("A1", "R: 1", testLocation);
        testSeat.setArea(areaParkett);
        testSeat.setEntrance(entranceA);
        seatRepository.persist(testSeat);
    }

    @AfterEach
    @Transactional
    @SuppressWarnings("unused")
    void tearDown() {
        emailSeatMapTokenRepository.deleteAll();
        reservationRepository.deleteAll();
        checkInTokenRepository.deleteAll();
        seatRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationAreaRepository.deleteAll();
        eventLocationEntranceRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void testGetAllManagerSeatsForbidden() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(403);
    }

    @Test
    void testGetAllManagerSeatsUnauthorized() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(401);
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
    void testGetAllManagerSeats() {
        given().when()
                .queryParam("eventLocationId", testLocation.id)
                .get("/api/manager/seats")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(
            user = "manager",
            roles = {"MANAGER"})
    void testGetAllManagerSeatsMissingEventLocationId() {
        given().when().get("/api/manager/seats").then().statusCode(400);
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
    void testGetManagerSeatById() {
        given().when()
                .get("/api/manager/seats/" + testSeat.id)
                .then()
                .statusCode(200)
                .body("id", is(testSeat.id.toString()))
                .body("seatNumber", is("A1"))
                .body("area", is("Parkett"))
                .body("entrance", is("A"));
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
    void testGetManagerSeatByIdNotFound() {
        given().when().get("/api/manager/seats/" + id(999)).then().statusCode(404);
    }
}
