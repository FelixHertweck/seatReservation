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

@ApplicationScoped
public class EmailVerificationRepository implements PanacheRepository<EmailVerification> {

    public Optional<EmailVerification> findByUser(
            de.felixhertweck.seatreservation.model.entity.User user) {
        return find("user", user).firstResultOptional();
    }

    /**
     * Finds expired email verification entries with a limit for batch processing.
     *
     * @param batchSize maximum number of entries to return
     * @return List of expired EmailVerification entities (limited by batchSize)
     */
    public List<EmailVerification> findExpiredEntries(int batchSize) {
        return find("expirationTime < ?1", LocalDateTime.now()).page(0, batchSize).list();
    }

    /**
     * Deletes all expired email verification entries.
     *
     * @return number of deleted entries
     */
    @Transactional
    public long deleteExpiredEntries() {
        return delete("expirationTime < ?1", LocalDateTime.now());
    }

    /**
     * Deletes expired email verification entries in batches.
     *
     * @param batchSize maximum number of entries to delete in one batch
     * @return number of deleted entries
     */
    @Transactional
    public long deleteExpiredEntriesInBatch(int batchSize) {
        List<EmailVerification> expiredEntries = findExpiredEntries(batchSize);
        if (expiredEntries.isEmpty()) {
            return 0;
        }

        List<Long> ids = expiredEntries.stream().map(e -> e.id).toList();
        return delete("id in ?1", ids);
    }
}
