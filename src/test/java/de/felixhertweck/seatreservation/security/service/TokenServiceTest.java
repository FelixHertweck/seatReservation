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
package de.felixhertweck.seatreservation.security.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.NewCookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.RefreshToken;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@QuarkusTest
public class TokenServiceTest {

    @Inject TokenService tokenService;

    @Inject RefreshTokenRepository refreshTokenRepository;

    @Inject UserRepository userRepository;

    @InjectMock JWTParser jwtParser;

    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up existing refresh tokens
        refreshTokenRepository.deleteAll();

        // Use existing test user from import-test.sql
        testUser = userRepository.findById(1L);
        assertNotNull(testUser, "Test user should exist");

        // Reset mocks
        Mockito.reset(jwtParser);
    }

    @Test
    void generateToken_ValidTokenContent() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>(Arrays.asList("USER", "ADMIN")));

        try (MockedStatic<Jwt> mockedJwt = Mockito.mockStatic(Jwt.class)) {
            JwtClaimsBuilder claimsBuilder = mock(JwtClaimsBuilder.class);

            mockedJwt.when(() -> Jwt.upn(user.getUsername())).thenReturn(claimsBuilder);
            when(claimsBuilder.groups(user.getRoles())).thenReturn(claimsBuilder);
            when(claimsBuilder.claim(Claims.email, user.getEmail())).thenReturn(claimsBuilder);
            when(claimsBuilder.issuedAt(any())).thenReturn(claimsBuilder);
            when(claimsBuilder.expiresIn(any(Duration.class))).thenReturn(claimsBuilder);
            when(claimsBuilder.sign()).thenReturn("mockedToken");

            tokenService.generateToken(user);

            mockedJwt.verify(() -> Jwt.upn(user.getUsername()));
            Mockito.verify(claimsBuilder).groups(user.getRoles());
            Mockito.verify(claimsBuilder).claim(Claims.email, user.getEmail());
        }
    }

    @Test
    void generateToken_NullEmail_UsesEmptyString() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail(null); // Null email
        user.setRoles(new HashSet<>(Collections.singletonList("USER")));

        try (MockedStatic<Jwt> mockedJwt = Mockito.mockStatic(Jwt.class)) {
            JwtClaimsBuilder claimsBuilder = mock(JwtClaimsBuilder.class);

            mockedJwt.when(() -> Jwt.upn(user.getUsername())).thenReturn(claimsBuilder);
            when(claimsBuilder.groups(user.getRoles())).thenReturn(claimsBuilder);
            when(claimsBuilder.claim(Claims.email, "")).thenReturn(claimsBuilder);
            when(claimsBuilder.issuedAt(any())).thenReturn(claimsBuilder);
            when(claimsBuilder.expiresIn(any(Duration.class))).thenReturn(claimsBuilder);
            when(claimsBuilder.sign()).thenReturn("mockedToken");

            tokenService.generateToken(user);

            // Verify that empty string is used when email is null
            Mockito.verify(claimsBuilder).claim(Claims.email, "");
        }
    }

    @Test
    void generateToken_EmptyRoles_HandlesCorrectly() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>()); // Empty roles

        try (MockedStatic<Jwt> mockedJwt = Mockito.mockStatic(Jwt.class)) {
            JwtClaimsBuilder claimsBuilder = mock(JwtClaimsBuilder.class);

            mockedJwt.when(() -> Jwt.upn(user.getUsername())).thenReturn(claimsBuilder);
            when(claimsBuilder.groups(user.getRoles())).thenReturn(claimsBuilder);
            when(claimsBuilder.claim(Claims.email, user.getEmail())).thenReturn(claimsBuilder);
            when(claimsBuilder.issuedAt(any())).thenReturn(claimsBuilder);
            when(claimsBuilder.expiresIn(any(Duration.class))).thenReturn(claimsBuilder);
            when(claimsBuilder.sign()).thenReturn("mockedToken");

            tokenService.generateToken(user);

            Mockito.verify(claimsBuilder).groups(user.getRoles());
        }
    }

    @Test
    void createNewJwtCookie_EmptyToken() {
        String token = "";

        var cookie = tokenService.createNewJwtCookie(token, "jwt");

        assertNotNull(cookie);
        assertEquals("jwt", cookie.getName());
        assertEquals("", cookie.getValue());
    }

    @Test
    void createNewJwtCookie_NullToken() {
        String token = null;

        var cookie = tokenService.createNewJwtCookie(token, "jwt");

        assertNotNull(cookie);
        assertEquals("jwt", cookie.getName());
        assertNull(cookie.getValue());
    }

    @Test
    void generateToken_IntegrationTest_WithRealJwt() {
        // Integration test that actually calls the real Jwt library
        User user = new User();
        user.setUsername("integrationTestUser");
        user.setEmail("integration@test.com");
        user.setRoles(new HashSet<>(Arrays.asList("USER", "ADMIN")));

        // Don't mock Jwt - use the real implementation
        String token = tokenService.generateToken(user);

        // Verify token is actually generated
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Basic validation that it looks like a JWT (3 parts separated by dots)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT should have 3 parts separated by dots");
    }

    @Test
    void generateToken_IntegrationTest_WithNullEmail() {
        User user = new User();
        user.setUsername("testUserNullEmail");
        user.setEmail(null);
        user.setRoles(new HashSet<>(Collections.singletonList("USER")));

        String token = tokenService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length);
    }

    @Test
    void generateToken_IntegrationTest_WithEmptyRoles() {
        User user = new User();
        user.setUsername("testUserEmptyRoles");
        user.setEmail("empty@roles.com");
        user.setRoles(new HashSet<>());

        String token = tokenService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length);
    }

    @Test
    void createNewNullCookie_WithHttpOnly() {
        NewCookie cookie = tokenService.createNewNullCookie("testCookie", true);

        assertNotNull(cookie);
        assertEquals("testCookie", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure()); // Based on default config
    }

    @Test
    void createNewNullCookie_WithoutHttpOnly() {
        NewCookie cookie = tokenService.createNewNullCookie("testCookie", false);

        assertNotNull(cookie);
        assertEquals("testCookie", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals(0, cookie.getMaxAge());
        assertFalse(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
    }

    @Test
    void createNewNullCookie_DeletesCookie() {
        // Test that the cookie is created to delete itself (maxAge=0)
        NewCookie cookie = tokenService.createNewNullCookie("jwt", true);

        assertEquals(0, cookie.getMaxAge());
        assertEquals("", cookie.getValue());
    }

    // ==================== Tests for Refresh Token Functionality ====================

    @Test
    @Transactional
    void testGenerateRefreshToken() {
        // When
        String refreshTokenJwt = tokenService.generateRefreshToken(testUser);

        // Then
        assertNotNull(refreshTokenJwt);
        assertFalse(refreshTokenJwt.isEmpty());

        // Verify JWT structure (header.payload.signature)
        String[] tokenParts = refreshTokenJwt.split("\\.");
        assertEquals(3, tokenParts.length, "JWT should have 3 parts");

        // Verify token was persisted to database
        assertEquals(1, refreshTokenRepository.count());
        RefreshToken storedToken = refreshTokenRepository.listAll().get(0);
        assertNotNull(storedToken);
        assertEquals(testUser.id, storedToken.getUser().id);
        assertNotNull(storedToken.getTokenHash());
        assertNotNull(storedToken.getCreatedAt());
        assertNotNull(storedToken.getExpiresAt());
    }

    @Test
    @Transactional
    void testGenerateRefreshToken_CreatesUniqueTokens() {
        // When - Generate multiple tokens
        String token1 = tokenService.generateRefreshToken(testUser);
        String token2 = tokenService.generateRefreshToken(testUser);
        String token3 = tokenService.generateRefreshToken(testUser);

        // Then - All tokens should be unique
        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        assertNotEquals(token1, token3);

        // Verify all stored in database
        assertEquals(3, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testValidateRefreshToken_ValidToken() throws Exception {
        // Given - Generate a valid refresh token
        String refreshTokenJwt = tokenService.generateRefreshToken(testUser);

        // Mock JWT parser to return the token
        RefreshToken storedToken = refreshTokenRepository.listAll().get(0);
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(storedToken.id.toString());

        // Extract the actual token value from the JWT (we need to decode it)
        // For testing, we'll mock the token_value claim
        when(mockJwt.getClaim("token_value")).thenReturn("valid-token-value");
        when(jwtParser.parse(refreshTokenJwt)).thenReturn(mockJwt);

        // Update the stored token hash to match
        storedToken.setTokenHash(
                io.quarkus.elytron.security.common.BcryptUtil.bcryptHash("valid-token-value"));
        refreshTokenRepository.persist(storedToken);

        // When
        User validatedUser = tokenService.validateRefreshToken(refreshTokenJwt);

        // Then
        assertNotNull(validatedUser);
        assertEquals(testUser.id, validatedUser.id);
    }

    @Test
    @Transactional
    void testValidateRefreshToken_ExpiredToken() throws Exception {
        // Given - Generate and then expire a token
        String refreshTokenJwt = tokenService.generateRefreshToken(testUser);
        RefreshToken storedToken = refreshTokenRepository.listAll().get(0);

        // Expire the token
        storedToken.setExpiresAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS));
        refreshTokenRepository.persist(storedToken);

        // Mock JWT parser
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(storedToken.id.toString());
        when(mockJwt.getClaim("token_value")).thenReturn("some-token-value");
        when(jwtParser.parse(refreshTokenJwt)).thenReturn(mockJwt);

        // When/Then
        assertThrows(
                JwtInvalidException.class,
                () -> tokenService.validateRefreshToken(refreshTokenJwt),
                "Should throw JwtInvalidException for expired token");
    }

    @Test
    void testValidateRefreshToken_InvalidJwt() throws Exception {
        // Given
        String invalidJwt = "invalid.jwt.token";

        // Mock JWT parser to throw exception
        when(jwtParser.parse(invalidJwt)).thenThrow(new RuntimeException("Invalid JWT"));

        // When/Then
        assertThrows(
                JwtInvalidException.class,
                () -> tokenService.validateRefreshToken(invalidJwt),
                "Should throw JwtInvalidException for invalid JWT");
    }

    @Test
    @Transactional
    void testValidateRefreshToken_TokenNotFoundInDatabase() throws Exception {
        // Given - Mock a JWT with non-existent token ID
        String fakeJwt = "fake.jwt.token";
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn("999999");
        when(mockJwt.getClaim("token_value")).thenReturn("some-value");
        when(jwtParser.parse(fakeJwt)).thenReturn(mockJwt);

        // When/Then
        assertThrows(
                JwtInvalidException.class,
                () -> tokenService.validateRefreshToken(fakeJwt),
                "Should throw JwtInvalidException when token not found in database");
    }

    @Test
    @Transactional
    void testValidateRefreshToken_InvalidTokenValue() throws Exception {
        // Given - Generate token but validate with wrong value
        String refreshTokenJwt = tokenService.generateRefreshToken(testUser);
        RefreshToken storedToken = refreshTokenRepository.listAll().get(0);

        // Mock JWT parser with wrong token value
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(storedToken.id.toString());
        when(mockJwt.getClaim("token_value")).thenReturn("wrong-token-value");
        when(jwtParser.parse(refreshTokenJwt)).thenReturn(mockJwt);

        // When/Then
        assertThrows(
                JwtInvalidException.class,
                () -> tokenService.validateRefreshToken(refreshTokenJwt),
                "Should throw JwtInvalidException for wrong token value");
    }

    @Test
    @Transactional
    void testCreateNewRefreshTokenCookie() {
        // Given
        String refreshToken = tokenService.generateRefreshToken(testUser);

        // Mock JWT parsing to avoid real JWT validation
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getExpirationTime())
                .thenReturn(System.currentTimeMillis() / 1000 + 604800); // 7 days
        try {
            when(jwtParser.parse(refreshToken)).thenReturn(mockJwt);
        } catch (Exception e) {
            fail("Failed to mock JWT parser");
        }

        // When
        NewCookie cookie = null;
        try {
            cookie = tokenService.createNewRefreshTokenCookie(refreshToken, "refreshToken");
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Then
        assertNotNull(cookie);
        assertEquals("refreshToken", cookie.getName());
        assertEquals(refreshToken, cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure()); // Based on default config
        assertTrue(cookie.getMaxAge() > 0);
    }

    @Test
    @Transactional
    void testCreateStatusCookie() {
        // Given
        String refreshToken = tokenService.generateRefreshToken(testUser);

        // Mock JWT parsing to avoid real JWT validation
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getExpirationTime())
                .thenReturn(System.currentTimeMillis() / 1000 + 604800); // 7 days
        try {
            when(jwtParser.parse(refreshToken)).thenReturn(mockJwt);
        } catch (Exception e) {
            fail("Failed to mock JWT parser");
        }

        // When
        NewCookie cookie = null;
        try {
            cookie = tokenService.createStatusCookie(refreshToken, "refreshToken_expiration");
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        // Then
        assertNotNull(cookie);
        assertEquals("refreshToken_expiration", cookie.getName());
        assertFalse(cookie.getValue().isEmpty());
        assertEquals("/", cookie.getPath());
        assertFalse(cookie.isHttpOnly()); // Status cookies are not httpOnly
        assertTrue(cookie.getMaxAge() > 0);
    }

    @Test
    @Transactional
    void testLogoutAllDevices() {
        // Given - Create multiple refresh tokens for the user
        tokenService.generateRefreshToken(testUser);
        tokenService.generateRefreshToken(testUser);
        tokenService.generateRefreshToken(testUser);

        assertEquals(3, refreshTokenRepository.count());

        // When
        tokenService.logoutAllDevices(testUser);

        // Then
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testLogoutAllDevices_DoesNotAffectOtherUsers() {
        // Given - Create tokens for two different users
        User otherUser = userRepository.findById(2L);
        assertNotNull(otherUser);

        tokenService.generateRefreshToken(testUser);
        tokenService.generateRefreshToken(testUser);
        tokenService.generateRefreshToken(otherUser);

        assertEquals(3, refreshTokenRepository.count());

        // When - Logout only testUser
        tokenService.logoutAllDevices(testUser);

        // Then - Only otherUser's token remains
        assertEquals(1, refreshTokenRepository.count());
        RefreshToken remainingToken = refreshTokenRepository.listAll().get(0);
        assertEquals(otherUser.id, remainingToken.getUser().id);
    }

    @Test
    @Transactional
    void testDeleteRefreshToken_ValidToken() throws Exception {
        // Given - Generate a refresh token
        String refreshToken = tokenService.generateRefreshToken(testUser);
        assertEquals(1, refreshTokenRepository.count());

        // When - Delete the refresh token
        tokenService.deleteRefreshToken(refreshToken);

        // Then - Token should be deleted from database
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteRefreshToken_NullToken() throws Exception {
        // Given - Generate a token first
        tokenService.generateRefreshToken(testUser);
        assertEquals(1, refreshTokenRepository.count());

        // When - Delete with null token
        tokenService.deleteRefreshToken(null);

        // Then - Token should still exist
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteRefreshToken_EmptyToken() throws Exception {
        // Given - Generate a token first
        tokenService.generateRefreshToken(testUser);
        assertEquals(1, refreshTokenRepository.count());

        // When - Delete with empty token
        tokenService.deleteRefreshToken("");

        // Then - Token should still exist
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteRefreshToken_InvalidToken() throws Exception {
        // Given - Generate a token first
        tokenService.generateRefreshToken(testUser);
        assertEquals(1, refreshTokenRepository.count());

        // When - Delete with invalid token (should not throw exception)
        tokenService.deleteRefreshToken("invalid.token.format");

        // Then - Original token should still exist
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteRefreshToken_NonExistentToken() throws Exception {
        // Given - Generate a token but mock JWT parser to return non-existent ID
        String refreshToken = tokenService.generateRefreshToken(testUser);
        assertEquals(1, refreshTokenRepository.count());

        // Create another token with a non-existent ID
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn("999999");
        when(jwtParser.parse("fake.token.jwt")).thenReturn(mockJwt);

        // When - Delete the non-existent token
        tokenService.deleteRefreshToken("fake.token.jwt");

        // Then - Original token should still exist
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteRefreshToken_OnlyDeletesSpecifiedToken() throws Exception {
        // Given - Generate multiple tokens
        String token1 = tokenService.generateRefreshToken(testUser);
        String token2 = tokenService.generateRefreshToken(testUser);
        String token3 = tokenService.generateRefreshToken(testUser);
        assertEquals(3, refreshTokenRepository.count());

        // When - Delete only the second token
        tokenService.deleteRefreshToken(token2);

        // Then - Only two tokens should remain
        assertEquals(2, refreshTokenRepository.count());
    }
}
