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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeatCartServiceTest {

    private static final long TTL_SECONDS = 300;
    private static final long ACCESS_GRANT_TTL_BUFFER_SECONDS = 30;
    private static final long ACCESS_GRANT_TTL_SECONDS =
            TTL_SECONDS + ACCESS_GRANT_TTL_BUFFER_SECONDS;
    private static final int ALLOWED_COUNT = 2;

    private ReservationRepository reservationRepository;
    private EventUserAllowanceRepository eventUserAllowanceRepository;
    private SeatCartAccessGrantStore accessGrantStore;
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;
    private SetCommands<String, String> setCommands;
    private SeatCartService seatCartService;

    private final UUID eventId = id(1);
    private final UUID seatId = id(2);
    private final UUID userId = id(3);
    private final UUID otherUserId = id(4);

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        eventUserAllowanceRepository = mock(EventUserAllowanceRepository.class);
        accessGrantStore = mock(SeatCartAccessGrantStore.class);
        valueCommands = mock(ValueCommands.class);
        keyCommands = mock(KeyCommands.class);
        setCommands = mock(SetCommands.class);

        RedisDataSource redisDataSource = mock(RedisDataSource.class);
        when(redisDataSource.value(String.class)).thenReturn(valueCommands);
        when(redisDataSource.key(String.class)).thenReturn(keyCommands);
        when(redisDataSource.set(String.class)).thenReturn(setCommands);

        seatCartService = new SeatCartService(redisDataSource);
        seatCartService.reservationRepository = reservationRepository;
        seatCartService.eventUserAllowanceRepository = eventUserAllowanceRepository;
        seatCartService.accessGrantStore = accessGrantStore;
        seatCartService.ttlSeconds = TTL_SECONDS;
        seatCartService.accessGrantTtlBufferSeconds = ACCESS_GRANT_TTL_BUFFER_SECONDS;

        // Default: access already granted with an allowance of ALLOWED_COUNT, seat not persisted
        // as unavailable - most tests exercise only the Redis-only hot path plus this one DB read.
        when(accessGrantStore.get(eventId, userId)).thenReturn(Optional.of(ALLOWED_COUNT));
        when(reservationRepository.findByEventIdAndSeatIds(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    private String key() {
        return "seatcart:" + eventId + ":" + seatId;
    }

    private String key(UUID seat) {
        return "seatcart:" + eventId + ":" + seat;
    }

    private String indexKey() {
        return "seatcart:idx:" + eventId;
    }

    private String userIndexKey() {
        return "seatcart:useridx:" + eventId + ":" + userId;
    }

    private static Reservation reservationWith(UUID seatId, ReservationStatus status) {
        Reservation reservation = mock(Reservation.class);
        Seat seat = mock(Seat.class);
        seat.id = seatId;
        when(reservation.getSeat()).thenReturn(seat);
        when(reservation.getStatus()).thenReturn(status);
        return reservation;
    }

    @Test
    void addSeatToCart_Success_NewHold() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);
        when(setCommands.smembers(userIndexKey())).thenReturn(Set.of(seatId.toString()));
        when(valueCommands.mget(any(String[].class))).thenReturn(Map.of(key(), userId.toString()));

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        assertTrue(result.expiresAt().isAfter(Instant.now()));
        // No TTL-refresh call for the seat hold itself (new hold, NX already set it).
        verify(valueCommands, never()).set(eq(key()), anyString(), any(SetArgs.class));
        verify(setCommands, times(1)).sadd(userIndexKey(), seatId.toString());
        verify(setCommands, times(1)).sadd(indexKey(), seatId.toString());
        // Sliding window: access grant TTL pushed out again on every successful cart write.
        verify(accessGrantStore, times(1))
                .refreshTtl(eventId, userId, Duration.ofSeconds(ACCESS_GRANT_TTL_SECONDS));
    }

    @Test
    void addSeatToCart_AlreadyHeldBySameUser_RefreshesTtl() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(userId.toString());

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        verify(valueCommands, times(1)).set(eq(key()), eq(userId.toString()), any(SetArgs.class));
        // Refreshing an existing hold never needs the quota check.
        verify(setCommands, never()).sadd(userIndexKey(), seatId.toString());
    }

    @Test
    void addSeatToCart_HeldByAnotherUser_ThrowsSeatPendingException() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(otherUserId.toString());

        assertThrows(
                SeatPendingException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
    }

    @Test
    void addSeatToCart_SeatAlreadyReserved_ThrowsFromDbCheck() {
        Reservation reserved = reservationWith(seatId, ReservationStatus.RESERVED);
        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(List.of(reserved));

        assertThrows(
                SeatAlreadyReservedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(valueCommands, never()).setGet(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void addSeatToCart_SeatBlocked_ThrowsFromDbCheck() {
        Reservation blocked = reservationWith(seatId, ReservationStatus.BLOCKED);
        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(List.of(blocked));

        assertThrows(
                SeatBlockedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(valueCommands, never()).setGet(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void addSeatToCart_AccessNotGranted_NoAllowanceInDb_Throws() {
        when(accessGrantStore.get(eventId, userId)).thenReturn(Optional.empty());
        when(eventUserAllowanceRepository.findByUserIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                SeatCartAccessNotGrantedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(valueCommands, never()).setGet(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void addSeatToCart_AccessGrantExpired_SelfHealsFromDatabaseAndMintsNewGrant() {
        when(accessGrantStore.get(eventId, userId)).thenReturn(Optional.empty());
        EventUserAllowance allowance = mock(EventUserAllowance.class);
        when(allowance.getReservationsAllowedCount()).thenReturn(ALLOWED_COUNT);
        when(eventUserAllowanceRepository.findByUserIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(allowance));
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);
        when(setCommands.smembers(userIndexKey())).thenReturn(Set.of(seatId.toString()));
        when(valueCommands.mget(any(String[].class))).thenReturn(Map.of(key(), userId.toString()));

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        verify(accessGrantStore, times(1))
                .set(eventId, userId, ALLOWED_COUNT, Duration.ofSeconds(ACCESS_GRANT_TTL_SECONDS));
    }

    @Test
    void addSeatToCart_QuotaExceeded_RollsBackHoldAndThrows() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);
        UUID otherHeldSeat1 = id(7);
        UUID otherHeldSeat2 = id(8);
        // ALLOWED_COUNT is 2, but the user (including the seat just added) would hold 3.
        when(setCommands.smembers(userIndexKey()))
                .thenReturn(
                        Set.of(
                                seatId.toString(),
                                otherHeldSeat1.toString(),
                                otherHeldSeat2.toString()));
        when(valueCommands.mget(any(String[].class)))
                .thenReturn(
                        Map.of(
                                key(), userId.toString(),
                                key(otherHeldSeat1), userId.toString(),
                                key(otherHeldSeat2), userId.toString()));

        assertThrows(
                NoSeatsAvailableException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));

        verify(keyCommands, times(1)).del(key());
        verify(setCommands, times(1)).srem(userIndexKey(), seatId.toString());
        verify(setCommands, never()).sadd(indexKey(), seatId.toString());
    }

    @Test
    void addSeatToCart_AtQuotaLimit_Succeeds() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);
        UUID otherHeldSeat = id(7);
        when(setCommands.smembers(userIndexKey()))
                .thenReturn(Set.of(seatId.toString(), otherHeldSeat.toString()));
        when(valueCommands.mget(any(String[].class)))
                .thenReturn(
                        Map.of(
                                key(), userId.toString(),
                                key(otherHeldSeat), userId.toString()));

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        verify(keyCommands, never()).del(key());
    }

    @Test
    void removeSeatFromCart_OwnedByUser_DeletesKeyAndIndexes() {
        when(valueCommands.get(key())).thenReturn(userId.toString());

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, times(1)).del(key());
        verify(setCommands, times(1)).srem(indexKey(), seatId.toString());
        verify(setCommands, times(1)).srem(userIndexKey(), seatId.toString());
    }

    @Test
    void removeSeatFromCart_OwnedByAnotherUser_DoesNotDelete() {
        when(valueCommands.get(key())).thenReturn(otherUserId.toString());

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, never()).del(anyString());
        verify(setCommands, never()).srem(anyString(), anyString());
    }

    @Test
    void removeSeatFromCart_NotHeld_DoesNotDelete() {
        when(valueCommands.get(key())).thenReturn(null);

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, never()).del(anyString());
        verify(setCommands, never()).srem(anyString(), anyString());
    }

    @Test
    void releaseSeats_EmptyList_DoesNotCallRedis() {
        seatCartService.releaseSeats(eventId, Collections.emptyList());

        verify(keyCommands, never()).del(any(String[].class));
        verify(setCommands, never()).srem(anyString(), anyString());
    }

    @Test
    void releaseSeats_WithSeats_DeletesKeysAndPrunesIndex() {
        UUID seat2 = id(5);

        seatCartService.releaseSeats(eventId, List.of(seatId, seat2));

        verify(keyCommands, times(1)).del(eq(key()), eq(key(seat2)));
        verify(setCommands, times(1)).srem(indexKey(), seatId.toString(), seat2.toString());
    }

    @Test
    void isHeldByAnotherUser_HeldByOther_ReturnsTrue() {
        when(valueCommands.get(key())).thenReturn(otherUserId.toString());

        assertTrue(seatCartService.isHeldByAnotherUser(eventId, seatId, userId));
    }

    @Test
    void isHeldByAnotherUser_HeldBySameUser_ReturnsFalse() {
        when(valueCommands.get(key())).thenReturn(userId.toString());

        assertFalse(seatCartService.isHeldByAnotherUser(eventId, seatId, userId));
    }

    @Test
    void isHeldByAnotherUser_NotHeld_ReturnsFalse() {
        when(valueCommands.get(key())).thenReturn(null);

        assertFalse(seatCartService.isHeldByAnotherUser(eventId, seatId, userId));
    }

    @Test
    void findPendingSeatIds_AllStillHeld_ReturnsAllSeatIds() {
        UUID seat2 = id(6);
        when(setCommands.smembers(indexKey()))
                .thenReturn(Set.of(seatId.toString(), seat2.toString()));
        when(valueCommands.mget(any(String[].class)))
                .thenReturn(
                        Map.of(
                                "seatcart:" + eventId + ":" + seatId, userId.toString(),
                                "seatcart:" + eventId + ":" + seat2, otherUserId.toString()));

        Set<UUID> result = seatCartService.findPendingSeatIds(eventId);

        assertEquals(Set.of(seatId, seat2), result);
        verify(setCommands, never()).srem(anyString(), anyString());
    }

    @Test
    void findPendingSeatIds_PrunesExpiredIndexEntries() {
        UUID seat2 = id(6);
        when(setCommands.smembers(indexKey()))
                .thenReturn(Set.of(seatId.toString(), seat2.toString()));
        // Only seatId's hold key is still present - seat2's hold expired via TTL but the index
        // entry survived (Redis sets have no per-member TTL).
        when(valueCommands.mget(any(String[].class)))
                .thenReturn(Map.of("seatcart:" + eventId + ":" + seatId, userId.toString()));

        Set<UUID> result = seatCartService.findPendingSeatIds(eventId);

        assertEquals(Set.of(seatId), result);
        verify(setCommands, times(1)).srem(indexKey(), seat2.toString());
    }

    @Test
    void findPendingSeatIds_NoHolds_ReturnsEmptySet() {
        when(setCommands.smembers(indexKey())).thenReturn(Collections.emptySet());

        Set<UUID> result = seatCartService.findPendingSeatIds(eventId);

        assertTrue(result.isEmpty());
        verify(valueCommands, never()).mget(any(String[].class));
    }

    @Test
    void grantAccess_SetsAccessKeyWithAllowedCountAndSlidingTtl() {
        seatCartService.grantAccess(eventId, userId, ALLOWED_COUNT);

        verify(accessGrantStore, times(1))
                .set(eventId, userId, ALLOWED_COUNT, Duration.ofSeconds(ACCESS_GRANT_TTL_SECONDS));
    }
}
