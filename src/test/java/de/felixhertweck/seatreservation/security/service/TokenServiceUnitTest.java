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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TokenServiceUnitTest {

    @Mock private JWTParser parser;

    @InjectMocks private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService.cookieSecure = false;
        tokenService.expirationMinutes = 60;
        tokenService.refreshExpirationDays = 7;
    }

    @Test
    void createNewRefreshTokenCookie_WhenParserThrowsParseException_ThrowsJwtInvalidException()
            throws Exception {
        String invalidToken = "invalid-token";
        when(parser.parse(invalidToken)).thenThrow(new ParseException("Invalid JWT"));

        assertThrows(
                JwtInvalidException.class,
                () -> tokenService.createNewRefreshTokenCookie(invalidToken, "refreshToken"),
                "Should throw JwtInvalidException when ParseException is caught in"
                        + " getExpirationFromJwt");
    }

    @Test
    void createStatusCookie_WhenParserThrowsParseException_ThrowsJwtInvalidException()
            throws Exception {
        String invalidToken = "invalid-token";
        when(parser.parse(invalidToken)).thenThrow(new ParseException("Invalid JWT"));

        assertThrows(
                JwtInvalidException.class,
                () -> tokenService.createStatusCookie(invalidToken, "refreshToken_expiration"),
                "Should throw JwtInvalidException when ParseException is caught in"
                        + " getExpirationFromJwt");
    }
}
