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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.LoginAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.exceptions.AccountLockedException;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class LoginRateLimitingTest {

    @InjectMock UserRepository userRepository;

    @InjectMock LoginAttemptRepository loginAttemptRepository;

    AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository, loginAttemptRepository);
        authService = new AuthService();
        authService.userRepository = userRepository;
        authService.loginAttemptRepository = loginAttemptRepository;
        authService.init();
    }

    @Test
    void testSuccessfulLoginRecordsAttempt() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String salt = "randomSalt";
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenReturn(0L);
        when(userRepository.findByUsername(username)).thenReturn(user);

        User authenticatedUser = authService.authenticate(username, password);

        assertNotNull(authenticatedUser);
        assertEquals(username, authenticatedUser.getUsername());
        verify(loginAttemptRepository, times(1)).recordAttempt(any(User.class), Mockito.eq(true));
    }

    @Test
    void testFailedLoginRecordsAttempt() {
        String username = "testuser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String salt = "randomSalt";
        String passwordHash = BcryptUtil.bcryptHash(correctPassword + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenReturn(0L);
        when(userRepository.findByUsername(username)).thenReturn(user);

        assertThrows(
                AuthenticationFailedException.class,
                () -> authService.authenticate(username, wrongPassword));

        verify(loginAttemptRepository, times(1)).recordAttempt(any(User.class), Mockito.eq(false));
    }

    @Test
    void testNoLockoutBelowTier1() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String salt = "randomSalt";
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPasswordSalt(salt);

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenReturn(2L);
        when(userRepository.findByUsername(username)).thenReturn(user);

        User authenticatedUser = authService.authenticate(username, password);

        assertNotNull(authenticatedUser);
    }

    @Test
    void testTier1LockoutAt3Attempts() {
        String username = "testuser";
        String password = "anypassword";

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenAnswer(
                        invocation -> {
                            Instant windowStart = invocation.getArgument(1);
                            Instant now = Instant.now();
                            // 15 minute window check returns 3 attempts
                            if (windowStart.isAfter(now.minusSeconds(16 * 60))) {
                                return 3L;
                            }
                            return 0L;
                        });

        AccountLockedException thrown =
                assertThrows(
                        AccountLockedException.class,
                        () -> authService.authenticate(username, password));

        assertNotNull(thrown.getRetryAfter());
        assertTrue(thrown.getRetryAfter().isAfter(Instant.now()));
        assertTrue(
                thrown.getRetryAfter().isBefore(Instant.now().plusSeconds(35)),
                "Tier 1 lockout should be around 30s");
    }

    @Test
    void testTier2LockoutAt5AttemptsOverrulesTier1() {
        String username = "testuser";
        String password = "anypassword";

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenAnswer(
                        invocation -> {
                            Instant windowStart = invocation.getArgument(1);
                            Instant now = Instant.now();
                            if (windowStart.isAfter(now.minusSeconds(16 * 60))) {
                                return 5L;
                            }
                            return 0L;
                        });

        AccountLockedException thrown =
                assertThrows(
                        AccountLockedException.class,
                        () -> authService.authenticate(username, password));

        assertNotNull(thrown.getRetryAfter());
        assertTrue(
                thrown.getRetryAfter().isAfter(Instant.now().plusSeconds(30)),
                "Tier 2 duration (2m) should exceed Tier 1 (30s)");
        assertTrue(
                thrown.getRetryAfter().isBefore(Instant.now().plusSeconds(125)),
                "Tier 2 duration should be within 2 minutes");
    }

    @Test
    void testNonExistentUserRecordsFailedAttempt() {
        String username = "nonexistentuser";
        String password = "anypassword";

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenReturn(0L);
        when(userRepository.findByUsername(username)).thenReturn(null);

        assertThrows(
                AuthenticationFailedException.class,
                () -> authService.authenticate(username, password));

        verify(loginAttemptRepository, times(1)).recordAttempt(username, false);
    }
}
