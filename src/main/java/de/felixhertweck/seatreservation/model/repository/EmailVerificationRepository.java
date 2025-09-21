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

import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailVerificationRepository implements PanacheRepository<EmailVerification> {

    private static final Logger LOG = Logger.getLogger(EmailVerificationRepository.class);

    public Optional<EmailVerification> findByUser(User user) {
        LOG.debugf("Finding EmailVerification by user ID: %d", user.id);
        Optional<EmailVerification> result = find("user", user).firstResultOptional();
        if (result.isPresent()) {
            LOG.debugf("EmailVerification found for user ID: %d", user.id);
        } else {
            LOG.debugf("No EmailVerification found for user ID: %d", user.id);
        }
        return result;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteByUserId(Long userId) {
        LOG.debugf(
                "Attempting to delete EmailVerification for user ID: %d in new transaction.",
                userId);
        long deletedCount = delete("user.id", userId);
        LOG.infof("Deleted %d EmailVerification entries for user ID: %d.", deletedCount, userId);
    }

    /**
     * Finds an EmailVerification by user ID.
     *
     * @param userId the user ID to search for
     * @return Optional EmailVerification entity
     */
    public Optional<EmailVerification> findByUserIdOptional(Long userId) {
        LOG.debugf("Finding EmailVerification by user ID: %d", userId);
        Optional<EmailVerification> result = find("user.id", userId).firstResultOptional();
        if (result.isPresent()) {
            LOG.debugf("EmailVerification found for user ID: %d", userId);
        } else {
            LOG.debugf("No EmailVerification found for user ID: %d", userId);
        }
        return result;
    }

    /**
     * Finds an EmailVerification by verification token/code.
     *
     * @param token the verification token/code to search for
     * @return EmailVerification entity or null if not found
     */
    public EmailVerification findByToken(String token) {
        LOG.debugf("Finding EmailVerification by token: %s", token);
        EmailVerification result = find("token", token).firstResult();
        if (result != null) {
            LOG.debugf("EmailVerification found for token: %s", token);
        } else {
            LOG.debugf("No EmailVerification found for token: %s", token);
        }
        return result;
    }
}
