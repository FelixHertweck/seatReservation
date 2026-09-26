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

import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.LayoutChangeLog;
import de.felixhertweck.seatreservation.model.repository.CheckInTokenRepository;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.LayoutChangeLogRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(
        user = "manager",
        roles = {"MANAGER"})
@JwtSecurity(
        claims =
                @Claim(
                        key = "uid",
                        value = "00000000-0000-0000-0000-000000000002",
                        type = ClaimType.STRING))
public class LayoutBatchResourceTest {

    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationMarkerRepository markerRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventRepository eventRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject CheckInTokenRepository checkInTokenRepository;
    @Inject EmailSeatMapTokenRepository emailSeatMapTokenRepository;
    @Inject LayoutChangeLogRepository changeLogRepository;
    @Inject UserRepository userRepository;

    private EventLocation location;
    private EventLocation otherLocation;

    @BeforeEach
    @Transactional
    void setUp() {
        cleanUpDatabase();
        var manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        location = newLocation("Batch Location", manager);
        otherLocation = newLocation("Other Batch Location", manager);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        cleanUpDatabase();
    }

    private EventLocation newLocation(
            String name, de.felixhertweck.seatreservation.model.entity.User manager) {
        EventLocation l = new EventLocation();
        l.setName(name);
        l.setAddress("1 Test Street");
        l.setManager(manager);
        eventLocationRepository.persist(l);
        return l;
    }

    private void cleanUpDatabase() {
        emailSeatMapTokenRepository.deleteAll();
        reservationRepository.deleteAll();
        checkInTokenRepository.deleteAll();
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        markerRepository.deleteAll();
        entranceRepository.deleteAll();
        areaRepository.deleteAll();
        eventLocationRepository.deleteAll();
        changeLogRepository.deleteAll();
    }

    private String url() {
        return "/api/manager/eventlocations/" + location.id + "/layout-operations";
    }

    private String createSeat(String number, String areaRef, String entranceRef) {
        return """
        {"entity":"SEAT","action":"CREATE","ref":"s%s","areaRef":"%s","entranceRef":"%s",
         "data":{"seatNumber":"%s","seatRow":"A","eventLocationId":"%s",
                 "coordinate":{"xCoordinate":1,"yCoordinate":2}}}\
        """
                .formatted(number, areaRef, entranceRef, number, location.id);
    }

    @Test
    void appliesOperationsInOrderResolvesRefsAndLogsEachStep() {
        String body =
                """
                {"operations":[
                  {"entity":"AREA","action":"CREATE","ref":"a1",
                   "data":{"name":"Front","eventLocationId":"%1$s","boundary":[]}},
                  {"entity":"ENTRANCE","action":"CREATE","ref":"e1",
                   "data":{"name":"North","eventLocationId":"%1$s"}},
                  %2$s,
                  %3$s,
                  {"entity":"MARKER","action":"CREATE","ref":"m1",
                   "data":{"label":"Stage","eventLocationId":"%1$s",
                           "coordinate":{"xCoordinate":5,"yCoordinate":5}}}
                ]}\
                """
                        .formatted(
                                location.id,
                                createSeat("1", "a1", "e1"),
                                createSeat("2", "a1", "e1"));

        String batchId =
                given().contentType(ContentType.JSON)
                        .body(body)
                        .when()
                        .post(url())
                        .then()
                        .statusCode(200)
                        .body("batchId", notNullValue())
                        .body("results.size()", is(5))
                        .body("results[0].sequenceNo", is(1))
                        .body("results[0].entity", is("AREA"))
                        .body("results[0].ref", is("a1"))
                        .body("results[4].entity", is("MARKER"))
                        .extract()
                        .path("batchId");

        assertEquals(2, seatRepository.findByEventLocation(location).size());
        seatRepository
                .findByEventLocation(location)
                .forEach(
                        s -> {
                            assertEquals("Front", s.getArea().getName());
                            assertEquals("North", s.getEntrance().getName());
                        });

        List<LayoutChangeLog> log = changeLogRepository.findByBatch(UUID.fromString(batchId));
        assertEquals(5, log.size());
        assertEquals(
                List.of("AREA", "ENTRANCE", "SEAT", "SEAT", "MARKER"),
                log.stream().map(LayoutChangeLog::getEntityType).toList());
        assertEquals(
                List.of(1, 2, 3, 4, 5), log.stream().map(LayoutChangeLog::getSequenceNo).toList());
        assertTrue(log.stream().allMatch(l -> l.getEntityId() != null && l.getPayload() != null));
    }

    @Test
    void failingOperationRollsBackTheWholeBatchAndItsLog() {
        String body =
                """
                {"operations":[
                  {"entity":"ENTRANCE","action":"CREATE","ref":"e1",
                   "data":{"name":"North","eventLocationId":"%1$s"}},
                  {"entity":"ENTRANCE","action":"CREATE","ref":"e2",
                   "data":{"name":"  ","eventLocationId":"%1$s"}}
                ]}\
                """
                        .formatted(location.id);

        given().contentType(ContentType.JSON).body(body).when().post(url()).then().statusCode(400);

        assertEquals(0, entranceRepository.count());
        assertEquals(0, changeLogRepository.count());
    }

    @Test
    void unknownRefIsRejected() {
        given().contentType(ContentType.JSON)
                .body("{\"operations\":[" + createSeat("1", "nope", "null") + "]}")
                .when()
                .post(url())
                .then()
                .statusCode(400);
        assertEquals(0, seatRepository.count());
    }

    @Test
    void duplicateRefIsRejected() {
        String entrance =
                """
                {"entity":"ENTRANCE","action":"CREATE","ref":"e1",
                 "data":{"name":"North","eventLocationId":"%s"}}\
                """
                        .formatted(location.id);
        given().contentType(ContentType.JSON)
                .body("{\"operations\":[" + entrance + "," + entrance + "]}")
                .when()
                .post(url())
                .then()
                .statusCode(400);
        assertEquals(0, entranceRepository.count());
    }

    @Test
    void operationOnEntityOfAnotherLocationIsRejected() {
        UUID foreign = persistEntrance(otherLocation, "Foreign");

        String body =
                """
                {"operations":[
                  {"entity":"ENTRANCE","action":"UPDATE","id":"%s",
                   "data":{"name":"Hijacked","eventLocationId":"%s"}}
                ]}\
                """
                        .formatted(foreign, location.id);

        given().contentType(ContentType.JSON).body(body).when().post(url()).then().statusCode(400);

        assertEquals("Foreign", entranceRepository.findById(foreign).getName());
    }

    @Test
    void mismatchingEventLocationIdInPayloadIsRejected() {
        String body =
                """
                {"operations":[
                  {"entity":"ENTRANCE","action":"CREATE",
                   "data":{"name":"North","eventLocationId":"%s"}}
                ]}\
                """
                        .formatted(otherLocation.id);

        given().contentType(ContentType.JSON).body(body).when().post(url()).then().statusCode(400);
        assertEquals(0, entranceRepository.count());
    }

    @Test
    void deleteOperationRemovesEntity() {
        UUID id = persistEntrance(location, "Old");

        given().contentType(ContentType.JSON)
                .body(
                        "{\"operations\":[{\"entity\":\"ENTRANCE\",\"action\":\"DELETE\",\"id\":\""
                                + id
                                + "\"}]}")
                .when()
                .post(url())
                .then()
                .statusCode(200)
                .body("results[0].id", is(id.toString()));

        assertEquals(0, entranceRepository.count());
    }

    @Test
    void emptyOperationListIsRejected() {
        given().contentType(ContentType.JSON)
                .body("{\"operations\":[]}")
                .when()
                .post(url())
                .then()
                .statusCode(400);
    }

    @Transactional
    UUID persistEntrance(EventLocation l, String name) {
        EventLocationEntrance e = new EventLocationEntrance(name);
        e.setEventLocation(eventLocationRepository.findById(l.id));
        entranceRepository.persist(e);
        return e.id;
    }
}
