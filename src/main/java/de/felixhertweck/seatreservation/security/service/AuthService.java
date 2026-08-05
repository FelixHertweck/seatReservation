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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.LoginAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.PasswordResetTokenRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.PasswordResetConfirmDTO;
import de.felixhertweck.seatreservation.security.dto.PasswordResetRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.dto.UsernameRecoveryRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.AccountLockedException;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.exceptions.PasswordResetTokenExpiredException;
import de.felixhertweck.seatreservation.security.exceptions.PasswordResetTokenNotFoundException;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    private record LockoutTier(int attempts, Duration window, Duration lockoutDuration) {}

    private static final List<LockoutTier> LOGIN_LOCKOUT_TIERS =
            List.of(
                    new LockoutTier(3, Duration.ofMinutes(15), Duration.ofSeconds(30)),
                    new LockoutTier(5, Duration.ofMinutes(15), Duration.ofMinutes(2)),
                    new LockoutTier(10, Duration.ofHours(1), Duration.ofMinutes(15)),
                    new LockoutTier(15, Duration.ofHours(24), Duration.ofHours(1)));

    @Inject UserRepository userRepository;

    @Inject UserService userService;

    @Inject PasswordResetTokenRepository passwordResetTokenRepository;

    @Inject EmailService emailService;

    @Inject LoginAttemptRepository loginAttemptRepository;

    @Inject TokenService tokenService;

    @Inject EmailCooldownService emailCooldownService;

    @ConfigProperty(name = "registration.enabled", defaultValue = "true")
    boolean registrationEnabled;

    private String randomPasswordHash;

    @PostConstruct
    void init() {
        String randomPassword =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(SecurityUtils.generateRandomBytes(32));
        this.randomPasswordHash = BcryptUtil.bcryptHash(randomPassword);
    }

    /**
     * Checks if user registration is enabled.
     *
     * @return true if registration is enabled, false otherwise
     */
    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    /**
     * Authenticates a user with the given username and password.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @return the authenticated User if authentication is successful
     * @throws AuthenticationFailedException if authentication fails
     * @throws AccountLockedException if the account is temporarily locked due to too many failed
     *     attempts
     */
    public User authenticate(String username, String password)
            throws AuthenticationFailedException, AccountLockedException {
        LOG.debugf("Attempting to authenticate user with username: %s", username);

        // Check if account is locked due to failed login attempts
        checkAccountLockout(username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            LOG.warnf("Authentication failed for username %s: User not found.", username);

            // Perform password hash comparison with random hash to mitigate timing attacks
            BcryptUtil.matches(password, randomPasswordHash);

            loginAttemptRepository.recordAttempt(username, false);
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }
        if (user.getPasswordHash() == null) {
            LOG.warnf(
                    "Authentication failed for username %s: passkey-only account without password.",
                    username);

            // Perform password hash comparison with random hash to mitigate timing attacks and
            // avoid leaking that this account has no password set.
            BcryptUtil.matches(password, randomPasswordHash);

            loginAttemptRepository.recordAttempt(user, false);
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }
        if (passwordMatches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            LOG.infof("User %s authenticated successfully.", user.getUsername());
            loginAttemptRepository.recordAttempt(user, true);
            return user;
        }

        LOG.warnf("Authentication failed for username %s: Password mismatch.", username);
        loginAttemptRepository.recordAttempt(user, false);
        throw new AuthenticationFailedException("Failed to authenticate user: " + username);
    }

    /**
     * Checks if the account is temporarily locked due to too many failed login attempts.
     *
     * @param username the username to check
     * @throws AccountLockedException if the account is locked
     */
    private void checkAccountLockout(String username) throws AccountLockedException {
        Instant now = Instant.now();
        Instant strictestRetryAfter = null;

        for (LockoutTier tier : LOGIN_LOCKOUT_TIERS) {
            Instant windowStart = now.minus(tier.window());
            long failedAttempts = loginAttemptRepository.countFailedAttempts(username, windowStart);
            if (failedAttempts >= tier.attempts()) {
                Instant oldest =
                        loginAttemptRepository.getOldestFailedAttemptTime(username, windowStart);
                Instant retryAfter = (oldest != null ? oldest : now).plus(tier.lockoutDuration());
                if (strictestRetryAfter == null || retryAfter.isAfter(strictestRetryAfter)) {
                    strictestRetryAfter = retryAfter;
                }
            }
        }

        if (strictestRetryAfter != null) {
            LOG.warnf(
                    "Account locked for username %s. Remaining lockout time: %s",
                    username, strictestRetryAfter);
            throw new AccountLockedException(
                    "Account temporarily locked due to too many failed login attempts. Please try"
                            + " again later.",
                    strictestRetryAfter);
        }
    }

    public boolean passwordMatches(String password, String passwordSalt, String storedHash) {
        // Passkey-only accounts have no stored password hash and can never match.
        if (storedHash == null) {
            return false;
        }
        // Combine the provided password with the stored salt before hashing for comparison
        return BcryptUtil.matches(password + passwordSalt, storedHash);
    }

    /**
     * Registers a new user with the given registration details.
     *
     * @param registerRequest the registration details
     * @return the registered User
     * @throws DuplicateUserException if the user already exists
     * @throws InvalidUserException if the user details are invalid
     * @throws RegistrationDisabledException if registration is disabled
     */
    public User register(RegisterRequestDTO registerRequest)
            throws DuplicateUserException, InvalidUserException, RegistrationDisabledException {
        LOG.debugf("Attempting to register new user: %s", registerRequest.getUsername());

        if (!registrationEnabled) {
            LOG.warnf(
                    "Registration attempt made for user %s when registration is disabled.",
                    registerRequest.getUsername());
            throw new RegistrationDisabledException("User registration is currently disabled");
        }

        UserCreationDTO userCreationDTO = new UserCreationDTO(registerRequest);

        userService.createUser(userCreationDTO, Set.of(Roles.USER), true);

        User user = userRepository.findByUsername(registerRequest.getUsername());

        if (user == null) {
            LOG.warnf("User %s not found after registration.", registerRequest.getUsername());
            throw new InvalidUserException("User not found: " + registerRequest.getUsername());
        }

        LOG.infof("User %s registered successfully", registerRequest.getUsername());

        return user;
    }

    /**
     * Minimum wall-clock time {@link #requestPasswordReset} takes to return, regardless of whether
     * the account exists. Without this floor, the match/no-match branches take visibly different
     * times (DB writes + email enqueue vs. an immediate return), letting an attacker who already
     * knows a username infer whether their guessed email is correct.
     */
    private static final long MIN_REQUEST_PASSWORD_RESET_DURATION_MILLIS = 250;

    /**
     * Initiates a password reset for the given username/email pair. To prevent account enumeration,
     * this method returns silently (without error) whenever the username/email combination does not
     * match an existing account, and always takes at least {@link
     * #MIN_REQUEST_PASSWORD_RESET_DURATION_MILLIS} to respond so the two cases aren't
     * distinguishable by timing.
     *
     * @param requestDTO the username/email pair identifying the account
     */
    public void requestPasswordReset(PasswordResetRequestDTO requestDTO) {
        long startNanos = System.nanoTime();
        try {
            QuarkusTransaction.requiringNew().run(() -> doRequestPasswordReset(requestDTO));
        } catch (PersistenceException e) {
            // Benign race: concurrent requests can collide on the unique user_id constraint.
            LOG.infof(
                    "Password reset request for username %s raced with a concurrent request;"
                            + " ignoring.",
                    requestDTO.getUsername());
        } finally {
            sleepRemaining(startNanos, MIN_REQUEST_PASSWORD_RESET_DURATION_MILLIS);
        }
    }

    private void doRequestPasswordReset(PasswordResetRequestDTO requestDTO) {
        User user = userRepository.findByUsernameOptional(requestDTO.getUsername()).orElse(null);
        if (user == null
                || user.getEmail() == null
                || !user.getEmail().equalsIgnoreCase(requestDTO.getEmail())) {
            LOG.infof(
                    "Password reset requested for username %s, but user not found or email"
                            + " mismatch.",
                    requestDTO.getUsername());
            return;
        }

        if (emailCooldownService
                .checkAndRecord(EmailCooldownService.Purpose.PASSWORD_RESET, user.getUsername())
                .isPresent()) {
            LOG.infof(
                    "Password reset requested for username %s while on cooldown; ignoring.",
                    user.getUsername());
            return;
        }

        passwordResetTokenRepository.deleteByUserId(user.id);

        String token =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(SecurityUtils.generateRandomBytes(32));
        Instant expirationTime = Instant.now().plus(1, ChronoUnit.HOURS);

        PasswordResetToken resetToken = new PasswordResetToken(user, token, expirationTime);
        // Flush immediately so a unique-constraint collision surfaces here, not on commit.
        passwordResetTokenRepository.persistAndFlush(resetToken);

        try {
            emailService.sendPasswordResetEmail(user, resetToken);
            LOG.infof("Password reset email sent to user %s.", user.getUsername());
        } catch (IOException e) {
            LOG.errorf(e, "Failed to send password reset email to user %s", user.getUsername());
        }
    }

    /**
     * Minimum wall-clock time {@link #requestUsernameRecovery} takes to return, regardless of
     * whether the email address is associated with any account. See {@link
     * #MIN_REQUEST_PASSWORD_RESET_DURATION_MILLIS} for the underlying timing-attack rationale.
     */
    private static final long MIN_REQUEST_USERNAME_RECOVERY_DURATION_MILLIS = 250;

    /**
     * Sends an email listing every username associated with the given email address, if any. Since
     * {@code email} is not unique, this may resolve to zero, one, or several accounts; all matching
     * usernames are relayed in a single email. To prevent enumeration, this method returns silently
     * regardless of whether any account was found, and always takes at least {@link
     * #MIN_REQUEST_USERNAME_RECOVERY_DURATION_MILLIS} to respond.
     *
     * @param requestDTO the email address to look up
     */
    public void requestUsernameRecovery(UsernameRecoveryRequestDTO requestDTO) {
        long startNanos = System.nanoTime();
        try {
            doRequestUsernameRecovery(requestDTO);
        } finally {
            sleepRemaining(startNanos, MIN_REQUEST_USERNAME_RECOVERY_DURATION_MILLIS);
        }
    }

    private void doRequestUsernameRecovery(UsernameRecoveryRequestDTO requestDTO) {
        List<User> users = userRepository.findAllByEmail(requestDTO.getEmail());
        if (users.isEmpty()) {
            LOG.info("Username recovery requested for an email with no associated accounts.");
            return;
        }

        String normalizedEmail = requestDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        if (emailCooldownService
                .checkAndRecord(EmailCooldownService.Purpose.USERNAME_RECOVERY, normalizedEmail)
                .isPresent()) {
            LOG.info("Username recovery requested for email while on cooldown; ignoring.");
            return;
        }

        try {
            emailService.sendUsernameRecoveryEmail(
                    requestDTO.getEmail(), users.stream().map(User::getUsername).toList());
            LOG.info("Username recovery email sent.");
        } catch (IOException e) {
            LOG.error("Failed to send username recovery email.", e);
        }
    }

    private void sleepRemaining(long startNanos, long minMillis) {
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        long remainingMillis = minMillis - elapsedMillis;
        if (remainingMillis > 0) {
            try {
                Thread.sleep(remainingMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sets a new password for the account identified by a valid, unexpired reset token. If the
     * account's email was not yet verified, it is marked as verified, since receiving the reset
     * link proves control over the address.
     *
     * @param confirmDTO the reset token and new password
     * @throws PasswordResetTokenNotFoundException if the token is unknown
     * @throws PasswordResetTokenExpiredException if the token has expired
     */
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDTO confirmDTO) {
        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByToken(confirmDTO.getToken()).orElse(null);
        if (resetToken == null) {
            throw new PasswordResetTokenNotFoundException("Password reset token not found.");
        }
        if (resetToken.getExpirationTime().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new PasswordResetTokenExpiredException("Password reset token expired.");
        }

        User user = resetToken.getUser();

        String salt = generateSalt();
        String passwordHash = BcryptUtil.bcryptHash(confirmDTO.getNewPassword() + salt);

        user.setPasswordSalt(salt);
        user.setPasswordHash(passwordHash);

        if (!Boolean.TRUE.equals(user.isEmailVerified())) {
            user.setEmailVerified(true);
        }

        userRepository.persist(user);
        passwordResetTokenRepository.delete(resetToken);

        // A password reset is commonly triggered by a suspected credential leak, so any
        // session issued under the old password must not remain valid.
        tokenService.logoutAllDevices(user);

        try {
            emailService.sendPasswordChangedNotification(user);
        } catch (IOException e) {
            LOG.errorf(e, "Failed to send password changed notification to %s", user.getEmail());
        }

        LOG.infof("Password successfully reset for user %s.", user.getUsername());
    }

    private String generateSalt() {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(SecurityUtils.generateRandomBytes(16));
    }
}
