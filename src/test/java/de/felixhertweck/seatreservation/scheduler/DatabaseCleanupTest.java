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
package de.felixhertweck.seatreservation.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.RefreshToken;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.LoginAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DatabaseCleanupTest {

    @Inject DatabaseCleanup cleanupScheduler;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject RefreshTokenRepository refreshTokenRepository;

    @Inject LoginAttemptRepository loginAttemptRepository;

    @Inject UserRepository userRepository;

    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        emailVerificationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();

        // Use existing test user from import-test.sql (user with id=3)
        testUser = userRepository.findById(3L);
        assertNotNull(testUser, "Test user with id=3 should exist from import-test.sql");
    }

    @Test
    @Transactional
    void testCleanupExpiredEmailVerifications() {
        // Use different users for expired and valid verifications (One-to-One relationship)
        User user1 = userRepository.findById(1L); // admin user
        User user2 = userRepository.findById(2L); // manager user

        // Create expired email verification for user1
        EmailVerification expiredVerification =
                new EmailVerification(user1, "123456", Instant.now().minus(1, ChronoUnit.HOURS));
        emailVerificationRepository.persist(expiredVerification);

        // Create non-expired email verification for user2
        EmailVerification validVerification =
                new EmailVerification(user2, "654321", Instant.now().plus(1, ChronoUnit.HOURS));
        emailVerificationRepository.persist(validVerification);

        // Verify both exist
        assertEquals(2, emailVerificationRepository.count());

        // Run cleanup
        cleanupScheduler.cleanupExpiredEmailVerifications();

        // Verify only expired one was deleted
        assertEquals(1, emailVerificationRepository.count());
        EmailVerification remaining = emailVerificationRepository.findAll().firstResult();
        assertEquals("654321", remaining.getToken());
    }

    @Test
    @Transactional
    void testCleanupExpiredRefreshTokens() {
        // Create token expired more than 1 day ago (should be deleted)
        RefreshToken oldExpiredToken =
                new RefreshToken(
                        "old-hash",
                        testUser,
                        Instant.now().minus(3, ChronoUnit.DAYS),
                        Instant.now().minus(2, ChronoUnit.DAYS));
        refreshTokenRepository.persist(oldExpiredToken);

        // Create token expired less than 1 day ago (should NOT be deleted - grace period)
        RefreshToken recentExpiredToken =
                new RefreshToken(
                        "recent-hash",
                        testUser,
                        Instant.now().minus(2, ChronoUnit.HOURS),
                        Instant.now().minus(1, ChronoUnit.HOURS));
        refreshTokenRepository.persist(recentExpiredToken);

        // Create valid token (should NOT be deleted)
        RefreshToken validToken =
                new RefreshToken(
                        "valid-hash",
                        testUser,
                        Instant.now(),
                        Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.persist(validToken);

        // Verify all three exist
        assertEquals(3, refreshTokenRepository.count());

        // Run cleanup
        cleanupScheduler.cleanupExpiredRefreshTokens();

        // Verify only the old expired token was deleted
        assertEquals(2, refreshTokenRepository.count());

        // Verify the remaining tokens are the recent expired and valid ones
        assertTrue(
                refreshTokenRepository.findAll().stream()
                        .anyMatch(t -> t.getTokenHash().equals("recent-hash")));
        assertTrue(
                refreshTokenRepository.findAll().stream()
                        .anyMatch(t -> t.getTokenHash().equals("valid-hash")));
        assertFalse(
                refreshTokenRepository.findAll().stream()
                        .anyMatch(t -> t.getTokenHash().equals("old-hash")));
    }

    @Test
    @Transactional
    void testManualCleanupExpiredEmailVerifications() {
        // Create expired email verification
        EmailVerification expiredVerification =
                new EmailVerification(testUser, "123456", Instant.now().minus(1, ChronoUnit.HOURS));
        emailVerificationRepository.persist(expiredVerification);

        assertEquals(1, emailVerificationRepository.count());

        // Run manual cleanup
        int deletedCount = cleanupScheduler.manualCleanupExpiredEmailVerifications();

        assertEquals(1, deletedCount);
        assertEquals(0, emailVerificationRepository.count());
    }

    @Test
    @Transactional
    void testManualCleanupExpiredRefreshTokens() {
        // Create token expired more than 1 day ago
        RefreshToken oldExpiredToken =
                new RefreshToken(
                        "old-hash",
                        testUser,
                        Instant.now().minus(3, ChronoUnit.DAYS),
                        Instant.now().minus(2, ChronoUnit.DAYS));
        refreshTokenRepository.persist(oldExpiredToken);

        assertEquals(1, refreshTokenRepository.count());

        // Run manual cleanup
        int deletedCount = cleanupScheduler.manualCleanupExpiredRefreshTokens();

        assertEquals(1, deletedCount);
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testCleanupWithNoExpiredEntries() {
        // Create only valid entries
        EmailVerification validVerification =
                new EmailVerification(testUser, "123456", Instant.now().plus(1, ChronoUnit.HOURS));
        emailVerificationRepository.persist(validVerification);

        RefreshToken validToken =
                new RefreshToken(
                        "valid-hash",
                        testUser,
                        Instant.now(),
                        Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.persist(validToken);

        // Run cleanup
        cleanupScheduler.cleanupExpiredEmailVerifications();
        cleanupScheduler.cleanupExpiredRefreshTokens();

        // Verify nothing was deleted
        assertEquals(1, emailVerificationRepository.count());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testCleanupOldLoginAttempts() {
        // Create login attempts older than 30 days (should be deleted)
        loginAttemptRepository.recordAttempt(testUser, false);
        loginAttemptRepository.recordAttempt(testUser, false);

        // Create recent login attempts (should NOT be deleted)
        loginAttemptRepository.recordAttempt(testUser, true);
        loginAttemptRepository.recordAttempt(testUser, false);

        // Verify all four exist
        assertEquals(4, loginAttemptRepository.count());

        // Manually set two attempts to be older than 30 days
        loginAttemptRepository.update(
                "attemptTime = ?1 where id in (select id from LoginAttempt order by id limit 2)",
                Instant.now().minus(31, ChronoUnit.DAYS));

        // Run cleanup
        cleanupScheduler.cleanupOldLoginAttempts();

        // Verify only the old attempts were deleted
        assertEquals(2, loginAttemptRepository.count());
    }

    @Test
    @Transactional
    void testManualCleanupOldLoginAttempts() {
        // Create old login attempts (older than 30 days)
        loginAttemptRepository.recordAttempt(testUser, false);
        loginAttemptRepository.recordAttempt(testUser, false);

        // Manually set attempts to be older than 30 days
        loginAttemptRepository.update(
                "attemptTime = ?1 where true", Instant.now().minus(31, ChronoUnit.DAYS));

        assertEquals(2, loginAttemptRepository.count());

        // Run manual cleanup
        cleanupScheduler.manualCleanupOldLoginAttempts();

        // Verify all old attempts were deleted
        assertEquals(0, loginAttemptRepository.count());
    }

    @Test
    @Transactional
    void testCleanupOldLoginAttemptsWithNoOldEntries() {
        // Create only recent login attempts
        loginAttemptRepository.recordAttempt(testUser, true);
        loginAttemptRepository.recordAttempt(testUser, false);
        loginAttemptRepository.recordAttempt(testUser, true);

        assertEquals(3, loginAttemptRepository.count());

        // Run cleanup
        cleanupScheduler.cleanupOldLoginAttempts();

        // Verify nothing was deleted
        assertEquals(3, loginAttemptRepository.count());
    }
}
