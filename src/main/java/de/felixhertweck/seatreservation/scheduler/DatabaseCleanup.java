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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository;
import io.quarkus.scheduler.Scheduled;
import org.jboss.logging.Logger;

/**
 * Scheduled service for automatic database cleanup.
 *
 * <p>This service runs daily.
 */
@ApplicationScoped
public class DatabaseCleanup {

    private static final Logger LOG = Logger.getLogger(DatabaseCleanup.class);

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject RefreshTokenRepository refreshTokenRepository;

    /**
     * Cleans up expired email verification entries.
     *
     * <p>Runs daily at 3:00 AM. Deletes all email verification entries where the expiration time
     * has passed.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3:00 AM
    @Transactional
    public void cleanupExpiredEmailVerifications() {
        LOG.info("Starting scheduled cleanup of expired email verifications.");

        try {
            Instant now = Instant.now();

            // Delete all expired email verification entries
            long deletedCount = emailVerificationRepository.delete("expirationTime < ?1", now);

            if (deletedCount > 0) {
                LOG.infof(
                        "Successfully cleaned up %d expired email verification entries.",
                        deletedCount);
            } else {
                LOG.debug("No expired email verification entries found to clean up.");
            }
        } catch (Exception e) {
            LOG.error("Error during email verification cleanup", e);
        }
    }

    /**
     * Cleans up expired refresh tokens.
     *
     * <p>Runs daily at 3:30 AM. Deletes all refresh tokens that expired more than 1 day ago.
     */
    @Scheduled(cron = "0 30 3 * * ?") // Every day at 3:30 AM
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        LOG.info("Starting scheduled cleanup of expired refresh tokens.");

        try {
            // Delete tokens that expired more than 1 day ago
            Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);

            long deletedCount = refreshTokenRepository.delete("expiresAt < ?1", oneDayAgo);

            if (deletedCount > 0) {
                LOG.infof(
                        "Successfully cleaned up %d expired refresh tokens (older than 1 day).",
                        deletedCount);
            } else {
                LOG.debug("No expired refresh tokens found to clean up.");
            }
        } catch (Exception e) {
            LOG.error("Error during refresh token cleanup", e);
        }
    }

    /**
     * Manual cleanup method for email verifications that can be called programmatically.
     *
     * @return the number of deleted entries
     */
    @Transactional
    public int manualCleanupExpiredEmailVerifications() {
        LOG.info("Manual cleanup of expired email verifications triggered.");
        Instant now = Instant.now();
        long deletedCount = emailVerificationRepository.delete("expirationTime < ?1", now);
        LOG.infof("Manually cleaned up %d expired email verification entries.", deletedCount);
        return (int) deletedCount;
    }

    /**
     * Manual cleanup method for refresh tokens that can be called programmatically.
     *
     * @return the number of deleted entries
     */
    @Transactional
    public int manualCleanupExpiredRefreshTokens() {
        LOG.info("Manual cleanup of expired refresh tokens triggered.");
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        long deletedCount = refreshTokenRepository.delete("expiresAt < ?1", oneDayAgo);
        LOG.infof("Manually cleaned up %d expired refresh tokens.", deletedCount);
        return (int) deletedCount;
    }
}
