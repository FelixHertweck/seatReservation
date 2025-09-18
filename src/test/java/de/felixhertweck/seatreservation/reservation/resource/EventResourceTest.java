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

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.repository.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class EventResourceTest {

    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject SeatRepository seatRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        var testUser = userRepository.findByUsernameOptional("user").orElseThrow();
        var managerUser = userRepository.findByUsernameOptional("manager").orElseThrow();

        var testLocation = new EventLocation();
        testLocation.setName("Test Location for User Event Test");
        testLocation.setAddress("456 Test Avenue");
        testLocation.setManager(managerUser);
        eventLocationRepository.persist(testLocation);

        Event testEvent = new Event();
        testEvent.setName("Accessible Event");
        testEvent.setEventLocation(testLocation);
        eventRepository.persist(testEvent);

        Event otherEvent = new Event();
        otherEvent.setName("Inaccessible Event");
        otherEvent.setEventLocation(testLocation);
        eventRepository.persist(otherEvent);

        EventUserAllowance allowance = new EventUserAllowance(testUser, testEvent, 5);
        eventUserAllowanceRepository.persist(allowance);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetEventsForCurrentUser_Success() {
        given().when()
                .get("/api/user/events")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Accessible Event"))
                .body("[0].reservationsAllowed", is(5));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"USER"})
    void testGetEventsForCurrentUser_NoAllowances() {
        given().when().get("/api/user/events").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void testGetEventsForCurrentUser_Unauthorized() {
        given().when().get("/api/user/events").then().statusCode(401);
    }
}
