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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CheckInResourceTest {

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
                new CheckInProcessRequestDTO(Collections.emptyList(), Collections.emptyList());

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
                new CheckInProcessRequestDTO(List.of(1L, 2L, 3L), Collections.emptyList());

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testProcessCheckInWithCancelList() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(Collections.emptyList(), List.of(4L, 5L));

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = Roles.SUPERVISOR)
    void testProcessCheckInWithNonExistentCheckInIdsInMixedList() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(List.of(1L, 2L), List.of(3L, 4L));

        given().contentType(ContentType.JSON)
                .body(requestDTO)
                .when()
                .post("/api/supervisor/checkin/process")
                .then()
                .statusCode(404);
    }

    @Test
    void testProcessCheckInWithoutAuthentication() {
        CheckInProcessRequestDTO requestDTO =
                new CheckInProcessRequestDTO(Collections.emptyList(), Collections.emptyList());

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
        given().when().get("/api/supervisor/checkin/usernames").then().statusCode(200);
    }

    @Test
    void testGetUsernamesWithReservationsWithoutAuthentication() {
        given().when().get("/api/supervisor/checkin/usernames").then().statusCode(401);
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
