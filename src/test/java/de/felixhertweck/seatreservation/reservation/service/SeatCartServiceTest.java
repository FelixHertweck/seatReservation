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

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeatCartServiceTest {

    private static final long TTL_SECONDS = 300;
    private static final long ACCESS_GRANT_TTL_SECONDS = TTL_SECONDS + 30;

    private ReservationRepository reservationRepository;
    private EventRepository eventRepository;
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
        eventRepository = mock(EventRepository.class);
        valueCommands = mock(ValueCommands.class);
        keyCommands = mock(KeyCommands.class);
        setCommands = mock(SetCommands.class);

        RedisDataSource redisDataSource = mock(RedisDataSource.class);
        when(redisDataSource.value(String.class)).thenReturn(valueCommands);
        when(redisDataSource.key(String.class)).thenReturn(keyCommands);
        when(redisDataSource.set(String.class)).thenReturn(setCommands);

        seatCartService = new SeatCartService(redisDataSource);
        seatCartService.reservationRepository = reservationRepository;
        seatCartService.eventRepository = eventRepository;
        seatCartService.ttlSeconds = TTL_SECONDS;

        // Default: access already granted, event already seeded - most tests exercise only the
        // Redis-only hot path. Tests that care about seeding/access-denial override this.
        when(valueCommands.get(accessKey())).thenReturn("1");
        when(valueCommands.get(seededKey())).thenReturn("1");
    }

    private String key() {
        return "seatcart:" + eventId + ":" + seatId;
    }

    private String key(UUID seat) {
        return "seatcart:" + eventId + ":" + seat;
    }

    private String accessKey() {
        return "seatcart:access:" + eventId + ":" + userId;
    }

    private String indexKey() {
        return "seatcart:idx:" + eventId;
    }

    private String seededKey() {
        return "seatcart:seeded:" + eventId;
    }

    private String seedingLockKey() {
        return "seatcart:seeding:" + eventId;
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

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        assertTrue(result.expiresAt().isAfter(Instant.now()));
        // No TTL-refresh call for the seat hold itself (new hold, NX already set it).
        verify(valueCommands, never()).set(eq(key()), anyString(), any(SetArgs.class));
        verify(setCommands, times(1)).sadd(indexKey(), seatId.toString());
        // Sliding window: access grant TTL pushed out again on every successful cart write.
        verify(keyCommands, times(1))
                .expire(eq(accessKey()), eq(Duration.ofSeconds(ACCESS_GRANT_TTL_SECONDS)));
    }

    @Test
    void addSeatToCart_AlreadyHeldBySameUser_RefreshesTtl() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(userId.toString());

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        verify(valueCommands, times(1)).set(eq(key()), eq(userId.toString()), any(SetArgs.class));
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
    void addSeatToCart_ReservedSentinel_ThrowsWithoutTouchingDb() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn("RESERVED");

        assertThrows(
                SeatAlreadyReservedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(reservationRepository, never()).findByEventIdAndSeatIds(any(), any());
        verify(reservationRepository, never()).findByEventId(any());
    }

    @Test
    void addSeatToCart_BlockedSentinel_ThrowsWithoutTouchingDb() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn("BLOCKED");

        assertThrows(
                SeatBlockedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(reservationRepository, never()).findByEventIdAndSeatIds(any(), any());
        verify(reservationRepository, never()).findByEventId(any());
    }

    @Test
    void addSeatToCart_AccessNotGranted_ThrowsWithoutTouchingRedisHold() {
        when(valueCommands.get(accessKey())).thenReturn(null);

        assertThrows(
                SeatCartAccessNotGrantedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(valueCommands, never()).setGet(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void addSeatToCart_EventNotYetSeeded_SeedsFromDatabaseThenServesFromRedis() {
        when(valueCommands.get(seededKey())).thenReturn(null);
        when(valueCommands.setnx(seedingLockKey(), "1")).thenReturn(true);

        Event event = mock(Event.class);
        when(event.getEndTime()).thenReturn(Instant.now().plusSeconds(3600));
        when(eventRepository.findByIdOptional(eventId)).thenReturn(Optional.of(event));

        UUID reservedSeatId = id(10);
        UUID blockedSeatId = id(11);
        List<Reservation> seededReservations =
                List.of(
                        reservationWith(reservedSeatId, ReservationStatus.RESERVED),
                        reservationWith(blockedSeatId, ReservationStatus.BLOCKED));
        when(reservationRepository.findByEventId(eventId)).thenReturn(seededReservations);

        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);

        seatCartService.addSeatToCart(eventId, seatId, userId);

        verify(valueCommands, times(1))
                .set(eq(key(reservedSeatId)), eq("RESERVED"), any(SetArgs.class));
        verify(valueCommands, times(1))
                .set(eq(key(blockedSeatId)), eq("BLOCKED"), any(SetArgs.class));
        verify(valueCommands, times(1)).set(eq(seededKey()), eq("1"), any(SetArgs.class));
        verify(keyCommands, times(1)).expire(eq(seedingLockKey()), any(Duration.class));
        verify(keyCommands, times(1)).del(seedingLockKey());
    }

    @Test
    void addSeatToCart_EventAlreadySeeded_DoesNotReseed() {
        // seededKey() already returns "1" via setUp's default stub.
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);

        seatCartService.addSeatToCart(eventId, seatId, userId);

        verify(reservationRepository, never()).findByEventId(any());
        verify(valueCommands, never()).setnx(anyString(), anyString());
    }

    @Test
    void addSeatToCart_SeedingLockContended_FallsBackToSingleSeatDbCheck() {
        when(valueCommands.get(seededKey())).thenReturn(null);
        when(valueCommands.setnx(seedingLockKey(), "1")).thenReturn(false);
        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(Collections.emptyList());
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        verify(reservationRepository, times(1)).findByEventIdAndSeatIds(eventId, List.of(seatId));
        verify(reservationRepository, never()).findByEventId(any());
    }

    @Test
    void removeSeatFromCart_OwnedByUser_DeletesKey() {
        when(valueCommands.get(key())).thenReturn(userId.toString());

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, times(1)).del(key());
        verify(setCommands, times(1)).srem(indexKey(), seatId.toString());
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
    void markSeatsReserved_EmptyList_DoesNotCallRedis() {
        seatCartService.markSeatsReserved(eventId, Collections.emptyList(), Instant.now());

        verify(valueCommands, never()).set(anyString(), anyString(), any(SetArgs.class));
        verify(setCommands, never()).srem(anyString(), anyString());
    }

    @Test
    void markSeatsReserved_WithSeats_SetsReservedSentinelAndPrunesIndex() {
        UUID seat2 = id(5);
        Instant eventEndTime = Instant.now().plusSeconds(3600);

        seatCartService.markSeatsReserved(eventId, List.of(seatId, seat2), eventEndTime);

        verify(valueCommands, times(1)).set(eq(key()), eq("RESERVED"), any(SetArgs.class));
        verify(valueCommands, times(1)).set(eq(key(seat2)), eq("RESERVED"), any(SetArgs.class));
        verify(setCommands, times(1)).srem(indexKey(), seatId.toString(), seat2.toString());
    }

    @Test
    void markSeatsBlocked_WithSeats_SetsBlockedSentinelAndPrunesIndex() {
        Instant eventEndTime = Instant.now().plusSeconds(3600);

        seatCartService.markSeatsBlocked(eventId, List.of(seatId), eventEndTime);

        verify(valueCommands, times(1)).set(eq(key()), eq("BLOCKED"), any(SetArgs.class));
        verify(setCommands, times(1)).srem(indexKey(), seatId.toString());
    }

    @Test
    void freeSeats_EmptyList_DoesNotCallRedis() {
        seatCartService.freeSeats(eventId, Collections.emptyList());

        verify(keyCommands, never()).del(any(String[].class));
        verify(setCommands, never()).srem(anyString(), anyString());
    }

    @Test
    void freeSeats_WithSeats_DeletesAllKeys() {
        UUID seat2 = id(5);

        seatCartService.freeSeats(eventId, List.of(seatId, seat2));

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
    void grantAccess_SetsAccessKeyWithSlidingTtl() {
        seatCartService.grantAccess(eventId, userId);

        verify(valueCommands, times(1)).set(eq(accessKey()), eq("1"), any(SetArgs.class));
    }
}
