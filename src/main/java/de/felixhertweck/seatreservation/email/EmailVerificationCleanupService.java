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
package de.felixhertweck.seatreservation.email;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Service for cleaning up expired email verification entries. Runs scheduled tasks to prevent the
 * database from growing indefinitely.
 */
@ApplicationScoped
public class EmailVerificationCleanupService {

    private static final Logger LOG = Logger.getLogger(EmailVerificationCleanupService.class);

    @Inject EmailVerificationRepository emailVerificationRepository;

    @ConfigProperty(name = "email.verification.cleanup.batch-size", defaultValue = "100")
    int batchSize;

    @ConfigProperty(name = "email.verification.cleanup.enabled", defaultValue = "true")
    boolean cleanupEnabled;

    /**
     * Scheduled cleanup task that runs every 10 minutes. Deletes expired email verification entries
     * in batches to prevent database overload.
     */
    @Scheduled(
            every = "{email.verification.cleanup.interval}",
            identity = "email-verification-cleanup")
    public void cleanupExpiredVerifications() {
        if (!cleanupEnabled) {
            LOG.debug("Email verification cleanup is disabled");
            return;
        }

        try {
            LOG.debug("Starting email verification cleanup task");

            long totalDeleted = 0;
            long deletedInBatch;

            // Process in batches to avoid overwhelming the database
            do {
                deletedInBatch = emailVerificationRepository.deleteExpiredEntriesInBatch(batchSize);
                totalDeleted += deletedInBatch;

                if (deletedInBatch > 0) {
                    LOG.debugf(
                            "Deleted %d expired email verification entries in current batch",
                            deletedInBatch);
                }

                // Small pause between batches to reduce database load
                if (deletedInBatch == batchSize) {
                    try {
                        Thread.sleep(100); // 100ms pause
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.warn("Cleanup task was interrupted");
                        break;
                    }
                }
            } while (deletedInBatch == batchSize); // Continue while we're deleting full batches

            if (totalDeleted > 0) {
                LOG.infof(
                        "Email verification cleanup completed. Total deleted entries: %d",
                        totalDeleted);
            } else {
                LOG.debug("Email verification cleanup completed. No expired entries found");
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error during email verification cleanup: %s", e.getMessage());
        }
    }

    /**
     * Manual cleanup method that can be called programmatically. Useful for testing or manual
     * maintenance.
     *
     * @return number of deleted entries
     */
    public long performManualCleanup() {
        if (!cleanupEnabled) {
            LOG.warn("Manual cleanup requested but cleanup is disabled");
            return 0;
        }

        try {
            LOG.info("Starting manual email verification cleanup");
            long totalDeleted = emailVerificationRepository.deleteExpiredEntries();
            LOG.infof("Manual cleanup completed. Deleted %d expired entries", totalDeleted);
            return totalDeleted;
        } catch (Exception e) {
            LOG.errorf(e, "Error during manual email verification cleanup: %s", e.getMessage());
            throw e;
        }
    }
}
