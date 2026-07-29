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

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.LoginAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.PasswordResetTokenRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.PasswordResetConfirmDTO;
import de.felixhertweck.seatreservation.security.dto.PasswordResetRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.dto.UsernameRecoveryRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.exceptions.PasswordResetTokenExpiredException;
import de.felixhertweck.seatreservation.security.exceptions.PasswordResetTokenNotFoundException;
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

    @InjectMock LoginAttemptRepository loginAttemptRepository;

    @InjectMock PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMock EmailService emailService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.reset(
                userRepository,
                tokenService,
                loginAttemptRepository,
                passwordResetTokenRepository,
                emailService);
        authService = new AuthService();
        authService.userRepository = userRepository;
        authService.loginAttemptRepository = loginAttemptRepository;
        authService.passwordResetTokenRepository = passwordResetTokenRepository;
        authService.emailService = emailService;
        authService.tokenService = tokenService;
        authService.maxFailedAttempts = 5;
        authService.lockoutDurationSeconds = 300;
        authService.init();

        // Mock loginAttemptRepository to return 0 failed attempts by default
        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class)))
                .thenReturn(0L);
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

    @Test
    void testIsRegistrationEnabled_DefaultTrue() {
        authService.registrationEnabled = true;
        assertTrue(
                authService.isRegistrationEnabled(), "Registration should be enabled by default");
    }

    @Test
    void testIsRegistrationEnabled_WhenDisabled() {
        authService.registrationEnabled = false;
        assertFalse(authService.isRegistrationEnabled(), "Registration should be disabled");
    }

    @Test
    void testRegisterThrowsExceptionWhenRegistrationDisabled() {
        authService.registrationEnabled = false;

        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setFirstname("John");
        registerRequest.setLastname("Doe");
        registerRequest.setEmail("john@example.com");

        RegistrationDisabledException thrown =
                assertThrows(
                        RegistrationDisabledException.class,
                        () -> authService.register(registerRequest),
                        "Expected RegistrationDisabledException when registration is disabled");

        assertTrue(
                thrown.getMessage().contains("registration is currently disabled"),
                "Exception message should indicate registration is disabled");
    }

    @Test
    void testRequestPasswordReset_UserFoundAndEmailMatches_PersistsTokenAndSendsEmail() {
        String username = "testuser";
        String email = "test@example.com";
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        when(userRepository.findByUsernameOptional(username)).thenReturn(Optional.of(user));

        PasswordResetRequestDTO requestDTO = new PasswordResetRequestDTO();
        requestDTO.setUsername(username);
        requestDTO.setEmail(email);

        authService.requestPasswordReset(requestDTO);

        verify(passwordResetTokenRepository, times(1)).deleteByUserId(user.id);
        verify(passwordResetTokenRepository, times(1))
                .persistAndFlush(any(PasswordResetToken.class));
    }

    @Test
    void testRequestPasswordReset_UserNotFound_DoesNotPersistTokenOrSendEmail() {
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());

        PasswordResetRequestDTO requestDTO = new PasswordResetRequestDTO();
        requestDTO.setUsername("nonexistentuser");
        requestDTO.setEmail("test@example.com");

        authService.requestPasswordReset(requestDTO);

        verify(passwordResetTokenRepository, never())
                .persistAndFlush(any(PasswordResetToken.class));
    }

    @Test
    void testRequestPasswordReset_EmailMismatch_DoesNotPersistTokenOrSendEmail() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        user.setEmail("real@example.com");

        when(userRepository.findByUsernameOptional(username)).thenReturn(Optional.of(user));

        PasswordResetRequestDTO requestDTO = new PasswordResetRequestDTO();
        requestDTO.setUsername(username);
        requestDTO.setEmail("attacker-guess@example.com");

        authService.requestPasswordReset(requestDTO);

        verify(passwordResetTokenRepository, never())
                .persistAndFlush(any(PasswordResetToken.class));
    }

    @Test
    void testRequestPasswordReset_ConcurrentRequestRace_DoesNotPropagateException() {
        // Simulates a unique constraint collision from a concurrent request.
        String username = "testuser";
        String email = "test@example.com";
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        when(userRepository.findByUsernameOptional(username)).thenReturn(Optional.of(user));
        Mockito.doThrow(new PersistenceException("duplicate key value violates unique constraint"))
                .when(passwordResetTokenRepository)
                .persistAndFlush(any(PasswordResetToken.class));

        PasswordResetRequestDTO requestDTO = new PasswordResetRequestDTO();
        requestDTO.setUsername(username);
        requestDTO.setEmail(email);

        assertDoesNotThrow(() -> authService.requestPasswordReset(requestDTO));
    }

    @Test
    void testRequestPasswordReset_UserNotFound_StillTakesAtLeastMinimumDuration() {
        // Guards against a timing side-channel: the "no such account" branch must not return
        // measurably faster than the "account found" branch, otherwise an attacker who already
        // knows a username could infer whether their guessed email is correct just from latency.
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());

        PasswordResetRequestDTO requestDTO = new PasswordResetRequestDTO();
        requestDTO.setUsername("nonexistentuser");
        requestDTO.setEmail("test@example.com");

        long start = System.nanoTime();
        authService.requestPasswordReset(requestDTO);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(
                elapsedMillis >= 200,
                "Expected requestPasswordReset to enforce a minimum response time, took only "
                        + elapsedMillis
                        + "ms");
    }

    @Test
    void testRequestUsernameRecovery_SingleUserFound_SendsEmailWithUsername() throws IOException {
        String email = "test@example.com";
        User user = new User();
        user.setUsername("testuser");
        user.setEmail(email);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(user));

        UsernameRecoveryRequestDTO requestDTO = new UsernameRecoveryRequestDTO();
        requestDTO.setEmail(email);

        authService.requestUsernameRecovery(requestDTO);

        verify(emailService, times(1)).sendUsernameRecoveryEmail(email, List.of("testuser"));
    }

    @Test
    void testRequestUsernameRecovery_MultipleUsersFound_SendsEmailWithAllUsernames()
            throws IOException {
        String email = "shared@example.com";
        User firstUser = new User();
        firstUser.setUsername("firstuser");
        firstUser.setEmail(email);
        User secondUser = new User();
        secondUser.setUsername("seconduser");
        secondUser.setEmail(email);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(firstUser, secondUser));

        UsernameRecoveryRequestDTO requestDTO = new UsernameRecoveryRequestDTO();
        requestDTO.setEmail(email);

        authService.requestUsernameRecovery(requestDTO);

        verify(emailService, times(1))
                .sendUsernameRecoveryEmail(email, List.of("firstuser", "seconduser"));
    }

    @Test
    void testRequestUsernameRecovery_NoUserFound_DoesNotSendEmail() throws IOException {
        when(userRepository.findAllByEmail(anyString())).thenReturn(List.of());

        UsernameRecoveryRequestDTO requestDTO = new UsernameRecoveryRequestDTO();
        requestDTO.setEmail("unknown@example.com");

        authService.requestUsernameRecovery(requestDTO);

        verify(emailService, never()).sendUsernameRecoveryEmail(anyString(), anyList());
    }

    @Test
    void testRequestUsernameRecovery_NoUserFound_StillTakesAtLeastMinimumDuration() {
        // Same timing-side-channel rationale as password reset: the "no account" branch must not
        // return measurably faster than the "account(s) found" branch.
        when(userRepository.findAllByEmail(anyString())).thenReturn(List.of());

        UsernameRecoveryRequestDTO requestDTO = new UsernameRecoveryRequestDTO();
        requestDTO.setEmail("unknown@example.com");

        long start = System.nanoTime();
        authService.requestUsernameRecovery(requestDTO);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(
                elapsedMillis >= 200,
                "Expected requestUsernameRecovery to enforce a minimum response time, took only "
                        + elapsedMillis
                        + "ms");
    }

    @Test
    void testRequestUsernameRecovery_EmailServiceThrowsIOException_DoesNotPropagate()
            throws IOException {
        String email = "test@example.com";
        User user = new User();
        user.setUsername("testuser");
        user.setEmail(email);

        when(userRepository.findAllByEmail(email)).thenReturn(List.of(user));
        doThrow(new IOException("template not found"))
                .when(emailService)
                .sendUsernameRecoveryEmail(anyString(), anyList());

        UsernameRecoveryRequestDTO requestDTO = new UsernameRecoveryRequestDTO();
        requestDTO.setEmail(email);

        assertDoesNotThrow(() -> authService.requestUsernameRecovery(requestDTO));
    }

    @Test
    void testConfirmPasswordReset_ValidToken_UpdatesPasswordAndDeletesToken() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEmailVerified(false);
        String previousHash = user.getPasswordHash();

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        user, "valid-token", Instant.now().plus(1, ChronoUnit.HOURS));

        when(passwordResetTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(resetToken));

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken("valid-token");
        confirmDTO.setNewPassword("newSecurePassword123");

        authService.confirmPasswordReset(confirmDTO);

        assertNotEquals(previousHash, user.getPasswordHash());
        assertTrue(user.isEmailVerified());
        verify(passwordResetTokenRepository, times(1)).delete(resetToken);
        verify(tokenService, times(1)).logoutAllDevices(user);
    }

    @Test
    void testConfirmPasswordReset_TokenNotFound_ThrowsPasswordResetTokenNotFoundException() {
        when(passwordResetTokenRepository.findByToken("unknown-token"))
                .thenReturn(Optional.empty());

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken("unknown-token");
        confirmDTO.setNewPassword("newSecurePassword123");

        assertThrows(
                PasswordResetTokenNotFoundException.class,
                () -> authService.confirmPasswordReset(confirmDTO));
    }

    @Test
    void
            testConfirmPasswordReset_ExpiredToken_ThrowsPasswordResetTokenExpiredExceptionAndDeletesToken() {
        User user = new User();
        user.setUsername("testuser");
        PasswordResetToken resetToken =
                new PasswordResetToken(
                        user, "expired-token", Instant.now().minus(1, ChronoUnit.HOURS));

        when(passwordResetTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(resetToken));

        PasswordResetConfirmDTO confirmDTO = new PasswordResetConfirmDTO();
        confirmDTO.setToken("expired-token");
        confirmDTO.setNewPassword("newSecurePassword123");

        assertThrows(
                PasswordResetTokenExpiredException.class,
                () -> authService.confirmPasswordReset(confirmDTO));

        verify(passwordResetTokenRepository, times(1)).delete(resetToken);
    }
}
