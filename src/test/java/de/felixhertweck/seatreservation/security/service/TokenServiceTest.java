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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.User;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        tokenService.expirationMinutes = 60; // Default value for testing
    }

    @Test
    void testGenerateToken() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>(Arrays.asList("USER", "MANAGER")));

        try (MockedStatic<Jwt> mockedJwt = Mockito.mockStatic(Jwt.class)) {
            JwtClaimsBuilder claimsBuilder = mock(JwtClaimsBuilder.class);

            mockedJwt.when(() -> Jwt.upn(user.getUsername())).thenReturn(claimsBuilder);
            when(claimsBuilder.groups(user.getRoles())).thenReturn(claimsBuilder);
            when(claimsBuilder.claim(Claims.email, user.getEmail())).thenReturn(claimsBuilder);
            when(claimsBuilder.expiresIn(any(Duration.class))).thenReturn(claimsBuilder);
            when(claimsBuilder.sign()).thenReturn("mockedToken");

            String token = tokenService.generateToken(user);

            assertNotNull(token);
            assertEquals("mockedToken", token);

            mockedJwt.verify(() -> Jwt.upn(user.getUsername()));
            Mockito.verify(claimsBuilder).groups(user.getRoles());
            Mockito.verify(claimsBuilder).claim(Claims.email, user.getEmail());
            Mockito.verify(claimsBuilder)
                    .expiresIn(Duration.ofMinutes(tokenService.expirationMinutes));
            Mockito.verify(claimsBuilder).sign();
        }
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
            when(claimsBuilder.expiresIn(any(Duration.class))).thenReturn(claimsBuilder);
            when(claimsBuilder.sign()).thenReturn("mockedToken");

            tokenService.generateToken(user);

            mockedJwt.verify(() -> Jwt.upn(user.getUsername()));
            Mockito.verify(claimsBuilder).groups(user.getRoles());
            Mockito.verify(claimsBuilder).claim(Claims.email, user.getEmail());
        }
    }

    @Test
    void generateToken_TokenExpiration() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>(Collections.singletonList("USER")));

        try (MockedStatic<Jwt> mockedJwt = Mockito.mockStatic(Jwt.class)) {
            JwtClaimsBuilder claimsBuilder = mock(JwtClaimsBuilder.class);

            mockedJwt.when(() -> Jwt.upn(user.getUsername())).thenReturn(claimsBuilder);
            when(claimsBuilder.groups(user.getRoles())).thenReturn(claimsBuilder);
            when(claimsBuilder.claim(Claims.email, user.getEmail())).thenReturn(claimsBuilder);
            when(claimsBuilder.expiresIn(any(Duration.class))).thenReturn(claimsBuilder);
            when(claimsBuilder.sign()).thenReturn("mockedToken");

            tokenService.generateToken(user);

            Mockito.verify(claimsBuilder)
                    .expiresIn(Duration.ofMinutes(tokenService.expirationMinutes));
        }
    }
}
