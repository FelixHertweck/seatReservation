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
package de.felixhertweck.seatreservation.supervisor.resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CheckInResourceTest {

    @InjectMock UserRepository userRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock ReservationRepository reservationRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setupUserRepositoryMock() {
        // Supervisor user
        User supervisorUser = new User();
        supervisorUser.id = 1L;
        supervisorUser.setUsername("testUser");
        supervisorUser.setRoles(Set.of(Roles.SUPERVISOR));
        when(userRepository.findByUsername("testUser")).thenReturn(supervisorUser);
        when(userRepository.findById(1L)).thenReturn(supervisorUser);

        // Admin user
        User adminUser = new User();
        adminUser.id = 2L;
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(Roles.ADMIN));
        when(userRepository.findByUsername("admin")).thenReturn(adminUser);
        when(userRepository.findById(2L)).thenReturn(adminUser);

        // Manager user
        User managerUser = new User();
        managerUser.id = 3L;
        managerUser.setUsername("manager");
        managerUser.setRoles(Set.of(Roles.MANAGER));
        when(userRepository.findByUsername("manager")).thenReturn(managerUser);
        when(userRepository.findById(3L)).thenReturn(managerUser);

        // Supervisor without access
        User otherSupervisor = new User();
        otherSupervisor.id = 4L;
        otherSupervisor.setUsername("otherSupervisor");
        otherSupervisor.setRoles(Set.of(Roles.SUPERVISOR));
        when(userRepository.findByUsername("otherSupervisor")).thenReturn(otherSupervisor);
        when(userRepository.findById(4L)).thenReturn(otherSupervisor);

        // Build events
        Event event10 = new Event();
        event10.id = 10L;
        event10.setManager(managerUser);

        Event event20 = new Event();
        event20.id = 20L;
        // event20 has no manager and supervisorUser is not supervisor for it

        // Set the authorization matrix
        when(eventRepository.isUserSupervisor(eq(10L), eq(1L))).thenReturn(true);
        when(eventRepository.isUserSupervisor(eq(20L), eq(1L))).thenReturn(false);
        when(eventRepository.isUserSupervisor(anyLong(), eq(2L))).thenReturn(false);
        when(eventRepository.isUserSupervisor(anyLong(), eq(3L))).thenReturn(false);
        when(eventRepository.isUserSupervisor(anyLong(), eq(4L))).thenReturn(false);

        // eventRepository.findById to allow manager checks
        when(eventRepository.findById(10L)).thenReturn(event10);
        when(eventRepository.findById(20L)).thenReturn(event20);

        // Provide a PanacheQuery for findAll containing both events
        PanacheQuery<Event> eventsQuery = (PanacheQuery<Event>) mock(PanacheQuery.class);
        when(eventsQuery.stream()).thenReturn(Stream.of(event10, event20));
        when(eventRepository.findAll()).thenReturn(eventsQuery);

        // Mock reservations for event 10 (two usernames) and for event 20 none
        User r1 = new User();
        r1.setUsername("user1");
        User r2 = new User();
        r2.setUsername("user2");
        Reservation res1 = new Reservation();
        res1.id = 1L;
        res1.setUser(r1);
        res1.setEvent(event10);
        res1.setStatus(ReservationStatus.RESERVED);
        Reservation res2 = new Reservation();
        res2.id = 2L;
        res2.setUser(r2);
        res2.setEvent(event10);
        res2.setStatus(ReservationStatus.RESERVED);

        PanacheQuery<Reservation> reservedQueryEvent10 =
                (PanacheQuery<Reservation>) mock(PanacheQuery.class);
        when(reservedQueryEvent10.stream()).thenReturn(Stream.of(res1, res2));
        when(reservationRepository.find("event.id", 10L)).thenReturn(reservedQueryEvent10);

        PanacheQuery<Reservation> reservedQueryEvent20 =
                (PanacheQuery<Reservation>) mock(PanacheQuery.class);
        when(reservedQueryEvent20.stream()).thenReturn(Stream.empty());
        when(reservationRepository.find("event.id", 20L)).thenReturn(reservedQueryEvent20);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testPostCheckInInfoWithEmptyTokens() {
        CheckInInfoRequestDTO requestDTO = new CheckInInfoRequestDTO();
        requestDTO.userId = 1L;
        requestDTO.eventId = 10L;
        requestDTO.checkInTokens = Collections.emptyList();

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/info")
                .then()
                .statusCode(200)
                .body("reservations", hasSize(0));
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testPostCheckInInfoWithInvalidTokens() {
        CheckInInfoRequestDTO requestDTO = new CheckInInfoRequestDTO();
        requestDTO.userId = 1L;
        requestDTO.eventId = 10L;
        requestDTO.checkInTokens = List.of("invalidToken");

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/info")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testPostCheckInInfoWithMissingUserId() {
        CheckInInfoRequestDTO requestDTO = new CheckInInfoRequestDTO();
        requestDTO.userId = null;
        requestDTO.eventId = 10L;
        requestDTO.checkInTokens = Collections.emptyList();

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/info")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testPostCheckInInfoWithMissingEventId() {
        CheckInInfoRequestDTO requestDTO = new CheckInInfoRequestDTO();
        requestDTO.userId = 1L;
        requestDTO.eventId = null;
        requestDTO.checkInTokens = Collections.emptyList();

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/info")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testProcessCheckInWithEmptyLists() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        10L, 1L, Collections.emptyList(), Collections.emptyList());

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testProcessCheckInWithNonExistentCheckInIds() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(10L, 1L, List.of(1L, 2L, 3L), Collections.emptyList());

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testProcessCheckInWithCancelListAndNonExistentCheckInIds() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(10L, 1L, Collections.emptyList(), List.of(4L, 5L));

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testProcessCheckInWithNonExistentCheckInIdsInMixedList() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(10L, 1L, List.of(1L, 2L), List.of(3L, 4L));

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(400);
    }

    @Test
    void testProcessCheckInWithoutAuthentication() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(
                        10L, 1L, Collections.emptyList(), Collections.emptyList());

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(401);
    }

    @Test
    void testPostCheckInInfoWithoutAuthentication() {
        CheckInInfoRequestDTO requestDTO = new CheckInInfoRequestDTO();
        requestDTO.userId = 1L;
        requestDTO.eventId = 10L;
        requestDTO.checkInTokens = Collections.emptyList();

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/info")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testGetUsernamesWithReservations() {
        // Assuming event with ID 10 exists and has reservations
        given().when()
                .get("/api/supervisor/checkin/usernames/10")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @TestSecurity(user = "admin", roles = Roles.ADMIN)
    void testGetUsernamesWithReservations_AsAdmin() {
        given().when()
                .get("/api/supervisor/checkin/usernames/10")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @TestSecurity(user = "manager", roles = Roles.MANAGER)
    void testGetUsernamesWithReservations_AsManagerForEvent() {
        given().when()
                .get("/api/supervisor/checkin/usernames/10")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @TestSecurity(user = "otherSupervisor", roles = Roles.SUPERVISOR)
    void testGetUsernamesWithReservations_SupervisorNoAccess() {
        given().when().get("/api/supervisor/checkin/usernames/20").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testGetUsernamesWithReservations_SupervisorAccess() {
        given().when()
                .get("/api/supervisor/checkin/usernames/10")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    void testGetUsernamesWithReservationsWithoutAuthentication() {
        given().when().get("/api/supervisor/checkin/usernames/1").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testGetAllEvents() {
        given().when().get("/api/supervisor/checkin/events").then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = Roles.ADMIN)
    void testGetAllEvents_AsAdmin_SeesAll() {
        given().when()
                .get("/api/supervisor/checkin/events")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testGetAllEvents_AsSupervisor_SeesAuthorizedOnly() {
        given().when()
                .get("/api/supervisor/checkin/events")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "otherSupervisor", roles = Roles.SUPERVISOR)
    void testGetAllEvents_AsOtherSupervisor_SeesNone() {
        given().when()
                .get("/api/supervisor/checkin/events")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @TestSecurity(user = "manager", roles = Roles.MANAGER)
    void testGetAllEvents_AsManager_SeesManaged() {
        given().when()
                .get("/api/supervisor/checkin/events")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    void testGetAllEventsWithoutAuthentication() {
        given().when().get("/api/supervisor/checkin/events").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testGetCheckInInfoByUsernameNotFound() {
        given().when().post("/api/supervisor/checkin/info/nonExistentUser").then().statusCode(404);
    }

    @Test
    void testGetCheckInInfoByUsernameWithoutAuthentication() {
        given().when().post("/api/supervisor/checkin/info/testUser").then().statusCode(401);
    }
}
