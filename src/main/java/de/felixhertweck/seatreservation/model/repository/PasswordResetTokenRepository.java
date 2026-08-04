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

import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PasswordResetTokenRepository
        implements PanacheRepositoryBase<PasswordResetToken, UUID> {

    private static final Logger LOG = Logger.getLogger(PasswordResetTokenRepository.class);

    /**
     * Finds a PasswordResetToken by its token value.
     *
     * @param token the token to search for
     * @return Optional PasswordResetToken entity
     */
    public Optional<PasswordResetToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    /**
     * Deletes any existing PasswordResetToken for the given user.
     *
     * @param userId the user ID to delete the token for
     */
    @Transactional
    public void deleteByUserId(UUID userId) {
        long deletedCount = delete("user.id", userId);
        LOG.infof("Deleted %d PasswordResetToken entries for user ID: %s.", deletedCount, userId);
    }
}
