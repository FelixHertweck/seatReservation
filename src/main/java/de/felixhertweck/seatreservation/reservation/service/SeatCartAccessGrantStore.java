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
package de.felixhertweck.seatreservation.reservation.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;

/**
 * Thin wrapper around the single Redis key that records a user's seat-cart access grant for an
 * event ({@code seatcart:access:<eventId>:<userId>} -> their allowed seat count). Holds no business
 * logic (quota checks, self-healing on expiry, ...) - that stays in {@link SeatCartService}.
 *
 * <p>Kept as its own bean (rather than folded into {@link SeatCartService}) so {@link
 * de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository} can invalidate a
 * stale grant on every write without depending on {@link SeatCartService} - which itself depends on
 * that repository for its self-heal read. Injecting {@link SeatCartService} into the repository
 * would create a dependency cycle; this store has no dependencies of its own, so it doesn't.
 */
@ApplicationScoped
public class SeatCartAccessGrantStore {

    private static final String ACCESS_CACHE_PREFIX = "seatcart:access:";

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;

    @Inject
    public SeatCartAccessGrantStore(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key(String.class);
    }

    /** Returns the cached allowed-seat-count grant for this user/event, if still valid. */
    public Optional<Integer> get(UUID eventId, UUID userId) {
        String grant = valueCommands.get(key(eventId, userId));
        return grant == null ? Optional.empty() : Optional.of(Integer.parseInt(grant));
    }

    /** Sets (or overwrites) the grant with a fresh TTL. */
    public void set(UUID eventId, UUID userId, int allowedCount, Duration ttl) {
        valueCommands.set(
                key(eventId, userId), String.valueOf(allowedCount), new SetArgs().ex(ttl));
    }

    /** Pushes the grant's TTL back out without changing its value. No-op if it doesn't exist. */
    public void refreshTtl(UUID eventId, UUID userId, Duration ttl) {
        keyCommands.expire(key(eventId, userId), ttl);
    }

    /** Deletes the grant so the next check falls through to a direct Postgres read. */
    public void invalidate(UUID eventId, UUID userId) {
        keyCommands.del(key(eventId, userId));
    }

    private static String key(UUID eventId, UUID userId) {
        return ACCESS_CACHE_PREFIX + eventId + ":" + userId;
    }
}
