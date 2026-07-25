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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.RefreshToken;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TokenServiceUnitTest {

    @InjectMocks private TokenService tokenService;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private JWTParser parser;

    @Test
    void testValidateRefreshToken_ParseException() throws ParseException {
        String token = "invalid.token";
        when(parser.parse(token)).thenThrow(new ParseException("Mock ParseException"));

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals("Invalid JWT", exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_RuntimeException() throws ParseException {
        String token = "invalid.token";
        when(parser.parse(token)).thenThrow(new RuntimeException("Mock RuntimeException"));

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals("Invalid JWT", exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_MissingTokenId() throws ParseException {
        String token = "valid.token";
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(null);
        when(parser.parse(token)).thenReturn(mockJwt);

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals("Invalid token_id in JWT", exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_MalformedTokenId() throws ParseException {
        String token = "valid.token";
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn("not-a-valid-uuid");
        when(parser.parse(token)).thenReturn(mockJwt);

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals("Invalid token_id in JWT", exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_MissingTokenValue() throws ParseException {
        String token = "valid.token";
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(UUID.randomUUID().toString());
        when(mockJwt.getClaim("token_value")).thenReturn(null);
        when(parser.parse(token)).thenReturn(mockJwt);

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals("Missing token_value in JWT", exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_TokenNotFound() throws ParseException {
        String token = "valid.token";
        UUID tokenId = UUID.randomUUID();
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(tokenId.toString());
        when(mockJwt.getClaim("token_value")).thenReturn("some-value");
        when(parser.parse(token)).thenReturn(mockJwt);

        when(refreshTokenRepository.findById(tokenId)).thenReturn(null);

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals("Refresh token not found for token_id: " + tokenId, exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_BcryptMismatch() throws ParseException {
        String token = "valid.token";
        UUID tokenId = UUID.randomUUID();
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(tokenId.toString());
        when(mockJwt.getClaim("token_value")).thenReturn("incorrect-value");
        when(parser.parse(token)).thenReturn(mockJwt);

        User mockUser = new User();
        RefreshToken storedToken =
                new RefreshToken(
                        BcryptUtil.bcryptHash("correct-value"),
                        mockUser,
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(1)));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(storedToken);

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals(
                "Refresh token is invalid or expired for token_id: " + tokenId,
                exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_Expired() throws ParseException {
        String token = "valid.token";
        UUID tokenId = UUID.randomUUID();
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(tokenId.toString());
        when(mockJwt.getClaim("token_value")).thenReturn("value");
        when(parser.parse(token)).thenReturn(mockJwt);

        User mockUser = new User();
        RefreshToken storedToken =
                new RefreshToken(
                        BcryptUtil.bcryptHash("value"),
                        mockUser,
                        Instant.now().minus(Duration.ofDays(2)),
                        Instant.now().minus(Duration.ofDays(1)));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(storedToken);

        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> {
                            tokenService.validateRefreshToken(token);
                        });

        assertEquals(
                "Refresh token is invalid or expired for token_id: " + tokenId,
                exception.getMessage());
    }

    @Test
    void testValidateRefreshToken_Success() throws ParseException, JwtInvalidException {
        String token = "valid.token";
        UUID tokenId = UUID.randomUUID();
        JsonWebToken mockJwt = mock(JsonWebToken.class);
        when(mockJwt.getClaim("token_id")).thenReturn(tokenId.toString());
        when(mockJwt.getClaim("token_value")).thenReturn("value");
        when(parser.parse(token)).thenReturn(mockJwt);

        User mockUser = new User();
        mockUser.id = UUID.randomUUID();
        RefreshToken storedToken =
                new RefreshToken(
                        BcryptUtil.bcryptHash("value"),
                        mockUser,
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(1)));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(storedToken);

        User validatedUser = tokenService.validateRefreshToken(token);

        assertNotNull(validatedUser);
        assertEquals(mockUser.id, validatedUser.id);
    }
}
