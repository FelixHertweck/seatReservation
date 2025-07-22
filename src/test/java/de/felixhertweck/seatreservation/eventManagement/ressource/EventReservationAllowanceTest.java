package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.reservation.EventNotFoundException;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventReservationAllowanceTest {

    @InjectMock EventService eventService;

    @InjectMock UserSecurityContext userSecurityContext;

    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"MANAGER"})
    void getReservationAllowanceById_Success() {
        EventUserAllowancesDto dto = new EventUserAllowancesDto(1L, 2L, 5);
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventService.getReservationAllowanceById(anyLong(), any(User.class))).thenReturn(dto);

        given().when()
                .get("/api/manager/reservationAllowance/1")
                .then()
                .statusCode(200)
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
        when(eventService.getReservationAllowanceById(anyLong(), any(User.class)))
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
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventService.getReservationAllowances(any(User.class)))
                .thenReturn(Collections.singletonList(new EventUserAllowancesDto(1L, 2L, 5)));

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
        when(userSecurityContext.getCurrentUser()).thenReturn(new User());
        when(eventService.getReservationAllowancesByEventId(anyLong(), any(User.class)))
                .thenReturn(Collections.singletonList(new EventUserAllowancesDto(1L, 2L, 5)));

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
}
