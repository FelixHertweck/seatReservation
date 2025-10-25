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

import java.util.List;

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
    void testAuthenticateSuccessWithEmail() throws AuthenticationFailedException {
        String email = "test@example.com";
        String password = "testpassword";
        String salt = "randomSalt"; // Mock salt
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(user));

        User authenticatedUser = authService.authenticate(email, password);

        assertNotNull(authenticatedUser);
        assertEquals(email, authenticatedUser.getEmail());
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
    void testAuthenticateFailureEmailNotFound() {
        String email = "nonexistent@example.com";
        String password = "anypassword";

        when(userRepository.findByEmail(email)).thenReturn(null);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(email, password),
                        "Expected AuthenticationFailedException for email not found");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + email));
    }

    @Test
    void testAuthenticateWithEmailWrongPassword() {
        String email = "test@example.com";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String salt = "randomSalt";
        String passwordHash = BcryptUtil.bcryptHash(correctPassword + salt);

        User user = new User();
        user.setEmail(email);
        user.setUsername("testuser");
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(user));

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(email, wrongPassword),
                        "Expected AuthenticationFailedException for wrong password with email");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + email));
    }

    @Test
    void testAuthenticateWithEmailIdentifier() throws AuthenticationFailedException {
        // Test that email addresses are correctly identified as emails
        String email = "user.name+tag@example.com";
        String password = "password";
        String salt = "randomSalt";
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setEmail(email);
        user.setUsername("username");
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(user));

        User result = authService.authenticate(email, password);
        assertNotNull(result);

        // Verify email lookup was used, not username lookup
        Mockito.verify(userRepository).findAllByEmail(email);
        Mockito.verify(userRepository, Mockito.never()).findByUsername(email);
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
    void testAuthenticateIdentifierDetection() throws AuthenticationFailedException {
        // Test email detection
        String email = "user@domain.com";
        String password = "password";
        String salt = "salt";
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(user));

        User result = authService.authenticate(email, password);
        assertNotNull(result);

        // Verify email method was called, not username
        Mockito.verify(userRepository).findAllByEmail(email);
        Mockito.verify(userRepository, Mockito.never()).findByUsername(email);
    }

    @Test
    void testAuthenticateUsernameIdentification() throws AuthenticationFailedException {
        // Test username detection (no @ symbol)
        String username = "testuser";
        String password = "password";
        String salt = "salt";
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        User result = authService.authenticate(username, password);
        assertNotNull(result);

        // Verify username method was called, not email
        Mockito.verify(userRepository).findByUsername(username);
        Mockito.verify(userRepository, Mockito.never()).findByEmail(username);
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
