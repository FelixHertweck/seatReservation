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
package de.felixhertweck.seatreservation.userManagment.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class UserResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void testGetAllUsersAsAdmin() {
        given().when()
                .get("/api/users/admin")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @TestSecurity(user = "testuser", roles = "USER")
    public void testGetAllUsersAsAdminForbidden() {
        given().when().get("/api/users/admin").then().statusCode(403);
    }

    @Test
    public void testGetAllUsersAsAdminUnauthorized() {
        given().when().get("/api/users/admin").then().statusCode(401);
    }
}
