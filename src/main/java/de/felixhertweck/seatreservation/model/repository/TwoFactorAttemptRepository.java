/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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

import java.time.Instant;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.TwoFactorAttempt;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TwoFactorAttemptRepository implements PanacheRepositoryBase<TwoFactorAttempt, UUID> {

    private static final Logger LOG = Logger.getLogger(TwoFactorAttemptRepository.class);

    /**
     * Counts failed 2FA verification attempts for a user within a time window.
     *
     * @param user the user to check
     * @param since the start time of the window
     * @return the number of failed attempts
     */
    public long countFailedAttempts(User user, Instant since) {
        LOG.debugf("Counting failed 2FA attempts since: %s", since);
        return count("user = ?1 and successful = false and attemptTime >= ?2", user, since);
    }

    /**
     * Gets the timestamp of the oldest failed 2FA attempt within a time window.
     *
     * @param user the user to check
     * @param since the start time of the window
     * @return the timestamp of the oldest failed attempt, or null if none found
     */
    public Instant getOldestFailedAttemptTime(User user, Instant since) {
        LOG.debugf("Getting oldest failed 2FA attempt time since: %s", since);
        return find(
                        "user = ?1 and successful = false and attemptTime >= ?2 order by"
                                + " attemptTime asc",
                        user,
                        since)
                .firstResultOptional()
                .map(TwoFactorAttempt::getAttemptTime)
                .orElse(null);
    }

    /**
     * Records a 2FA verification attempt.
     *
     * @param user the user the attempt was made for
     * @param successful whether the attempt succeeded
     */
    @Transactional
    public void recordAttempt(User user, boolean successful) {
        LOG.debugf(
                "Recording 2FA attempt for user ID: %s, successful: %s",
                (Object) user.id, successful);
        persist(new TwoFactorAttempt(user, Instant.now(), successful));
    }

    /**
     * Deletes old 2FA attempt records before a certain time.
     *
     * @param before the time before which to delete records
     * @return the number of deleted records
     */
    @Transactional
    public long deleteOldAttempts(Instant before) {
        LOG.debugf("Deleting 2FA attempts before: %s", before);
        return delete("attemptTime < ?1", before);
    }
}
