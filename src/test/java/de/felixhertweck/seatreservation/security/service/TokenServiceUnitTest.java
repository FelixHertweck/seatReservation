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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceUnitTest {

    @Mock private JWTParser parser;

    @InjectMocks private TokenService tokenService;

    @BeforeEach
    void setUp() {
        // Mockito annotations will initialize parser and inject it into tokenService
    }

    @Test
    void validateRefreshToken_ThrowsJwtInvalidException_WhenParserThrowsParseException()
            throws ParseException {
        // Given
        String dummyToken = "invalid-jwt-token";
        when(parser.parse(dummyToken)).thenThrow(new ParseException("Parsing failed"));

        // When & Then
        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> tokenService.validateRefreshToken(dummyToken));
        assertEquals("Invalid JWT", exception.getMessage());
        assertEquals(ParseException.class, exception.getCause().getClass());
    }

    @Test
    void validateRefreshToken_ThrowsJwtInvalidException_WhenParserThrowsRuntimeException()
            throws ParseException {
        // Given
        String dummyToken = "invalid-jwt-token";
        when(parser.parse(dummyToken)).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> tokenService.validateRefreshToken(dummyToken));
        assertEquals("Invalid JWT", exception.getMessage());
        assertEquals(RuntimeException.class, exception.getCause().getClass());
    }

    @Test
    void validateRefreshToken_ThrowsJwtInvalidException_WhenTokenIdIsMissing()
            throws ParseException {
        // Given
        String dummyToken = "valid-format-jwt";
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(parser.parse(dummyToken)).thenReturn(jwt);
        when(jwt.getClaim("token_id")).thenReturn(null);

        // When & Then
        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> tokenService.validateRefreshToken(dummyToken));
        assertEquals("Invalid token_id in JWT", exception.getMessage());
    }

    @Test
    void validateRefreshToken_ThrowsJwtInvalidException_WhenTokenIdIsInvalidFormat()
            throws ParseException {
        // Given
        String dummyToken = "valid-format-jwt";
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(parser.parse(dummyToken)).thenReturn(jwt);
        when(jwt.getClaim("token_id")).thenReturn("not-a-uuid");

        // When & Then
        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> tokenService.validateRefreshToken(dummyToken));
        assertEquals("Invalid token_id in JWT", exception.getMessage());
        assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
    }

    @Test
    void validateRefreshToken_ThrowsJwtInvalidException_WhenTokenValueIsMissing()
            throws ParseException {
        // Given
        String dummyToken = "valid-format-jwt";
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(parser.parse(dummyToken)).thenReturn(jwt);
        when(jwt.getClaim("token_id")).thenReturn("123e4567-e89b-12d3-a456-426614174000");
        when(jwt.getClaim("token_value")).thenReturn(null);

        // When & Then
        JwtInvalidException exception =
                assertThrows(
                        JwtInvalidException.class,
                        () -> tokenService.validateRefreshToken(dummyToken));
        assertEquals("Missing token_value in JWT", exception.getMessage());
    }
}
