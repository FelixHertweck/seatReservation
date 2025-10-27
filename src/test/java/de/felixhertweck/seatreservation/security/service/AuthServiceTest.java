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
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
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
    }

    @Test
    void testAuthenticateSuccess() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String salt = "randomSalt"; // Mock salt
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        User authenticatedUser = authService.authenticate(username, password);

        assertNotNull(authenticatedUser);
        assertEquals(username, authenticatedUser.getUsername());
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
        String salt = "randomSalt"; // Mock salt
        String passwordHash = BcryptUtil.bcryptHash(correctPassword + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(username, wrongPassword),
                        "Expected AuthenticationFailedException for wrong password");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + username));
    }

    @Test
    void testAuthenticateWithEmptyPassword() {
        String username = "testuser";
        String password = "";
        String salt = "randomSalt";
        String validPasswordHash = BcryptUtil.bcryptHash("validPassword" + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(validPasswordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(username, password),
                        "Expected AuthenticationFailedException for empty password");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + username));
    }

    @Test
    void testAuthenticateWithInvalidHash() {
        String username = "testuser";
        String password = "password";
        String salt = "validSalt";

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("invalidHash"); // Invalid bcrypt hash format
        user.setPasswordSalt(salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        // Should throw RuntimeException when BcryptUtil fails to parse invalid hash
        assertThrows(
                RuntimeException.class,
                () -> authService.authenticate(username, password),
                "Expected RuntimeException for invalid hash format");
    }

    @Test
    void testAuthenticateSpecialCharactersInPassword() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "p@ssw0rd!#$%^&*()";
        String salt = "randomSalt";
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        User authenticatedUser = authService.authenticate(username, password);

        assertNotNull(authenticatedUser);
        assertEquals(username, authenticatedUser.getUsername());
    }
}
