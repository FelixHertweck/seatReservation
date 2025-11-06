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

import java.time.Instant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.LoginAttempt;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LoginAttemptRepository implements PanacheRepository<LoginAttempt> {

    private static final Logger LOG = Logger.getLogger(LoginAttemptRepository.class);

    /**
     * Counts failed login attempts for a username within a time window.
     *
     * @param username the username to check
     * @param since the start time of the window
     * @return the number of failed login attempts
     */
    public long countFailedAttempts(String username, Instant since) {
        LOG.debugf("Counting failed login attempts for username: %s since: %s", username, since);
        return count("username = ?1 and successful = false and attemptTime >= ?2", username, since);
    }

    /**
     * Deletes old login attempt records before a certain time.
     *
     * @param before the time before which to delete records
     * @return the number of deleted records
     */
    @Transactional
    public long deleteOldAttempts(Instant before) {
        LOG.debugf("Deleting login attempts before: %s", before);
        return delete("attemptTime < ?1", before);
    }

    /**
     * Records a login attempt.
     *
     * @param username the username of the login attempt
     * @param successful whether the login was successful
     */
    @Transactional
    public void recordAttempt(String username, boolean successful) {
        LOG.debugf(
                "Recording login attempt for username: %s, successful: %s", username, successful);
        LoginAttempt attempt = new LoginAttempt(username, Instant.now(), successful);
        persist(attempt);
    }

    /**
     * Gets the timestamp of the oldest failed login attempt within a time window.
     *
     * @param username the username to check
     * @param since the start time of the window
     * @return the timestamp of the oldest failed attempt, or null if none found
     */
    public Instant getOldestFailedAttemptTime(String username, Instant since) {
        LOG.debugf(
                "Getting oldest failed login attempt time for username: %s since: %s",
                username, since);
        return find(
                        "username = ?1 and successful = false and attemptTime >= ?2 order by"
                                + " attemptTime asc",
                        username,
                        since)
                .firstResultOptional()
                .map(LoginAttempt::getAttemptTime)
                .orElse(null);
    }
}
