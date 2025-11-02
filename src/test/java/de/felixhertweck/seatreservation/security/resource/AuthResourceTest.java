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

import java.util.Collections;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AuthResourceTest {

    @InjectMock AuthService authService;
    @InjectMock TokenService tokenService;
    @InjectMock UserSecurityContext userSecurityContext;

    @Inject UserRepository userRepository;

    @Inject RefreshTokenRepository refreshTokenRepository;

    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        Mockito.reset(authService, tokenService, userSecurityContext);

        // Clean up refresh tokens
        refreshTokenRepository.deleteAll();

        // Use existing test user from import-test.sql
        testUser = userRepository.findById(1L); // admin user
        assertNotNull(testUser, "Test user should exist from import-test.sql");
    }

    @Test
    void testLoginSuccess() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String token = "mockedToken123";

        // Create a mock User object
        User mockUser = Mockito.mock(User.class);
        Mockito.when(mockUser.getUsername()).thenReturn(username);

        // Mock the authenticate method to return the mock User
        Mockito.when(authService.authenticate(username, password)).thenReturn(mockUser);

        // Mock token generation
        Mockito.when(tokenService.generateToken(mockUser)).thenReturn(token);
        Mockito.when(tokenService.generateRefreshToken(mockUser)).thenReturn("refreshToken123");

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
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
        Mockito.when(tokenService.createNewJwtCookie(token, "jwt")).thenReturn(mockedCookie);

        NewCookie mockedRefreshCookie =
                new NewCookie.Builder("refreshToken")
                        .value("refreshToken123")
                        .path("/")
                        .maxAge(60 * 60)
                        .httpOnly(true)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createNewRefreshTokenCookie("refreshToken123", "refreshToken"))
                .thenReturn(mockedRefreshCookie);

        NewCookie mockedRefreshTokenExpirationCookie =
                new NewCookie.Builder("refreshToken_expiration")
                        .value("expirationValue123")
                        .path("/")
                        .maxAge(60 * 60)
                        .httpOnly(false)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createStatusCookie("refreshToken123", "refreshToken_expiration"))
                .thenReturn(mockedRefreshTokenExpirationCookie);

        Mockito.when(tokenService.getExpirationMinutes())
                .thenReturn(60L); // Mock the expirationMinutes for 60 minutes

        io.restassured.response.Response response =
                given().contentType(MediaType.APPLICATION_JSON)
                        .body(loginRequest)
                        .when()
                        .post("/api/auth/login");

        response.then().statusCode(Response.Status.OK.getStatusCode());

        // Verify all three cookies are present in the response
        String setCookieHeaders = response.getHeaders().getValues("Set-Cookie").toString();
        assertTrue(setCookieHeaders.contains("jwt=" + token));
        assertTrue(setCookieHeaders.contains("refreshToken=refreshToken123"));
        assertTrue(setCookieHeaders.contains("refreshToken_expiration=expirationValue123"));
    }

    @Test
    void testLoginFailureWrongCredentials() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "wrongpassword";
        String errorMessage = String.format("Failed to authenticate user: %s", username);

        Mockito.when(authService.authenticate(username, password))
                .thenThrow(new AuthenticationFailedException(errorMessage));

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
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
        loginRequest.setUsername(null); // Explicitly set to null
        loginRequest.setPassword(null); // Explicitly set to null

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        LoginRequestDTO loginRequestEmpty = new LoginRequestDTO();
        loginRequestEmpty.setUsername(""); // Empty username
        loginRequestEmpty.setPassword("password");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestEmpty)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        LoginRequestDTO loginRequestEmpty2 = new LoginRequestDTO();
        loginRequestEmpty2.setUsername("username");
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

        // Create a mock User object
        User mockUser = Mockito.mock(User.class);
        Mockito.when(mockUser.getUsername()).thenReturn("newuser");

        // Mock the register method to return the mock User
        Mockito.when(authService.register(Mockito.any(RegisterRequestDTO.class)))
                .thenReturn(mockUser);

        // Mock token generation
        Mockito.when(tokenService.generateToken(mockUser)).thenReturn("accessToken123");
        Mockito.when(tokenService.generateRefreshToken(mockUser)).thenReturn("refreshToken123");

        // Mock cookie creation
        NewCookie mockedAccessCookie =
                new NewCookie.Builder("jwt")
                        .value("accessToken123")
                        .path("/")
                        .maxAge(60 * 60)
                        .httpOnly(true)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createNewJwtCookie("accessToken123", "jwt"))
                .thenReturn(mockedAccessCookie);

        NewCookie mockedRefreshCookie =
                new NewCookie.Builder("refreshToken")
                        .value("refreshToken123")
                        .path("/")
                        .maxAge(60 * 60)
                        .httpOnly(true)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createNewRefreshTokenCookie("refreshToken123", "refreshToken"))
                .thenReturn(mockedRefreshCookie);

        NewCookie mockedRefreshTokenExpirationCookie =
                new NewCookie.Builder("refreshToken_expiration")
                        .value("expirationValue123")
                        .path("/")
                        .maxAge(60 * 60)
                        .httpOnly(false)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createStatusCookie("refreshToken123", "refreshToken_expiration"))
                .thenReturn(mockedRefreshTokenExpirationCookie);

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
                .register(Mockito.any(RegisterRequestDTO.class));

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

    @Test
    void testRefreshToken_Success() throws Exception {
        String refreshToken = "validRefreshToken123";
        String newJwtToken = "newJwtToken456";
        String newRefreshToken = "newRefreshToken789";

        // Mock token service methods
        Mockito.when(tokenService.validateRefreshToken(refreshToken)).thenReturn(testUser);
        Mockito.when(tokenService.generateToken(testUser)).thenReturn(newJwtToken);
        Mockito.when(tokenService.generateRefreshToken(testUser)).thenReturn(newRefreshToken);

        // Mock cookie creation
        NewCookie mockedJwtCookie =
                new NewCookie.Builder("jwt")
                        .value(newJwtToken)
                        .path("/")
                        .maxAge(60 * 60)
                        .httpOnly(true)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createNewJwtCookie(newJwtToken, "jwt"))
                .thenReturn(mockedJwtCookie);

        NewCookie mockedRefreshCookie =
                new NewCookie.Builder("refreshToken")
                        .value(newRefreshToken)
                        .path("/")
                        .maxAge(60 * 60 * 24 * 7)
                        .httpOnly(true)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createNewRefreshTokenCookie(newRefreshToken, "refreshToken"))
                .thenReturn(mockedRefreshCookie);

        NewCookie mockedExpirationCookie =
                new NewCookie.Builder("refreshToken_expiration")
                        .value("expirationValue")
                        .path("/")
                        .maxAge(60 * 60 * 24 * 7)
                        .httpOnly(false)
                        .secure(true)
                        .build();
        Mockito.when(tokenService.createStatusCookie(newRefreshToken, "refreshToken_expiration"))
                .thenReturn(mockedExpirationCookie);

        io.restassured.response.Response response =
                given().cookie("refreshToken", refreshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/auth/refresh");

        response.then().statusCode(Response.Status.OK.getStatusCode());

        // Verify cookies are present
        String setCookieHeaders = response.getHeaders().getValues("Set-Cookie").toString();
        assertTrue(
                setCookieHeaders.contains("jwt=" + newJwtToken),
                "JWT cookie not found or incorrect. Headers: " + setCookieHeaders);
        assertTrue(
                setCookieHeaders.contains("refreshToken=" + newRefreshToken),
                "Refresh token cookie not found or incorrect. Headers: " + setCookieHeaders);
        assertTrue(
                setCookieHeaders.contains("refreshToken_expiration=expirationValue"),
                "Refresh token expiration cookie not found or incorrect. Headers: "
                        + setCookieHeaders);
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        // Mock token service to throw exception for invalid token
        Mockito.when(tokenService.validateRefreshToken(invalidToken))
                .thenThrow(new JwtInvalidException("Invalid refresh token"));

        io.restassured.response.Response response =
                given().cookie("refreshToken", invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/auth/refresh");

        response.then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testRefreshToken_MissingToken() {
        io.restassured.response.Response response =
                given().contentType(MediaType.APPLICATION_JSON).when().post("/api/auth/refresh");

        response.then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void testRefreshToken_EmptyToken() throws Exception {
        String emptyToken = "";

        // Mock token service to throw exception for empty token
        Mockito.when(tokenService.validateRefreshToken(emptyToken))
                .thenThrow(new JwtInvalidException("Empty refresh token"));

        given().cookie("refreshToken", emptyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @Transactional
    @TestSecurity(
            user = "admin",
            roles = {"ADMIN"})
    void testLogoutAllDevices_Success() throws Exception {
        // Mock the security context to return the test user when getCurrentUser is called
        Mockito.when(userSecurityContext.getCurrentUser()).thenReturn(testUser);
        Mockito.doNothing().when(tokenService).logoutAllDevices(testUser);

        // Mock cookie clearing
        NewCookie clearedJwtCookie =
                new NewCookie.Builder("jwt").value("").path("/").maxAge(0).httpOnly(true).build();
        Mockito.when(tokenService.createNewNullCookie("jwt", true)).thenReturn(clearedJwtCookie);

        NewCookie clearedRefreshCookie =
                new NewCookie.Builder("refreshToken")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .build();
        Mockito.when(tokenService.createNewNullCookie("refreshToken", true))
                .thenReturn(clearedRefreshCookie);

        NewCookie clearedExpirationCookie =
                new NewCookie.Builder("refreshToken_expiration")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(false)
                        .build();
        Mockito.when(tokenService.createNewNullCookie("refreshToken_expiration", false))
                .thenReturn(clearedExpirationCookie);

        io.restassured.response.Response response =
                given().contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/auth/logoutAllDevices");

        response.then().statusCode(Response.Status.OK.getStatusCode());

        // Verify cookies are cleared (maxAge=0)
        String setCookieHeaders = response.getHeaders().getValues("Set-Cookie").toString();
        assertTrue(
                setCookieHeaders.contains("jwt=") && setCookieHeaders.contains("Max-Age=0"),
                "JWT cookie should be cleared");
        assertTrue(
                setCookieHeaders.contains("refreshToken=")
                        && setCookieHeaders.contains("Max-Age=0"),
                "Refresh token cookie should be cleared");
    }

    @Test
    void testLogoutAllDevices_WithoutAuth_Unauthorized() {
        // Try to call logoutAllDevices without authentication
        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/auth/logoutAllDevices")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @TestSecurity(
            user = "testuser",
            roles = {"USER"})
    void testLogout_Success() {
        Mockito.when(userSecurityContext.getCurrentUser()).thenReturn(testUser);
        // Mock cookie clearing
        NewCookie clearedJwtCookie =
                new NewCookie.Builder("jwt").value("").path("/").maxAge(0).httpOnly(true).build();
        Mockito.when(tokenService.createNewNullCookie("jwt", true)).thenReturn(clearedJwtCookie);

        NewCookie clearedRefreshCookie =
                new NewCookie.Builder("refreshToken")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .build();
        Mockito.when(tokenService.createNewNullCookie("refreshToken", true))
                .thenReturn(clearedRefreshCookie);

        NewCookie clearedExpirationCookie =
                new NewCookie.Builder("refreshToken_expiration")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(false)
                        .build();
        Mockito.when(tokenService.createNewNullCookie("refreshToken_expiration", false))
                .thenReturn(clearedExpirationCookie);

        io.restassured.response.Response response =
                given().cookie("refreshToken", "someRefreshToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/auth/logout");

        response.then().statusCode(Response.Status.OK.getStatusCode());

        // Verify cookies are cleared
        String setCookieHeaders = response.getHeaders().getValues("Set-Cookie").toString();
        assertTrue(
                setCookieHeaders.contains("jwt=") && setCookieHeaders.contains("Max-Age=0"),
                "JWT cookie should be cleared");
        assertTrue(
                setCookieHeaders.contains("refreshToken=")
                        && setCookieHeaders.contains("Max-Age=0"),
                "Refresh token cookie should be cleared");
        assertTrue(
                setCookieHeaders.contains("refreshToken_expiration=")
                        && setCookieHeaders.contains("Max-Age=0"),
                "Refresh token expiration cookie should be cleared");
    }

    @Test
    @TestSecurity(
            user = "testuser",
            roles = {"USER"})
    void testLogout_NoRefreshTokenCookie() {
        Mockito.when(userSecurityContext.getCurrentUser()).thenReturn(testUser);
        // Mock cookie clearing logic as it will still be called
        NewCookie clearedJwtCookie =
                new NewCookie.Builder("jwt").value("").path("/").maxAge(0).httpOnly(true).build();
        Mockito.when(tokenService.createNewNullCookie("jwt", true)).thenReturn(clearedJwtCookie);

        NewCookie clearedRefreshCookie =
                new NewCookie.Builder("refreshToken")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .build();
        Mockito.when(tokenService.createNewNullCookie("refreshToken", true))
                .thenReturn(clearedRefreshCookie);

        NewCookie clearedExpirationCookie =
                new NewCookie.Builder("refreshToken_expiration")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(false)
                        .build();
        Mockito.when(tokenService.createNewNullCookie("refreshToken_expiration", false))
                .thenReturn(clearedExpirationCookie);

        // When no cookies are sent
        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void testRefreshToken_ServiceThrowsException() throws Exception {
        String refreshToken = "validRefreshToken123";

        // Mock token service to throw a generic exception
        Mockito.when(tokenService.validateRefreshToken(refreshToken))
                .thenThrow(new RuntimeException("Database connection failed"));

        given().cookie("refreshToken", refreshToken)
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void testGetRegistrationStatus_RegistrationEnabled() {
        Mockito.when(authService.isRegistrationEnabled()).thenReturn(true);

        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/api/auth/registration-status")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("enabled", equalTo(true));
    }

    @Test
    void testGetRegistrationStatus_RegistrationDisabled() {
        Mockito.when(authService.isRegistrationEnabled()).thenReturn(false);

        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/api/auth/registration-status")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("enabled", equalTo(false));
    }

    @Test
    void testGetRegistrationStatus_IsPublicEndpoint() {
        // Should not require authentication
        Mockito.when(authService.isRegistrationEnabled()).thenReturn(true);

        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/api/auth/registration-status")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void testRegisterWhenDisabled_Returns403Forbidden() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("newuser");
        request.setPassword("SecurePassword123!");
        request.setFirstname("John");
        request.setLastname("Doe");
        request.setEmail("john@example.com");

        // Mock the authService.register to throw RegistrationDisabledException
        Mockito.doThrow(
                        new de.felixhertweck.seatreservation.common.exception
                                .RegistrationDisabledException(
                                "User registration is currently disabled"))
                .when(authService)
                .register(Mockito.any(RegisterRequestDTO.class));

        given().contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode())
                .body("message", containsString("registration is currently disabled"));
    }

    @Test
    void testGetRegistrationStatus_RegistrationEnabledByDefault() {
        // By default in @QuarkusTest, registration is enabled
        Mockito.when(authService.isRegistrationEnabled()).thenReturn(true);

        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/api/auth/registration-status")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("enabled", equalTo(true));
    }

    /** Test profile that disables registration. */
    public static class DisabledRegistrationProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("registration.enabled", "false");
        }
    }
}
