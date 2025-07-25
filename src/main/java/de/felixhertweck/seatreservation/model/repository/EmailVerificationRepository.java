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
package de.felixhertweck.seatreservation.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailVerificationRepository implements PanacheRepository<EmailVerification> {

    private static final Logger LOG = Logger.getLogger(EmailVerificationRepository.class);

    public Optional<EmailVerification> findByUser(
            de.felixhertweck.seatreservation.model.entity.User user) {
        LOG.debugf("Finding EmailVerification by user ID: %d", user.id);
        Optional<EmailVerification> result = find("user", user).firstResultOptional();
        if (result.isPresent()) {
            LOG.debugf("EmailVerification found for user ID: %d", user.id);
        } else {
            LOG.debugf("No EmailVerification found for user ID: %d", user.id);
        }
        return result;
    }

    /**
     * Finds expired email verification entries with a limit for batch processing.
     *
     * @param batchSize maximum number of entries to return
     * @return List of expired EmailVerification entities (limited by batchSize)
     */
    public List<EmailVerification> findExpiredEntries(int batchSize) {
        LOG.debugf("Finding expired EmailVerification entries with batch size: %d", batchSize);
        List<EmailVerification> entries =
                find("expirationTime < ?1", LocalDateTime.now()).page(0, batchSize).list();
        LOG.debugf("Found %d expired EmailVerification entries.", entries.size());
        return entries;
    }

    /**
     * Deletes all expired email verification entries.
     *
     * @return number of deleted entries
     */
    @Transactional
    public long deleteExpiredEntries() {
        LOG.infof("Deleting all expired EmailVerification entries.");
        long deletedCount = delete("expirationTime < ?1", LocalDateTime.now());
        LOG.infof("Deleted %d expired EmailVerification entries.", deletedCount);
        return deletedCount;
    }

    /**
     * Deletes expired email verification entries in batches.
     *
     * @param batchSize maximum number of entries to delete in one batch
     * @return number of deleted entries
     */
    @Transactional
    public long deleteExpiredEntriesInBatch(int batchSize) {
        LOG.debugf("Deleting expired EmailVerification entries in batch with size: %d", batchSize);
        List<EmailVerification> expiredEntries = findExpiredEntries(batchSize);
        if (expiredEntries.isEmpty()) {
            LOG.debug("No expired EmailVerification entries found in batch.");
            return 0;
        }

        List<Long> ids = expiredEntries.stream().map(e -> e.id).toList();
        long deletedCount = delete("id in ?1", ids);
        LOG.debugf("Deleted %d expired EmailVerification entries in current batch.", deletedCount);
        return deletedCount;
    }
}
