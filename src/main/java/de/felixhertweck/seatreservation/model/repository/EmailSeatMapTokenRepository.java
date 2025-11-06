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
import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.jboss.logging.Logger;

/**
 * Repository for managing EmailSeatMapToken entities. Provides methods to find, create, and delete
 * tokens used for accessing seat maps in email notifications.
 */
@ApplicationScoped
public class EmailSeatMapTokenRepository implements PanacheRepository<EmailSeatMapToken> {

    private static final Logger LOG = Logger.getLogger(EmailSeatMapTokenRepository.class);

    /**
     * Finds a token by its token string (regardless of expiration status).
     *
     * @param token the token string to search for
     * @return Optional containing the token if found
     */
    public Optional<EmailSeatMapToken> findByToken(String token) {
        LOG.debugf("Finding EmailSeatMapToken by token: %s", token);
        Optional<EmailSeatMapToken> result = find("token", token).firstResultOptional();
        if (result.isPresent()) {
            LOG.debugf("EmailSeatMapToken found for token: %s", token);
        } else {
            LOG.debugf("No EmailSeatMapToken found for token: %s", token);
        }
        return result;
    }

    /**
     * Deletes all expired tokens.
     *
     * @return the number of tokens deleted
     */
    @Transactional
    public long deleteExpiredTokens() {
        LOG.debug("Deleting expired EmailSeatMapTokens");
        Instant now = Instant.now();
        long deletedCount = delete("expirationTime <= ?1", now);
        LOG.infof("Deleted %d expired EmailSeatMapTokens", deletedCount);
        return deletedCount;
    }

    /**
     * Deletes all tokens for a specific user.
     *
     * @param user the user whose tokens to delete
     * @return the number of tokens deleted
     */
    @Transactional
    public long deleteByUser(User user) {
        LOG.debugf("Deleting EmailSeatMapTokens for user ID: %d", user.id);
        long deletedCount = delete("user", user);
        LOG.infof("Deleted %d EmailSeatMapTokens for user ID: %d", deletedCount, user.id);
        return deletedCount;
    }

    /**
     * Finds all expired tokens.
     *
     * @return a list of all expired tokens
     */
    public List<EmailSeatMapToken> findExpiredTokens() {
        Instant now = Instant.now();
        return list("expirationTime <= ?1", now);
    }
}
