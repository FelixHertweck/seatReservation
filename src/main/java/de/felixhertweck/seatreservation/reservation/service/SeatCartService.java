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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.reservation.dto.SeatCartEntryDTO;
import de.felixhertweck.seatreservation.reservation.exception.SeatAlreadyReservedException;
import de.felixhertweck.seatreservation.reservation.exception.SeatBlockedException;
import de.felixhertweck.seatreservation.reservation.exception.SeatCartAccessNotGrantedException;
import de.felixhertweck.seatreservation.reservation.exception.SeatPendingException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Redis-backed cache for a seat's cart/availability state, and for whether a user is currently
 * allowed to touch a given event's cart at all. Postgres remains the source of truth for
 * reservations; this cache is allowed to be wrong in rare edge cases (see class-level notes on each
 * method) - the frontend re-fetches {@code GET /api/user/events} and retries on conflict.
 *
 * <p>Each seat has a single string key {@code seatcart:<eventId>:<seatId>} whose value is one of:
 *
 * <ul>
 *   <li>{@code "RESERVED"} / {@code "BLOCKED"} - mirrors a persisted {@link Reservation#status},
 *       set by the write paths that persist that status (see {@link #markSeatsReserved}, {@link
 *       #markSeatsBlocked}), cleared by {@link #freeSeats} on cancellation/unblock.
 *   <li>{@code <userId>} - a temporary cart hold with a TTL, as before.
 *   <li>absent - the seat is free.
 * </ul>
 *
 * A per-event Redis set ({@code seatcart:idx:<eventId>}) indexes which seat IDs currently have a
 * temporary hold (never RESERVED/BLOCKED seats), so {@link #findPendingSeatIds} doesn't require a
 * keyspace-wide scan.
 *
 * <p>A separate per-(event,user) key ({@code seatcart:access:<eventId>:<userId>}) records whether
 * the user is currently allowed to write to this event's cart at all - minted by {@link
 * #grantAccess} when the user fetches their event list (where the allowance check already runs for
 * other reasons) and refreshed on every successful cart write, so it never expires out from under
 * an actively-selecting user.
 */
@ApplicationScoped
public class SeatCartService {

    private static final Logger LOG = Logger.getLogger(SeatCartService.class);
    private static final String KEY_PREFIX = "seatcart:";
    private static final String ACCESS_CACHE_PREFIX = "seatcart:access:";
    private static final String INDEX_KEY_PREFIX = "seatcart:idx:";
    private static final String SEEDED_KEY_PREFIX = "seatcart:seeded:";
    private static final String SEEDING_LOCK_KEY_PREFIX = "seatcart:seeding:";

    private static final String RESERVED_SENTINEL = "RESERVED";
    private static final String BLOCKED_SENTINEL = "BLOCKED";

    /** Buffer added on top of {@code ttlSeconds} for the access-grant's sliding TTL window. */
    private static final long ACCESS_GRANT_TTL_BUFFER_SECONDS = 30;

    /** Buffer added on top of an event's remaining lifetime for RESERVED/BLOCKED marker TTLs. */
    private static final Duration MARKER_TTL_BUFFER = Duration.ofDays(1);

    private static final Duration MIN_MARKER_TTL = Duration.ofSeconds(60);
    private static final Duration SEEDING_LOCK_TTL = Duration.ofSeconds(10);

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;

    @ConfigProperty(name = "seatcart.ttl-seconds")
    long ttlSeconds;

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final SetCommands<String, String> setCommands;

    @Inject
    public SeatCartService(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key(String.class);
        this.setCommands = redisDataSource.set(String.class);
    }

    /**
     * Attempts to hold the given seat in the current user's cart. Entirely Redis-based in the
     * common case - no Postgres read - except for a one-off per-event seeding read the very first
     * time any seat in that event is touched (see {@link #ensureEventSeeded}), and the rare case
     * where a concurrent seeding run is already in flight (falls back to a single direct DB check
     * for just this seat, see {@link #assertSeatNotPersistedAsUnavailable}).
     *
     * @throws SeatCartAccessNotGrantedException if the user has no (or an expired) access grant for
     *     this event - the caller must refresh {@code GET /api/user/events} and retry
     * @throws SeatAlreadyReservedException if the seat is already persisted as reserved
     * @throws SeatBlockedException if the seat is persisted as blocked
     * @throws SeatPendingException if the seat is currently held by a different user's cart
     */
    public SeatCartEntryDTO addSeatToCart(UUID eventId, UUID seatId, UUID userId) {
        assertAccessGranted(eventId, userId);

        if (!ensureEventSeeded(eventId)) {
            // A concurrent request is seeding this event right now; answer this one request
            // directly from Postgres rather than waiting on the seeding run to finish.
            assertSeatNotPersistedAsUnavailable(eventId, seatId);
        }

        String key = key(eventId, seatId);
        String userIdStr = userId.toString();
        String previousValue =
                valueCommands.setGet(
                        key, userIdStr, new SetArgs().nx().ex(Duration.ofSeconds(ttlSeconds)));

        if (RESERVED_SENTINEL.equals(previousValue)) {
            throw new SeatAlreadyReservedException("Seat is already reserved");
        }
        if (BLOCKED_SENTINEL.equals(previousValue)) {
            throw new SeatBlockedException("Seat is blocked");
        }
        if (previousValue != null && !previousValue.equals(userIdStr)) {
            LOG.warnf(
                    "Seat %s for event %s is held by another user's cart; rejecting hold for user"
                            + " %s.",
                    seatId, eventId, userId);
            throw new SeatPendingException("Seat is currently selected by another user");
        }

        if (previousValue != null) {
            // Already held by this user: NX above was a no-op, refresh the TTL explicitly.
            valueCommands.set(key, userIdStr, new SetArgs().ex(Duration.ofSeconds(ttlSeconds)));
        }

        // Track this seat in the event's index so findPendingSeatIds can look it up without
        // scanning the whole keyspace. Idempotent (SADD), so safe on both new holds and refreshes.
        setCommands.sadd(indexKey(eventId), seatId.toString());

        // Sliding window: as long as the user keeps interacting with this event's cart, their
        // access grant keeps getting pushed out instead of expiring mid-session.
        keyCommands.expire(
                accessCacheKey(eventId, userId),
                Duration.ofSeconds(ttlSeconds + ACCESS_GRANT_TTL_BUFFER_SECONDS));

        return new SeatCartEntryDTO(seatId, Instant.now().plusSeconds(ttlSeconds));
    }

    /** Releases the seat from the cart, but only if it is currently held by {@code userId}. */
    public void removeSeatFromCart(UUID eventId, UUID seatId, UUID userId) {
        String key = key(eventId, seatId);
        String owner = valueCommands.get(key);
        if (owner != null && owner.equals(userId.toString())) {
            keyCommands.del(key);
            setCommands.srem(indexKey(eventId), seatId.toString());
        }
    }

    /**
     * Marks the given seats as RESERVED in Redis (rather than clearing their key), so a subsequent
     * {@link #addSeatToCart} for the same seat rejects with {@link SeatAlreadyReservedException}
     * purely from Redis, without a Postgres read. Called after a reservation is successfully
     * persisted for these seats.
     *
     * @param eventEndTime used to derive the marker's TTL (see {@link #markerTtl})
     */
    public void markSeatsReserved(UUID eventId, Collection<UUID> seatIds, Instant eventEndTime) {
        setSentinelForSeats(eventId, seatIds, RESERVED_SENTINEL, eventEndTime);
    }

    /**
     * Marks the given seats as BLOCKED in Redis. Called after a manager persists a block for these
     * seats.
     *
     * @param eventEndTime used to derive the marker's TTL (see {@link #markerTtl})
     */
    public void markSeatsBlocked(UUID eventId, Collection<UUID> seatIds, Instant eventEndTime) {
        setSentinelForSeats(eventId, seatIds, BLOCKED_SENTINEL, eventEndTime);
    }

    private void setSentinelForSeats(
            UUID eventId, Collection<UUID> seatIds, String sentinel, Instant eventEndTime) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }
        SetArgs args = new SetArgs().ex(markerTtl(eventEndTime));
        for (UUID seatId : seatIds) {
            valueCommands.set(key(eventId, seatId), sentinel, args);
        }
        setCommands.srem(
                indexKey(eventId), seatIds.stream().map(UUID::toString).toArray(String[]::new));
    }

    /**
     * Frees the given seats entirely (deletes their Redis key), regardless of whether they were
     * previously a cart hold, RESERVED, or BLOCKED. Called after a reservation is cancelled or a
     * block is lifted, so the seat becomes selectable again.
     */
    public void freeSeats(UUID eventId, Collection<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }
        String[] seatIdStrs = seatIds.stream().map(UUID::toString).toArray(String[]::new);
        String[] keys = seatIds.stream().map(seatId -> key(eventId, seatId)).toArray(String[]::new);
        keyCommands.del(keys);
        setCommands.srem(indexKey(eventId), seatIdStrs);
    }

    /** Returns whether the given seat is currently held by a user other than {@code userId}. */
    public boolean isHeldByAnotherUser(UUID eventId, UUID seatId, UUID userId) {
        String owner = valueCommands.get(key(eventId, seatId));
        return owner != null && !owner.equals(userId.toString());
    }

    /**
     * Finds every seat ID in this event that currently has an active cart hold, regardless of
     * owner. Used to surface a transient {@link ReservationStatus#PENDING} status to other users.
     *
     * <p>Backed by the per-event index set rather than a keyspace-wide {@code KEYS} scan (which
     * would block Redis for O(total keys) on every call to this method - and this is called on
     * every {@code GET /api/user/events}). The index can contain stale entries for holds that
     * already expired via TTL (Redis has no per-set-member expiry), so each candidate is verified
     * with a single {@code MGET} and stale entries are opportunistically pruned from the index.
     * RESERVED/BLOCKED sentinels are never added to this index (see {@link #setSentinelForSeats}),
     * so they never show up here.
     */
    public Set<UUID> findPendingSeatIds(UUID eventId) {
        Set<String> candidateSeatIdStrs = setCommands.smembers(indexKey(eventId));
        if (candidateSeatIdStrs.isEmpty()) {
            return Set.of();
        }

        Map<String, String> holdKeyToSeatIdStr =
                candidateSeatIdStrs.stream()
                        .collect(
                                Collectors.toMap(
                                        seatIdStr -> key(eventId, UUID.fromString(seatIdStr)),
                                        seatIdStr -> seatIdStr));
        Map<String, String> stillHeld =
                valueCommands.mget(holdKeyToSeatIdStr.keySet().toArray(new String[0]));

        Set<UUID> pendingSeatIds = new HashSet<>();
        List<String> expiredSeatIdStrs = new ArrayList<>();
        holdKeyToSeatIdStr.forEach(
                (holdKey, seatIdStr) -> {
                    if (stillHeld.containsKey(holdKey)) {
                        pendingSeatIds.add(UUID.fromString(seatIdStr));
                    } else {
                        expiredSeatIdStrs.add(seatIdStr);
                    }
                });

        if (!expiredSeatIdStrs.isEmpty()) {
            setCommands.srem(indexKey(eventId), expiredSeatIdStrs.toArray(new String[0]));
        }

        return pendingSeatIds;
    }

    /**
     * Grants {@code userId} access to {@code eventId}'s cart for a sliding TTL window. Called by
     * {@link EventService} while it already loads the user's event allowances for {@code GET
     * /api/user/events} - minting the grant here means {@link #addSeatToCart} never needs its own
     * Postgres allowance check.
     */
    public void grantAccess(UUID eventId, UUID userId) {
        valueCommands.set(
                accessCacheKey(eventId, userId),
                "1",
                new SetArgs().ex(Duration.ofSeconds(ttlSeconds + ACCESS_GRANT_TTL_BUFFER_SECONDS)));
    }

    /**
     * Verifies {@code userId} currently has a Redis access grant for {@code eventId}. Purely
     * Redis-based, no Postgres fallback: an expired or missing grant means the caller's event list
     * is stale (or they never fetched it) and must be refreshed via {@code GET /api/user/events}
     * (which re-mints the grant via {@link #grantAccess}) before retrying.
     */
    private void assertAccessGranted(UUID eventId, UUID userId) {
        if (valueCommands.get(accessCacheKey(eventId, userId)) == null) {
            LOG.warnf(
                    "user ID: %s has no seat cart access grant for event ID: %s.", userId, eventId);
            throw new SeatCartAccessNotGrantedException(
                    "Seat cart access has expired or was never granted; refresh the event list and"
                            + " try again");
        }
    }

    /**
     * Ensures this event's RESERVED/BLOCKED seats have been mirrored from Postgres into Redis at
     * least once, so {@link #addSeatToCart} can rely purely on Redis afterwards.
     *
     * @return {@code true} if the event is seeded (either already, or just now by this call);
     *     {@code false} if another request is seeding it concurrently, in which case the caller
     *     must fall back to a direct Postgres check for this one request instead of waiting.
     */
    private boolean ensureEventSeeded(UUID eventId) {
        if (valueCommands.get(seededKey(eventId)) != null) {
            return true;
        }

        if (!valueCommands.setnx(seedingLockKey(eventId), "1")) {
            return false;
        }
        keyCommands.expire(seedingLockKey(eventId), SEEDING_LOCK_TTL);
        try {
            seedEventFromDatabase(eventId);
        } finally {
            keyCommands.del(seedingLockKey(eventId));
        }
        return true;
    }

    private void seedEventFromDatabase(UUID eventId) {
        Instant eventEndTime =
                eventRepository.findByIdOptional(eventId).map(Event::getEndTime).orElse(null);
        Duration ttl = eventEndTime != null ? markerTtl(eventEndTime) : MIN_MARKER_TTL;
        SetArgs args = new SetArgs().ex(ttl);

        for (Reservation reservation : reservationRepository.findByEventId(eventId)) {
            String sentinel =
                    reservation.getStatus() == ReservationStatus.BLOCKED
                            ? BLOCKED_SENTINEL
                            : RESERVED_SENTINEL;
            valueCommands.set(key(eventId, reservation.getSeat().id), sentinel, args);
        }
        valueCommands.set(seededKey(eventId), "1", args);
    }

    /** RESERVED/BLOCKED marker TTL: the event's remaining lifetime plus a buffer, floored. */
    private static Duration markerTtl(Instant eventEndTime) {
        Duration ttl = Duration.between(Instant.now(), eventEndTime).plus(MARKER_TTL_BUFFER);
        return ttl.compareTo(MIN_MARKER_TTL) > 0 ? ttl : MIN_MARKER_TTL;
    }

    /**
     * Rare-path fallback used only when {@link #ensureEventSeeded} couldn't run because a
     * concurrent seeding request was already in flight for this event.
     */
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

    private static String indexKey(UUID eventId) {
        return INDEX_KEY_PREFIX + eventId;
    }

    private static String accessCacheKey(UUID eventId, UUID userId) {
        return ACCESS_CACHE_PREFIX + eventId + ":" + userId;
    }

    private static String seededKey(UUID eventId) {
        return SEEDED_KEY_PREFIX + eventId;
    }

    private static String seedingLockKey(UUID eventId) {
        return SEEDING_LOCK_KEY_PREFIX + eventId;
    }
}
