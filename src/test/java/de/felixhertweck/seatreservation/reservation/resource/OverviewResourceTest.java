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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OverviewResourceTest {

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
    void getOverview_AsManager_Success() {
        given().when()
                .get("/api/manager/overview")
                .then()
                .statusCode(200)
                .body("stats", notNullValue())
                .body("upcomingEvents", notNullValue())
                .body("deadlineWarnings", notNullValue());
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {"USER"})
    void getOverview_AsUser_Forbidden() {
        given().when().get("/api/manager/overview").then().statusCode(403);
    }
}
