/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
package de.felixhertweck.seatreservation.notification.resource;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.UserNotification;
import de.felixhertweck.seatreservation.model.repository.UserNotificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.notification.enums.ActionType;
import de.felixhertweck.seatreservation.notification.enums.NotificationCategory;
import de.felixhertweck.seatreservation.notification.enums.NotificationPriority;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NotificationResourceTest {

    @Inject UserRepository userRepository;
    @Inject UserNotificationRepository notificationRepository;

    private User testUser;
    private UserNotification testNotification;

    @BeforeEach
    @Transactional
    void setUp() {
        notificationRepository.deleteAll();

        testUser = userRepository.findByUsernameOptional("user").orElseThrow();

        testNotification =
                new UserNotification(
                        testUser,
                        NotificationCategory.BOOKING,
                        "Test Notification",
                        "This is a test notification message.",
                        NotificationPriority.NORMAL,
                        ActionType.NAVIGATE,
                        "/profile/reservations",
                        "View",
                        null);
        notificationRepository.persist(testNotification);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        notificationRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetNotifications_Success() {
        given().when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("totalItems", is(1))
                .body("unreadCount", is(1))
                .body("items[0].title", is("Test Notification"))
                .body("items[0].category", is("BOOKING"));
    }

    @Test
    void testGetNotifications_Unauthorized() {
        given().when().get("/api/notifications").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetNotifications_FilterUnreadOnly() {
        given().queryParam("unreadOnly", true)
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("items", hasSize(1));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetNotifications_FilterCategory() {
        given().queryParam("category", "BOOKING")
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("items", hasSize(1));

        given().queryParam("category", "EVENT_REMINDER")
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testGetUnreadCount_Success() {
        given().when()
                .get("/api/notifications/unread-count")
                .then()
                .statusCode(200)
                .body("unreadCount", is(1));
    }

    @Test
    void testGetUnreadCount_Unauthorized() {
        given().when().get("/api/notifications/unread-count").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testMarkAsRead_Success() {
        given().when()
                .patch("/api/notifications/" + testNotification.id + "/read")
                .then()
                .statusCode(204);

        given().when()
                .get("/api/notifications/unread-count")
                .then()
                .statusCode(200)
                .body("unreadCount", is(0));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testMarkAsRead_NotFound() {
        given().when().patch("/api/notifications/" + id(9999) + "/read").then().statusCode(404);
    }

    @Test
    void testMarkAsRead_Unauthorized() {
        given().when()
                .patch("/api/notifications/" + testNotification.id + "/read")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testMarkAllAsRead_Success() {
        given().when()
                .patch("/api/notifications/read-all")
                .then()
                .statusCode(200)
                .body("unreadCount", is(0));
    }

    @Test
    void testMarkAllAsRead_Unauthorized() {
        given().when().patch("/api/notifications/read-all").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteNotification_Success() {
        given().when().delete("/api/notifications/" + testNotification.id).then().statusCode(204);

        given().when().get("/api/notifications").then().statusCode(200).body("items", hasSize(0));
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testDeleteNotification_NotFound() {
        given().when().delete("/api/notifications/" + id(9999)).then().statusCode(404);
    }

    @Test
    void testDeleteNotification_Unauthorized() {
        given().when().delete("/api/notifications/" + testNotification.id).then().statusCode(401);
    }
}
