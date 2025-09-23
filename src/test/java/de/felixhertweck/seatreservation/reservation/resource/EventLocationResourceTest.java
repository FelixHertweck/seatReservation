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
package de.felixhertweck.seatreservation.reservation.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

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
class EventLocationResourceTest {

    @Inject UserRepository userRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject de.felixhertweck.seatreservation.model.repository.SeatRepository seatRepository;

    @Inject
    de.felixhertweck.seatreservation.model.repository.ReservationRepository reservationRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean slate (respect FK constraints)
        // 1) Reservations -> 2) Allowances -> 3) Seats -> 4) Events -> 5) Locations
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();

        // Seed minimal data required for tests (get-or-create user)
        var user = userRepository.findByUsername("user");
        if (user == null) {
            user = new de.felixhertweck.seatreservation.model.entity.User();
            user.setUsername("user");
            user.setEmail("user@example.com");
            user.setEmailVerified(true);
            userRepository.persist(user);
        }

        var location =
                new de.felixhertweck.seatreservation.model.entity.EventLocation(
                        "My Test Location", "123 Test St", null, 100);
        eventLocationRepository.persist(location);

        var event = new de.felixhertweck.seatreservation.model.entity.Event();
        event.setName("My Test Event");
        event.setEventLocation(location);
        eventRepository.persist(event);

        var allowance = new de.felixhertweck.seatreservation.model.entity.EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(event);
        allowance.setReservationsAllowedCount(1);
        eventUserAllowanceRepository.persist(allowance);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Reverse order respecting FKs
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void getLocations_ReturnsLocationsForUser() {
        given().when()
                .get("/api/user/locations")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", is("My Test Location"))
                .body("[0].address", is("123 Test St"))
                .body("[0].capacity", is(100));
    }

    @Test
    void getLocations_NoAuth_ReturnsUnauthorized() {
        given().when().get("/api/user/locations").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void getLocations_UserWithNoLocations_ReturnsEmptyList() {
        // Perform cleanup in a committed transaction so the resource sees the changes
        clearUserLocationsForTest();

        given().when().get("/api/user/locations").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void getLocations_DeduplicatesLocations_FromAllowanceAndReservation() {
        // Seed reservation in a committed transaction so the resource sees it
        seedReservationForDedup();

        given().when()
                .get("/api/user/locations")
                .then()
                .statusCode(200)
                .body("$", hasSize(1)) // Should still be 1 because location deduplicated
                .body("[0].name", is("My Test Location"));
    }

    @Transactional
    void clearUserLocationsForTest() {
        // Reverse order respecting FKs
        reservationRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Transactional
    void seedReservationForDedup() {
        var user = userRepository.findByUsername("user");
        var location = eventLocationRepository.findAll().firstResult();
        var event = eventRepository.findAll().firstResult();
        var seat = new de.felixhertweck.seatreservation.model.entity.Seat("S1", "Row 1", location);
        seatRepository.persist(seat);
        var reservation =
                new de.felixhertweck.seatreservation.model.entity.Reservation(
                        user,
                        event,
                        seat,
                        java.time.Instant.now(),
                        de.felixhertweck.seatreservation.model.entity.ReservationStatus.RESERVED);
        reservationRepository.persist(reservation);
    }
}
