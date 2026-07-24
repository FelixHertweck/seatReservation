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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeatCartServiceTest {

    private static final long TTL_SECONDS = 300;

    private ReservationRepository reservationRepository;
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;
    private SeatCartService seatCartService;

    private final UUID eventId = id(1);
    private final UUID seatId = id(2);
    private final UUID userId = id(3);
    private final UUID otherUserId = id(4);

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        valueCommands = mock(ValueCommands.class);
        keyCommands = mock(KeyCommands.class);

        RedisDataSource redisDataSource = mock(RedisDataSource.class);
        when(redisDataSource.value(String.class)).thenReturn(valueCommands);
        when(redisDataSource.key(String.class)).thenReturn(keyCommands);

        seatCartService = new SeatCartService(redisDataSource);
        seatCartService.reservationRepository = reservationRepository;
        seatCartService.ttlSeconds = TTL_SECONDS;

        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(Collections.emptyList());
    }

    private String key() {
        return "seatcart:" + eventId + ":" + seatId;
    }

    @Test
    void addSeatToCart_Success_NewHold() {
        when(valueCommands.setGet(eq(key()), eq(userId.toString()), any(SetArgs.class)))
                .thenReturn(null);

        SeatCartEntryDTO result = seatCartService.addSeatToCart(eventId, seatId, userId);

        assertEquals(seatId, result.seatId());
        assertTrue(result.expiresAt().isAfter(Instant.now()));
        verify(valueCommands, never()).set(anyString(), anyString(), any(SetArgs.class));
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
    void addSeatToCart_SeatAlreadyReserved_ThrowsWithoutTouchingRedis() {
        Reservation reserved = mock(Reservation.class);
        when(reserved.getStatus()).thenReturn(ReservationStatus.RESERVED);
        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(List.of(reserved));

        assertThrows(
                SeatAlreadyReservedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(valueCommands, never()).setGet(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void addSeatToCart_SeatBlocked_ThrowsWithoutTouchingRedis() {
        Reservation blocked = mock(Reservation.class);
        when(blocked.getStatus()).thenReturn(ReservationStatus.BLOCKED);
        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(List.of(blocked));

        assertThrows(
                SeatBlockedException.class,
                () -> seatCartService.addSeatToCart(eventId, seatId, userId));
        verify(valueCommands, never()).setGet(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void removeSeatFromCart_OwnedByUser_DeletesKey() {
        when(valueCommands.get(key())).thenReturn(userId.toString());

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, times(1)).del(key());
    }

    @Test
    void removeSeatFromCart_OwnedByAnotherUser_DoesNotDelete() {
        when(valueCommands.get(key())).thenReturn(otherUserId.toString());

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, never()).del(anyString());
    }

    @Test
    void removeSeatFromCart_NotHeld_DoesNotDelete() {
        when(valueCommands.get(key())).thenReturn(null);

        seatCartService.removeSeatFromCart(eventId, seatId, userId);

        verify(keyCommands, never()).del(anyString());
    }

    @Test
    void releaseSeats_EmptyList_DoesNotCallRedis() {
        seatCartService.releaseSeats(eventId, Collections.emptyList());

        verify(keyCommands, never()).del(any(String[].class));
    }

    @Test
    void releaseSeats_WithSeats_DeletesAllKeys() {
        UUID seat2 = id(5);

        seatCartService.releaseSeats(eventId, List.of(seatId, seat2));

        verify(keyCommands, times(1))
                .del(
                        eq("seatcart:" + eventId + ":" + seatId),
                        eq("seatcart:" + eventId + ":" + seat2));
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
    void findPendingSeatIds_ParsesSeatIdsFromKeys() {
        UUID seat2 = id(6);
        when(keyCommands.keys("seatcart:" + eventId + ":*"))
                .thenReturn(
                        List.of(
                                "seatcart:" + eventId + ":" + seatId,
                                "seatcart:" + eventId + ":" + seat2));

        Set<UUID> result = seatCartService.findPendingSeatIds(eventId);

        assertEquals(Set.of(seatId, seat2), result);
    }

    @Test
    void findPendingSeatIds_NoHolds_ReturnsEmptySet() {
        when(keyCommands.keys("seatcart:" + eventId + ":*")).thenReturn(Collections.emptyList());

        Set<UUID> result = seatCartService.findPendingSeatIds(eventId);

        assertTrue(result.isEmpty());
    }
}
