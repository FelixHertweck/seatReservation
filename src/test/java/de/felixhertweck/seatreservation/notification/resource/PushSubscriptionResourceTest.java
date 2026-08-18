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

import java.util.List;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.UserPushSubscription;
import de.felixhertweck.seatreservation.model.repository.UserPushSubscriptionRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.notification.dto.PushSubscriptionRequestDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PushSubscriptionResourceTest {

    @Inject UserRepository userRepository;
    @Inject UserPushSubscriptionRepository pushSubscriptionRepository;

    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        pushSubscriptionRepository.deleteAll();
        testUser = userRepository.findByUsernameOptional("user").orElseThrow();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        pushSubscriptionRepository.deleteAll();
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testRegisterSubscription_Success() {
        PushSubscriptionRequestDTO dto =
                new PushSubscriptionRequestDTO(
                        "https://push.example.com/sub/123", "p256dhKeyBase64", "authSecretBase64");

        given().contentType("application/json")
                .body(dto)
                .when()
                .post("/api/push/subscriptions")
                .then()
                .statusCode(201);

        List<UserPushSubscription> subs = pushSubscriptionRepository.findByUser(testUser);
        assertEquals(1, subs.size());
        assertEquals("https://push.example.com/sub/123", subs.get(0).getEndpoint());
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testRegisterSubscription_InvalidRequest() {
        PushSubscriptionRequestDTO dto = new PushSubscriptionRequestDTO("", "", "");

        given().contentType("application/json")
                .body(dto)
                .when()
                .post("/api/push/subscriptions")
                .then()
                .statusCode(400);
    }

    @Test
    void testRegisterSubscription_Unauthorized() {
        PushSubscriptionRequestDTO dto =
                new PushSubscriptionRequestDTO(
                        "https://push.example.com/sub/123", "p256dhKeyBase64", "authSecretBase64");

        given().contentType("application/json")
                .body(dto)
                .when()
                .post("/api/push/subscriptions")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void testUnregisterSubscription_Success() {
        seedSubscription("https://push.example.com/sub/456");

        given().queryParam("endpoint", "https://push.example.com/sub/456")
                .when()
                .delete("/api/push/subscriptions")
                .then()
                .statusCode(204);

        assertTrue(pushSubscriptionRepository.findByUser(testUser).isEmpty());
    }

    @Transactional
    void seedSubscription(String endpoint) {
        UserPushSubscription sub =
                new UserPushSubscription(testUser, endpoint, "p256key", "authkey");
        pushSubscriptionRepository.persist(sub);
    }

    @Test
    void testUnregisterSubscription_Unauthorized() {
        given().queryParam("endpoint", "https://push.example.com/sub/456")
                .when()
                .delete("/api/push/subscriptions")
                .then()
                .statusCode(401);
    }
}
