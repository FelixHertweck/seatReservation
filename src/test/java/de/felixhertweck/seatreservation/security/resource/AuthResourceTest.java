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
package de.felixhertweck.seatreservation.security.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AuthResourceTest {

    @InjectMock AuthService authService;
    @InjectMock TokenService tokenService;

    @BeforeEach
    void setUp() {
        Mockito.reset(authService, tokenService);
    }

    @Test
    void testLoginSuccess() throws AuthenticationFailedException {
        String identifier = "testuser";
        String password = "testpassword";
        String token = "mockedToken123";

        Mockito.when(authService.authenticate(identifier, password)).thenReturn(token);

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setIdentifier(identifier);
        loginRequest.setPassword(password);

        // Mock the NewCookie creation
        NewCookie mockedCookie =
                new NewCookie.Builder("jwt")
                        .value(token)
                        .path("/")
                        .maxAge(60 * 60) // 60 minutes
                        .httpOnly(true)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createNewJwtCookie(token)).thenReturn(mockedCookie);

        Mockito.when(tokenService.getExpirationMinutes())
                .thenReturn(60L); // Mock the expirationMinutes for 60 minutes

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .header(
                        "Set-Cookie",
                        containsString("jwt=")) // Check for the presence of the jwt cookie in the
                // Set-Cookie header
                .header(
                        "Set-Cookie",
                        containsString(
                                "Max-Age="
                                        + (tokenService.getExpirationMinutes()
                                                * 60))); // Check maxAge of the jwt cookie in the
        // Set-Cookie header
    }

    @Test
    void testLoginFailureWrongCredentials() throws AuthenticationFailedException {
        String identifier = "testuser";
        String password = "wrongpassword";
        String errorMessage = String.format("Failed to authenticate user: %s", identifier);

        Mockito.when(authService.authenticate(identifier, password))
                .thenThrow(new AuthenticationFailedException(errorMessage));

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setIdentifier(identifier);
        loginRequest.setPassword(password);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode())
                .body("message", equalTo(errorMessage));
    }

    @Test
    void login_BadRequest_MissingCredentials() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setIdentifier(null); // Explicitly set to null
        loginRequest.setPassword(null); // Explicitly set to null

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        LoginRequestDTO loginRequestEmpty = new LoginRequestDTO();
        loginRequestEmpty.setIdentifier(""); // Empty identifier
        loginRequestEmpty.setPassword("password");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestEmpty)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        LoginRequestDTO loginRequestEmpty2 = new LoginRequestDTO();
        loginRequestEmpty2.setIdentifier("username");
        loginRequestEmpty2.setPassword(""); // Empty password

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestEmpty2)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void testRegisterSuccess() throws DuplicateUserException, InvalidUserException {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("newuser");
        registerRequest.setFirstname("New");
        registerRequest.setLastname("User");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("securepassword");

        Mockito.doAnswer(invocation -> null)
                .when(authService)
                .register(
                        registerRequest.getUsername(),
                        registerRequest.getEmail(),
                        registerRequest.getPassword(),
                        registerRequest.getFirstname(),
                        registerRequest.getLastname());

        given().contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void testRegisterFailureDuplicateUsername()
            throws DuplicateUserException, InvalidUserException {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("existinguser");
        registerRequest.setFirstname("Existing");
        registerRequest.setLastname("User");
        registerRequest.setEmail("existinguser@example.com");
        registerRequest.setPassword("securepassword");

        String errorMessage = "User with username existinguser already exists.";
        Mockito.doThrow(new DuplicateUserException(errorMessage))
                .when(authService)
                .register(
                        registerRequest.getUsername(),
                        registerRequest.getEmail(),
                        registerRequest.getPassword(),
                        registerRequest.getFirstname(),
                        registerRequest.getLastname());

        given().contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("message", equalTo(errorMessage));
    }

    @Test
    void testRegisterFailureInvalidData() throws DuplicateUserException, InvalidUserException {
        // Test case 1: Missing username
        RegisterRequestDTO registerRequest1 = new RegisterRequestDTO();
        registerRequest1.setUsername(null);
        registerRequest1.setFirstname("Test");
        registerRequest1.setLastname("User");
        registerRequest1.setEmail("test@example.com");
        registerRequest1.setPassword("password123");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest1)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test case 2: Password too short
        RegisterRequestDTO registerRequest3 = new RegisterRequestDTO();
        registerRequest3.setUsername("testuser3");
        registerRequest3.setFirstname("Test");
        registerRequest3.setLastname("User");
        registerRequest3.setEmail("test3@example.com");
        registerRequest3.setPassword("short"); // Less than 8 characters

        given().contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest3)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
