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

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventReservationAllowanceService;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventReservationAllowanceResourceTest {

    @InjectMock EventReservationAllowanceService eventReservationAllowanceService;

    @InjectMock UserSecurityContext userSecurityContext;

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void getReservationAllowanceById_Success() {
        // Create mock objects
        User mockUser = new User();
        mockUser.id = 2L;

        Event mockEvent = new Event();
        mockEvent.id = 1L;

        EventUserAllowance mockAllowance = new EventUserAllowance(mockUser, mockEvent, 5);
        mockAllowance.id = 1L;

        // Setup mocks
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventReservationAllowanceService.getReservationAllowanceById(
                        anyLong(), any(User.class)))
                .thenReturn(mockAllowance);

        given().when()
                .get("/api/manager/reservationAllowance/1")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("eventId", is(1))
                .body("userId", is(2))
                .body("reservationsAllowedCount", is(5));
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void getReservationAllowanceById_NotFound() {
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventReservationAllowanceService.getReservationAllowanceById(
                        anyLong(), any(User.class)))
                .thenThrow(new EventNotFoundException("Allowance not found"));

        given().when().get("/api/manager/reservationAllowance/99").then().statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void getReservationAllowanceById_Forbidden() {
        given().when().get("/api/manager/reservationAllowance/1").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void deleteReservationAllowance_Success() {
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        given().when().delete("/api/manager/reservationAllowance/1").then().statusCode(204);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void deleteReservationAllowance_Forbidden() {
        given().when().delete("/api/manager/reservationAllowance/1").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void getReservationAllowances_Success() {
        // Create mock objects
        User mockUser = new User();
        mockUser.id = 2L;

        Event mockEvent = new Event();
        mockEvent.id = 1L;

        EventUserAllowance mockAllowance = new EventUserAllowance(mockUser, mockEvent, 5);
        mockAllowance.id = 1L;

        // Setup mocks
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventReservationAllowanceService.getReservationAllowances(any(User.class)))
                .thenReturn(List.of(mockAllowance));

        given().when()
                .get("/api/manager/reservationAllowance")
                .then()
                .statusCode(200)
                .body("[0].eventId", is(1))
                .body("[0].userId", is(2))
                .body("[0].reservationsAllowedCount", is(5));
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void getReservationAllowances_Forbidden() {
        given().when().get("/api/manager/reservationAllowance").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void getReservationAllowancesByEventId_Success() {
        // Create mock objects
        User mockUser = new User();
        mockUser.id = 2L;

        Event mockEvent = new Event();
        mockEvent.id = 1L;

        EventUserAllowance mockAllowance = new EventUserAllowance(mockUser, mockEvent, 5);
        mockAllowance.id = 1L;

        // Setup mocks
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventReservationAllowanceService.getReservationAllowancesByEventId(
                        anyLong(), any(User.class)))
                .thenReturn(List.of(mockAllowance));

        given().when()
                .get("/api/manager/reservationAllowance/event/1")
                .then()
                .statusCode(200)
                .body("[0].eventId", is(1))
                .body("[0].userId", is(2))
                .body("[0].reservationsAllowedCount", is(5));
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void getReservationAllowancesByEventId_Forbidden() {
        given().when().get("/api/manager/reservationAllowance/event/1").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void updateReservationAllowance_Success() {
        EventUserAllowanceUpdateDto requestDto = new EventUserAllowanceUpdateDto(1L, 1L, 2L, 10);

        // Create mock objects
        User mockUser = new User();
        mockUser.id = 2L;

        Event mockEvent = new Event();
        mockEvent.id = 1L;

        EventUserAllowance mockAllowance = new EventUserAllowance(mockUser, mockEvent, 10);
        mockAllowance.id = 1L;

        // Setup mocks
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventReservationAllowanceService.updateReservationAllowance(
                        anyLong(), anyLong(), anyLong(), any(int.class), any(User.class)))
                .thenReturn(mockAllowance);

        given().contentType("application/json")
                .body(requestDto)
                .when()
                .put("/api/manager/reservationAllowance")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("eventId", is(1))
                .body("userId", is(2))
                .body("reservationsAllowedCount", is(10));
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void updateReservationAllowance_NotFound() {
        EventUserAllowanceUpdateDto requestDto = new EventUserAllowanceUpdateDto(99L, 1L, 2L, 10);

        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventReservationAllowanceService.updateReservationAllowance(
                        any(Long.class),
                        any(Long.class),
                        any(Long.class),
                        any(Integer.class),
                        any(User.class)))
                .thenThrow(new EventNotFoundException("Allowance not found"));

        given().contentType("application/json")
                .body(requestDto)
                .when()
                .put("/api/manager/reservationAllowance")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"USER"})
    void updateReservationAllowance_Forbidden() {
        EventUserAllowanceUpdateDto requestDto = new EventUserAllowanceUpdateDto(1L, 1L, 2L, 10);

        given().contentType("application/json")
                .body(requestDto)
                .when()
                .put("/api/manager/reservationAllowance")
                .then()
                .statusCode(403);
    }
}
