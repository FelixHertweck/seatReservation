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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;
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

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void importUsers_Success_AdminRole() {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        dtos.add(
                new AdminUserCreationDto(
                        "testuser1",
                        "test1@example.com",
                        false,
                        false,
                        "password",
                        "John",
                        "Doe",
                        Set.of("USER"),
                        Set.of()));
        dtos.add(
                new AdminUserCreationDto(
                        "testuser2",
                        "test2@example.com",
                        false,
                        false,
                        "password",
                        "Jane",
                        "Doe",
                        Set.of("MANAGER"),
                        Set.of()));

        given().contentType("application/json")
                .body(dtos)
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @TestSecurity(user = "testuser", roles = "USER")
    public void importUsers_Forbidden_UserRole() {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        dtos.add(
                new AdminUserCreationDto(
                        "testuser1",
                        "test1@example.com",
                        false,
                        false,
                        "password",
                        "John",
                        "Doe",
                        Set.of("USER"),
                        Set.of()));

        given().contentType("application/json")
                .body(dtos)
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(403);
    }

    @Test
    public void importUsers_Unauthorized() {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        dtos.add(
                new AdminUserCreationDto(
                        "testuser1",
                        "test1@example.com",
                        false,
                        false,
                        "password",
                        "John",
                        "Doe",
                        Set.of("USER"),
                        Set.of()));

        given().contentType("application/json")
                .body(dtos)
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void importUsers_InvalidInput() {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        // Invalid DTO: empty username
        dtos.add(
                new AdminUserCreationDto(
                        "",
                        "test1@example.com",
                        false,
                        false,
                        "password",
                        "John",
                        "Doe",
                        Set.of("USER"),
                        Set.of()));

        given().contentType("application/json")
                .body(dtos)
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(400); // Expecting Bad Request due to InvalidUserException
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void importUsers_DuplicateUser() {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        dtos.add(
                new AdminUserCreationDto(
                        "existinguser",
                        "existing@example.com",
                        false,
                        false,
                        "password",
                        "John",
                        "Doe",
                        Set.of("USER"),
                        Set.of()));
        dtos.add(
                new AdminUserCreationDto(
                        "existinguser",
                        "existing2@example.com",
                        false,
                        false,
                        "password",
                        "Jane",
                        "Doe",
                        Set.of("USER"),
                        Set.of()));

        given().contentType("application/json")
                .body(
                        new AdminUserCreationDto(
                                "existinguser",
                                "existing@example.com",
                                false,
                                false,
                                "password",
                                "John",
                                "Doe",
                                Set.of("USER"),
                                Set.of()))
                .when()
                .post("/api/users/admin")
                .then()
                .statusCode(is(anyOf(equalTo(200), equalTo(409))));

        // Then, try to import a set including the duplicate
        given().contentType("application/json")
                .body(dtos)
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(409); // Expecting Conflict due to DuplicateUserException
    }

    @jakarta.inject.Inject
    de.felixhertweck.seatreservation.model.repository.UserRepository userRepository;

    @jakarta.inject.Inject
    de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository refreshTokenRepository;

    @jakarta.inject.Inject
    de.felixhertweck.seatreservation.userManagment.service.UserService userService;

    @Test
    public void testDeleteUser_WithAssociatedEntities_Success() {
        de.felixhertweck.seatreservation.model.entity.User user =
                new de.felixhertweck.seatreservation.model.entity.User(
                        "cascadeuser",
                        "cascade@example.com",
                        true,
                        true,
                        "hash",
                        "salt",
                        "First",
                        "Last",
                        Set.of("USER"),
                        Set.of());
        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew()
                .run(
                        () -> {
                            userRepository.persist(user);
                            de.felixhertweck.seatreservation.model.entity.RefreshToken
                                    refreshToken =
                                            new de.felixhertweck.seatreservation.model.entity
                                                    .RefreshToken(
                                                    "tokenhash123",
                                                    user,
                                                    java.time.Instant.now(),
                                                    java.time.Instant.now()
                                                            .plus(java.time.Duration.ofDays(30)));
                            refreshTokenRepository.persist(refreshToken);
                        });

        de.felixhertweck.seatreservation.utils.AuthenticatedUser adminCaller =
                new de.felixhertweck.seatreservation.utils.AuthenticatedUser(
                        java.util.UUID.randomUUID(), Set.of("ADMIN"));

        userService.deleteUser(List.of(user.id), adminCaller);

        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew()
                .run(
                        () -> {
                            org.junit.jupiter.api.Assertions.assertNull(
                                    userRepository.findById(user.id));
                            org.junit.jupiter.api.Assertions.assertEquals(
                                    0, refreshTokenRepository.count("user.id", user.id));
                        });
    }
}
