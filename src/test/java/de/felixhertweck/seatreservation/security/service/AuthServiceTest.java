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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.AuthenticationFailedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AuthServiceTest {

    @InjectMock UserRepository userRepository;

    @InjectMock TokenService tokenService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository, tokenService);
        authService = new AuthService();
        authService.userRepository = userRepository;
        authService.tokenService = tokenService;
    }

    @Test
    void testAuthenticateSuccess() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String passwordHash = BcryptUtil.bcryptHash(password);
        String expectedToken = "mockedToken";

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);

        when(userRepository.findByUsername(username)).thenReturn(user);
        when(tokenService.generateToken(user)).thenReturn(expectedToken);

        String actualToken = authService.authenticate(username, password);

        assertEquals(expectedToken, actualToken);
    }

    @Test
    void testAuthenticateFailureUserNotFound() {
        String username = "nonexistentuser";
        String password = "anypassword";

        when(userRepository.findByUsername(username)).thenReturn(null);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(username, password),
                        "Expected AuthenticationFailedException for user not found");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + username));
    }

    @Test
    void testAuthenticateFailureWrongPassword() {
        String username = "testuser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String passwordHash = BcryptUtil.bcryptHash(correctPassword);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);

        when(userRepository.findByUsername(username)).thenReturn(user);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(username, wrongPassword),
                        "Expected AuthenticationFailedException for wrong password");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + username));
    }
}
