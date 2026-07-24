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
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.reservation.dto.SeatCartEntryDTO;
import de.felixhertweck.seatreservation.reservation.exception.SeatAlreadyReservedException;
import de.felixhertweck.seatreservation.reservation.exception.SeatBlockedException;
import de.felixhertweck.seatreservation.reservation.exception.SeatPendingException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Temporary, Redis-backed soft-lock on seats a user has selected but not yet reserved. Each held
 * seat is a single string key {@code seatcart:<eventId>:<seatId>} -> holding user's ID, with a
 * fixed TTL, so an unfinished selection releases itself automatically.
 */
@ApplicationScoped
public class SeatCartService {

    private static final Logger LOG = Logger.getLogger(SeatCartService.class);
    private static final String KEY_PREFIX = "seatcart:";

    @Inject ReservationRepository reservationRepository;

    @ConfigProperty(name = "seatcart.ttl-seconds")
    long ttlSeconds;

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;

    @Inject
    public SeatCartService(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key(String.class);
    }

    /**
     * Attempts to hold the given seat in the current user's cart.
     *
     * @throws SeatAlreadyReservedException if the seat is already persisted as reserved
     * @throws SeatBlockedException if the seat is persisted as blocked
     * @throws SeatPendingException if the seat is currently held by a different user's cart
     */
    public SeatCartEntryDTO addSeatToCart(UUID eventId, UUID seatId, UUID userId) {
        assertSeatNotPersistedAsUnavailable(eventId, seatId);

        String key = key(eventId, seatId);
        String userIdStr = userId.toString();
        String previousOwner =
                valueCommands.setGet(
                        key, userIdStr, new SetArgs().nx().ex(Duration.ofSeconds(ttlSeconds)));

        if (previousOwner != null && !previousOwner.equals(userIdStr)) {
            LOG.warnf(
                    "Seat %s for event %s is held by another user's cart; rejecting hold for user"
                            + " %s.",
                    seatId, eventId, userId);
            throw new SeatPendingException("Seat is currently selected by another user");
        }

        if (previousOwner != null) {
            // Already held by this user: NX above was a no-op, refresh the TTL explicitly.
            valueCommands.set(key, userIdStr, new SetArgs().ex(Duration.ofSeconds(ttlSeconds)));
        }

        return new SeatCartEntryDTO(seatId, Instant.now().plusSeconds(ttlSeconds));
    }

    /** Releases the seat from the cart, but only if it is currently held by {@code userId}. */
    public void removeSeatFromCart(UUID eventId, UUID seatId, UUID userId) {
        String key = key(eventId, seatId);
        String owner = valueCommands.get(key);
        if (owner != null && owner.equals(userId.toString())) {
            keyCommands.del(key);
        }
    }

    /**
     * Releases the cart entries for the given seats, regardless of owner. Called after a
     * reservation is successfully persisted for these seats.
     */
    public void releaseSeats(UUID eventId, Collection<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }
        String[] keys = seatIds.stream().map(seatId -> key(eventId, seatId)).toArray(String[]::new);
        keyCommands.del(keys);
    }

    /** Returns whether the given seat is currently held by a user other than {@code userId}. */
    public boolean isHeldByAnotherUser(UUID eventId, UUID seatId, UUID userId) {
        String owner = valueCommands.get(key(eventId, seatId));
        return owner != null && !owner.equals(userId.toString());
    }

    /**
     * Finds every seat ID in this event that currently has an active cart hold, regardless of
     * owner. Used to surface a transient {@link ReservationStatus#PENDING} status to other users.
     */
    public Set<UUID> findPendingSeatIds(UUID eventId) {
        List<String> keys = keyCommands.keys(KEY_PREFIX + eventId + ":*");
        return keys.stream()
                .map(k -> UUID.fromString(k.substring(k.lastIndexOf(':') + 1)))
                .collect(Collectors.toSet());
    }

    private void assertSeatNotPersistedAsUnavailable(UUID eventId, UUID seatId) {
        List<Reservation> existing =
                reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId));
        for (Reservation reservation : existing) {
            if (reservation.getStatus() == ReservationStatus.RESERVED) {
                throw new SeatAlreadyReservedException("Seat is already reserved");
            } else if (reservation.getStatus() == ReservationStatus.BLOCKED) {
                throw new SeatBlockedException("Seat is blocked");
            }
        }
    }

    private static String key(UUID eventId, UUID seatId) {
        return KEY_PREFIX + eventId + ":" + seatId;
    }
}
