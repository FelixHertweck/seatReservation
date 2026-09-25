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
package de.felixhertweck.seatreservation.usermanagement.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserCreationDto;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.ClaimType;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testGetAllUsersAsAdmin() {
        given().when()
                .get("/api/users/admin")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @TestSecurity(user = "testuser", roles = "USER")
    void testGetAllUsersAsAdminForbidden() {
        given().when().get("/api/users/admin").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "manager", roles = "MANAGER")
    void addTagToUsers_ManagerAllowed_ReportsUnknownUsernames() {
        given().contentType("application/json")
                .body("{\"usernames\":[\"no-such-user\"],\"tag\":\"mitglied-2026\"}")
                .when()
                .post("/api/users/manager/tags")
                .then()
                .statusCode(200)
                .body("notFound", org.hamcrest.Matchers.contains("no-such-user"));
    }

    @Test
    @TestSecurity(user = "manager", roles = "MANAGER")
    void addTagToUsers_BlankTag_BadRequest() {
        given().contentType("application/json")
                .body("{\"usernames\":[\"a\"],\"tag\":\" \"}")
                .when()
                .post("/api/users/manager/tags")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser", roles = "USER")
    void addTagToUsers_UserForbidden() {
        given().contentType("application/json")
                .body("{\"usernames\":[\"a\"],\"tag\":\"x\"}")
                .when()
                .post("/api/users/manager/tags")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateTagsForUsers_AdminAllowed_ReportsUnknownIds() {
        String id = "00000000-0000-0000-0000-00000000ffff";
        given().contentType("application/json")
                .body("{\"userIds\":[\"" + id + "\"],\"addTags\":[\"x\"],\"removeTags\":[]}")
                .when()
                .post("/api/users/admin/tags")
                .then()
                .statusCode(200)
                .body("notFound", org.hamcrest.Matchers.contains(id));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateTagsForUsers_NoTags_BadRequest() {
        given().contentType("application/json")
                .body(
                        "{\"userIds\":[\"00000000-0000-0000-0000-000000000001\"],\"addTags\":[],\"removeTags\":[]}")
                .when()
                .post("/api/users/admin/tags")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "manager", roles = "MANAGER")
    void updateTagsForUsers_ManagerForbidden() {
        given().contentType("application/json")
                .body(
                        "{\"userIds\":[\"00000000-0000-0000-0000-000000000001\"],\"addTags\":[\"x\"],\"removeTags\":[]}")
                .when()
                .post("/api/users/admin/tags")
                .then()
                .statusCode(403);
    }

    @Test
    void testGetAllUsersAsAdminUnauthorized() {
        given().when().get("/api/users/admin").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void importUsers_Success_AdminRole() {
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
                .body("created.size()", is(2))
                .body("failed.size()", is(0));
    }

    @Test
    @TestSecurity(user = "testuser", roles = "USER")
    void importUsers_Forbidden_UserRole() {
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
    void importUsers_Unauthorized() {
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
    void importUsers_InvalidEntry_IsReportedAndDoesNotRejectTheBatch() {
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
                .statusCode(200)
                .body("created.size()", is(0))
                .body("failed[0].reason", equalTo("INVALID"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void importUsers_DuplicateUser_IsReportedNotRejected() {
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
                .statusCode(200)
                .body("failed.reason", org.hamcrest.Matchers.hasItem("USERNAME_EXISTS"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void importUsers_ExistingUsernameWithOtherCasing_IsReported() {
        String body =
                "[{\"username\":\"CaseTest.User\",\"email\":null,\"emailVerified\":false,"
                        + "\"sendEmailVerification\":false,\"password\":\"password123\","
                        + "\"firstname\":\"Case\",\"lastname\":\"Test\","
                        + "\"roles\":[\"USER\"],\"tags\":[]}]";
        given().contentType("application/json")
                .body(body)
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(200)
                .body("created.size()", is(1));

        given().contentType("application/json")
                .body(body.replace("CaseTest.User", "casetest.user"))
                .when()
                .post("/api/users/admin/import")
                .then()
                .statusCode(200)
                .body("created.size()", is(0))
                .body("failed[0].reason", equalTo("USERNAME_EXISTS"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000001",
                            type = ClaimType.STRING))
    void resolveImportConflicts_UnknownUser_IsReportedAsFailure() {
        String body =
                "[{\"action\":\"UPDATE\",\"existingUserId\":\"00000000-0000-0000-0000-00000000ffff\","
                    + "\"update\":{\"firstname\":\"A\",\"lastname\":\"B\",\"email\":null,"
                    + "\"emailVerified\":false,\"sendEmailVerification\":false,"
                    + "\"roles\":[\"USER\"],\"tags\":[]}}]";
        given().contentType("application/json")
                .body(body)
                .when()
                .post("/api/users/admin/import/resolve")
                .then()
                .log()
                .body()
                .statusCode(200)
                .body("[0].success", is(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    @JwtSecurity(
            claims =
                    @Claim(
                            key = "uid",
                            value = "00000000-0000-0000-0000-000000000001",
                            type = ClaimType.STRING))
    void resolveImportConflicts_ReplaceThenUpdate_WorksAgainstTheDatabase() {
        String user =
                "{\"username\":\"resolve.me\",\"email\":null,\"emailVerified\":false,"
                        + "\"sendEmailVerification\":false,\"password\":\"password123\","
                        + "\"firstname\":\"Old\",\"lastname\":\"Name\","
                        + "\"roles\":[\"USER\"],\"tags\":[\"keep\"]}";
        String oldId =
                given().contentType("application/json")
                        .body("[" + user + "]")
                        .when()
                        .post("/api/users/admin/import")
                        .then()
                        .statusCode(200)
                        .body("created.size()", is(1))
                        .extract()
                        .path("created[0].id");

        String newId =
                given().contentType("application/json")
                        .body(
                                "[{\"action\":\"REPLACE\",\"existingUserId\":\""
                                        + oldId
                                        + "\",\"replacement\":"
                                        + user.replace("Old", "Replaced")
                                        + "}]")
                        .when()
                        .post("/api/users/admin/import/resolve")
                        .then()
                        .statusCode(200)
                        .body("[0].success", is(true))
                        .extract()
                        .path("[0].userId");
        org.junit.jupiter.api.Assertions.assertNotEquals(oldId, newId);

        given().contentType("application/json")
                .body(
                        "[{\"action\":\"UPDATE\",\"existingUserId\":\""
                                + newId
                                + "\",\"update\":{\"firstname\":\"Updated\","
                                + "\"lastname\":\"Name\",\"email\":null,\"emailVerified\":false,"
                                + "\"sendEmailVerification\":false,\"roles\":[\"USER\"],"
                                + "\"tags\":[\"keep\",\"more\"]}}]")
                .when()
                .post("/api/users/admin/import/resolve")
                .then()
                .statusCode(200)
                .body("[0].success", is(true))
                .body("[0].userId", equalTo(newId));

        // The old user is gone, the replaced one carries the updated values.
        given().when()
                .get("/api/users/admin")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + oldId + "' }", org.hamcrest.Matchers.nullValue())
                .body("find { it.id == '" + newId + "' }.firstname", equalTo("Updated"))
                .body(
                        "find { it.id == '" + newId + "' }.tags",
                        org.hamcrest.Matchers.containsInAnyOrder("keep", "more"));
    }

    @Test
    @TestSecurity(user = "manager", roles = "MANAGER")
    void resolveImportConflicts_ManagerForbidden() {
        given().contentType("application/json")
                .body("[]")
                .when()
                .post("/api/users/admin/import/resolve")
                .then()
                .statusCode(403);
    }

    @jakarta.inject.Inject
    de.felixhertweck.seatreservation.model.repository.UserRepository userRepository;

    @jakarta.inject.Inject
    de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository refreshTokenRepository;

    @jakarta.inject.Inject
    de.felixhertweck.seatreservation.usermanagement.service.UserService userService;

    @Test
    void testDeleteUser_WithAssociatedEntities_Success() {
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
