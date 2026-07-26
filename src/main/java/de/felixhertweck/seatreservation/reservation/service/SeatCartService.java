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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.reservation.dto.SeatCartEntryDTO;
import de.felixhertweck.seatreservation.reservation.exception.NoSeatsAvailableException;
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
 * Temporary, Redis-backed soft-lock on seats a user has selected but not yet reserved, plus whether
 * a user is currently allowed to touch a given event's cart at all. Postgres remains the source of
 * truth for reservations; this cache is allowed to be wrong in rare edge cases (see class-level
 * notes on each method) - the frontend re-fetches {@code GET /api/user/events} and retries on
 * conflict.
 *
 * <p>Each held seat is a single string key {@code seatcart:<eventId>:<seatId>} -> holding user's
 * ID, with a fixed TTL, so an unfinished selection releases itself automatically.
 *
 * <p>A per-event Redis set ({@code seatcart:idx:<eventId>}) indexes every currently held seat ID so
 * {@link #findPendingSeatIds} doesn't require a keyspace-wide scan. A second, per-(event,user) set
 * ({@code seatcart:useridx:<eventId>:<userId>}) indexes just that user's holds, so the per-event
 * seat quota (see {@link #assertQuotaNotExceeded}) can be checked without scanning every held seat
 * in the event.
 *
 * <p>A separate per-(event,user) key ({@code seatcart:access:<eventId>:<userId>}), whose value is
 * the user's allowed seat count, records whether the user is currently allowed to write to this
 * event's cart at all - minted by {@link #grantAccess} when the user fetches their event list
 * (where the allowance check already runs for other reasons) and refreshed on every successful cart
 * write, so it never expires out from under an actively-selecting user. If it has expired, {@link
 * #assertAccessGranted} self-heals with a single direct Postgres check rather than forcing the
 * frontend into a full resync. The grant is also actively invalidated the moment the underlying
 * {@code EventUserAllowance} changes - see {@link SeatCartAccessGrantStore} and {@link
 * de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository}.
 */
@ApplicationScoped
public class SeatCartService {

    private static final Logger LOG = Logger.getLogger(SeatCartService.class);
    private static final String KEY_PREFIX = "seatcart:";
    private static final String INDEX_KEY_PREFIX = "seatcart:idx:";
    private static final String USER_INDEX_KEY_PREFIX = "seatcart:useridx:";

    @Inject ReservationRepository reservationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject SeatCartAccessGrantStore accessGrantStore;

    @ConfigProperty(name = "seatcart.ttl-seconds")
    long ttlSeconds;

    /** Buffer added on top of {@code ttlSeconds} for the access-grant's sliding TTL window. */
    @ConfigProperty(name = "seatcart.access-grant-ttl-buffer-seconds")
    long accessGrantTtlBufferSeconds;

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
     * Attempts to hold the given seat in the current user's cart.
     *
     * @throws SeatCartAccessNotGrantedException if the user has no allowance for this event at all
     * @throws SeatAlreadyReservedException if the seat is already persisted as reserved
     * @throws SeatBlockedException if the seat is persisted as blocked
     * @throws SeatPendingException if the seat is currently held by a different user's cart
     * @throws NoSeatsAvailableException if the user has already reached their allowed seat count
     *     for this event with other active cart holds
     */
    public SeatCartEntryDTO addSeatToCart(UUID eventId, UUID seatId, UUID userId) {
        int allowedCount = assertAccessGranted(eventId, userId);
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

        if (previousOwner == null) {
            // Brand-new hold (not a refresh of one already owned by this user): only this case can
            // push the user over their per-event quota, so only check and roll back here.
            //
            // Known race, accepted as-is: this check isn't atomic with the SET NX above, so two
            // concurrent calls for different seats can both pass it. Not a booking-integrity risk
            // since createReservationForUser re-enforces the real allowance from Postgres.
            setCommands.sadd(userIndexKey(eventId, userId), seatId.toString());
            if (countHeldSeats(eventId, userId) > allowedCount) {
                keyCommands.del(key);
                setCommands.srem(userIndexKey(eventId, userId), seatId.toString());
                LOG.warnf(
                        "user ID: %s reached their seat cart quota (%d) for event ID: %s.",
                        userId, allowedCount, eventId);
                throw new NoSeatsAvailableException(
                        "You have reached your reservation limit for this event");
            }
        } else {
            // Already held by this user: NX above was a no-op, refresh the TTL explicitly.
            valueCommands.set(key, userIdStr, new SetArgs().ex(Duration.ofSeconds(ttlSeconds)));
        }

        // Track this seat in the event's index so findPendingSeatIds can look it up without
        // scanning the whole keyspace. Idempotent (SADD), so safe on both new holds and refreshes.
        setCommands.sadd(indexKey(eventId), seatId.toString());

        // Sliding window: as long as the user keeps interacting with this event's cart, their
        // access grant keeps getting pushed out instead of expiring mid-session.
        accessGrantStore.refreshTtl(
                eventId, userId, Duration.ofSeconds(ttlSeconds + accessGrantTtlBufferSeconds));

        return new SeatCartEntryDTO(seatId, Instant.now().plusSeconds(ttlSeconds));
    }

    /** Releases the seat from the cart, but only if it is currently held by {@code userId}. */
    public void removeSeatFromCart(UUID eventId, UUID seatId, UUID userId) {
        String key = key(eventId, seatId);
        String owner = valueCommands.get(key);
        if (owner != null && owner.equals(userId.toString())) {
            keyCommands.del(key);
            setCommands.srem(indexKey(eventId), seatId.toString());
            setCommands.srem(userIndexKey(eventId, userId), seatId.toString());
        }
    }

    /**
     * Releases the cart entries for the given seats, regardless of owner. Called after a
     * reservation is successfully persisted for these seats, so the next {@link #addSeatToCart} for
     * the same seat falls through to the (now up to date) Postgres check instead of a stale cart
     * hold.
     */
    public void releaseSeats(UUID eventId, Collection<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }
        String[] keys = seatIds.stream().map(seatId -> key(eventId, seatId)).toArray(String[]::new);
        keyCommands.del(keys);
        String[] seatIdStrs = seatIds.stream().map(UUID::toString).toArray(String[]::new);
        setCommands.srem(indexKey(eventId), seatIdStrs);
    }

    /** Returns whether the given seat is currently held by a user other than {@code userId}. */
    public boolean isHeldByAnotherUser(UUID eventId, UUID seatId, UUID userId) {
        String owner = valueCommands.get(key(eventId, seatId));
        return owner != null && !owner.equals(userId.toString());
    }

    /**
     * Finds every seat ID in this event that currently has an active cart hold owned by someone
     * other than {@code requestingUserId}. Used to surface a transient {@link
     * ReservationStatus#PENDING} status to other users - a seat the requesting user holds
     * themselves is their own in-progress selection, not something blocking them.
     *
     * <p>Backed by the per-event index set rather than a keyspace-wide {@code KEYS} scan (which
     * would block Redis for O(total keys) on every call to this method - and this is called on
     * every {@code GET /api/user/events}). The index can contain stale entries for holds that
     * already expired via TTL (Redis has no per-set-member expiry); {@link #resolveLiveHolds}
     * verifies each candidate with a single {@code MGET} and opportunistically prunes those stale
     * entries from the index.
     */
    public Set<UUID> findPendingSeatIds(UUID eventId, UUID requestingUserId) {
        String requestingUserIdStr = requestingUserId.toString();
        Map<UUID, String> liveHolds = resolveLiveHolds(eventId, indexKey(eventId));

        Set<UUID> pendingSeatIds = new HashSet<>();
        liveHolds.forEach(
                (seatId, owner) -> {
                    if (!owner.equals(requestingUserIdStr)) {
                        pendingSeatIds.add(seatId);
                    }
                });
        return pendingSeatIds;
    }

    /**
     * Grants {@code userId} access to {@code eventId}'s cart for a sliding TTL window, remembering
     * their currently allowed seat count so {@link #addSeatToCart} can enforce the per-event quota
     * without a Postgres read. Called by {@link EventService} while it already loads the user's
     * event allowances for {@code GET /api/user/events} - minting the grant here means {@link
     * #addSeatToCart} never needs its own Postgres allowance check in the common case.
     */
    public void grantAccess(UUID eventId, UUID userId, int allowedCount) {
        accessGrantStore.set(
                eventId,
                userId,
                allowedCount,
                Duration.ofSeconds(ttlSeconds + accessGrantTtlBufferSeconds));
    }

    /**
     * Verifies {@code userId} currently has a Redis access grant for {@code eventId} and returns
     * the allowed seat count it carries. If the grant is missing or expired - most commonly because
     * its TTL simply ran out while the user kept looking at an already-loaded page, or because the
     * underlying {@code EventUserAllowance} changed and {@link
     * de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository} invalidated
     * it - falls back to a single direct Postgres allowance check and re-mints the grant, rather
     * than forcing the frontend into a full {@code GET /api/user/events} resync for what is usually
     * a harmless, recoverable gap.
     *
     * @throws SeatCartAccessNotGrantedException if the user genuinely has no allowance for this
     *     event in Postgres either
     */
    private int assertAccessGranted(UUID eventId, UUID userId) {
        Optional<Integer> grant = accessGrantStore.get(eventId, userId);
        if (grant.isPresent()) {
            return grant.get();
        }

        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByUserIdAndEventId(userId, eventId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "user ID: %s has no seat cart access grant or"
                                                    + " allowance for event ID: %s.",
                                            userId, eventId);
                                    return new SeatCartAccessNotGrantedException(
                                            "You are not allowed to select seats for this event");
                                });
        grantAccess(eventId, userId, allowance.getReservationsAllowedCount());
        return allowance.getReservationsAllowedCount();
    }

    /**
     * Counts how many seats {@code userId} currently holds in {@code eventId}'s cart. Backed by the
     * per-(event,user) index set, verified against live hold keys via {@link #resolveLiveHolds} -
     * an entry that isn't actually owned by {@code userId} (expired, or - defensively, though not
     * reachable via any current write path - held by someone else) is pruned from the index rather
     * than counted.
     */
    private int countHeldSeats(UUID eventId, UUID userId) {
        String userIdStr = userId.toString();
        Map<UUID, String> liveHolds = resolveLiveHolds(eventId, userIndexKey(eventId, userId));

        int heldCount = 0;
        List<String> staleSeatIdStrs = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : liveHolds.entrySet()) {
            if (userIdStr.equals(entry.getValue())) {
                heldCount++;
            } else {
                staleSeatIdStrs.add(entry.getKey().toString());
            }
        }

        if (!staleSeatIdStrs.isEmpty()) {
            setCommands.srem(userIndexKey(eventId, userId), staleSeatIdStrs.toArray(new String[0]));
        }

        return heldCount;
    }

    /**
     * Resolves the seat IDs tracked in {@code indexSetKey} against their live hold keys via a
     * single {@code MGET}, and opportunistically prunes stale entries - holds that already expired
     * via TTL but are still listed in the index, since Redis has no per-set-member expiry - from
     * that same index. Returns every seat ID that still has an active hold, mapped to its holding
     * user's ID.
     *
     * <p>Note: {@link ValueCommands#mget} always returns an entry for every requested key, using
     * {@code null} as the value for keys that don't exist in Redis (per its own javadoc) - so
     * checking {@code stillHeld.containsKey(holdKey)} would always be {@code true} regardless of
     * whether the hold actually expired. Expiry must be detected via {@code stillHeld.get(holdKey)
     * == null} instead.
     *
     * <p>Shared by {@link #findPendingSeatIds} (index = the per-event set) and {@link
     * #countHeldSeats} (index = the per-(event,user) set).
     */
    private Map<UUID, String> resolveLiveHolds(UUID eventId, String indexSetKey) {
        Set<String> candidateSeatIdStrs = setCommands.smembers(indexSetKey);
        if (candidateSeatIdStrs.isEmpty()) {
            return Map.of();
        }

        Map<String, String> holdKeyToSeatIdStr =
                candidateSeatIdStrs.stream()
                        .collect(
                                Collectors.toMap(
                                        seatIdStr -> key(eventId, UUID.fromString(seatIdStr)),
                                        seatIdStr -> seatIdStr));
        Map<String, String> stillHeld =
                valueCommands.mget(holdKeyToSeatIdStr.keySet().toArray(new String[0]));

        Map<UUID, String> liveHolds = new HashMap<>();
        List<String> expiredSeatIdStrs = new ArrayList<>();
        holdKeyToSeatIdStr.forEach(
                (holdKey, seatIdStr) -> {
                    String owner = stillHeld.get(holdKey);
                    if (owner == null) {
                        expiredSeatIdStrs.add(seatIdStr);
                    } else {
                        liveHolds.put(UUID.fromString(seatIdStr), owner);
                    }
                });

        if (!expiredSeatIdStrs.isEmpty()) {
            setCommands.srem(indexSetKey, expiredSeatIdStrs.toArray(new String[0]));
        }

        return liveHolds;
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

    private static String indexKey(UUID eventId) {
        return INDEX_KEY_PREFIX + eventId;
    }

    private static String userIndexKey(UUID eventId, UUID userId) {
        return USER_INDEX_KEY_PREFIX + eventId + ":" + userId;
    }
}
