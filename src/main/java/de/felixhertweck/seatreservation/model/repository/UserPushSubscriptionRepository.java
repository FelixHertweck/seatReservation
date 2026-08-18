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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.UserPushSubscription;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserPushSubscriptionRepository
        implements PanacheRepositoryBase<UserPushSubscription, UUID> {

    private static final Logger LOG = Logger.getLogger(UserPushSubscriptionRepository.class);

    /** Finds all active push subscriptions for a user. */
    public List<UserPushSubscription> findByUser(User user) {
        return find("user", user).list();
    }

    /** Finds a subscription by unique endpoint. */
    public Optional<UserPushSubscription> findByEndpoint(String endpoint) {
        return find("endpoint", endpoint).firstResultOptional();
    }

    /** Deletes a push subscription by endpoint and user. */
    @Transactional
    public boolean deleteByEndpointAndUser(String endpoint, User user) {
        LOG.debugf("Deleting push subscription for user ID: %s", (Object) user.id);
        return delete("endpoint = ?1 and user = ?2", endpoint, user) > 0;
    }
}
