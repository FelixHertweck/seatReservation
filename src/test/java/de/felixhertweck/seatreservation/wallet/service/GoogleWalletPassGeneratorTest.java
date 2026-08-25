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
package de.felixhertweck.seatreservation.wallet.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.felixhertweck.seatreservation.common.events.EventCancelledEvent;
import de.felixhertweck.seatreservation.common.events.EventCreatedEvent;
import de.felixhertweck.seatreservation.common.events.EventDeletedEvent;
import de.felixhertweck.seatreservation.common.events.EventUpdatedEvent;
import de.felixhertweck.seatreservation.common.events.ReservationCancelledEvent;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GoogleWalletPassGeneratorTest {

    @Inject GoogleWalletPassGenerator googleWalletPassGenerator;

    @Test
    void testGetProvider() {
        assertEquals(WalletProvider.GOOGLE, googleWalletPassGenerator.getProvider());
    }

    @Test
    void testOnEventCreatedWhenDisabledDoesNotThrow() {
        googleWalletPassGenerator.googleWalletEnabled = false;

        EventCreatedEvent event =
                new EventCreatedEvent(
                        UUID.randomUUID(),
                        "Brand New Festival",
                        "Main Hall",
                        "123 Music St",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        null);

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventCreated(event));
    }

    @Test
    void testOnEventCreatedWhenEnabledHandlesBestEffortFailure() {
        googleWalletPassGenerator.googleWalletEnabled = true;

        EventCreatedEvent event =
                new EventCreatedEvent(
                        UUID.randomUUID(),
                        "Brand New Festival",
                        "Main Hall",
                        "123 Music St",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        null);

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventCreated(event));
    }

    @Test
    void testOnEventUpdatedWhenDisabledDoesNotThrow() {
        googleWalletPassGenerator.googleWalletEnabled = false;

        EventUpdatedEvent event =
                new EventUpdatedEvent(
                        UUID.randomUUID(),
                        "New Festival Name",
                        "Main Hall",
                        "123 Music St",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        null);

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventUpdated(event));
    }

    @Test
    void testOnEventUpdatedWhenEnabledHandlesBestEffortFailure() {
        googleWalletPassGenerator.googleWalletEnabled = true;

        EventUpdatedEvent event =
                new EventUpdatedEvent(
                        UUID.randomUUID(),
                        "Updated Festival",
                        "Stadthalle",
                        "Haupstrasse 1",
                        Instant.now(),
                        Instant.now().plusSeconds(7200),
                        null);

        // When key file or remote API is missing/unreachable, onEventUpdated catches exception
        // gracefully (best-effort)
        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventUpdated(event));
    }

    @Test
    void testOnReservationCancelledWhenDisabledDoesNotThrow() {
        googleWalletPassGenerator.googleWalletEnabled = false;

        ReservationCancelledEvent event =
                new ReservationCancelledEvent(null, List.of(new Reservation()), List.of());

        assertDoesNotThrow(() -> googleWalletPassGenerator.onReservationCancelled(event));
    }

    @Test
    void testOnReservationCancelledWhenEnabledHandlesBestEffortFailure() {
        googleWalletPassGenerator.googleWalletEnabled = true;

        Reservation reservation = new Reservation();
        reservation.id = UUID.randomUUID();
        ReservationCancelledEvent event =
                new ReservationCancelledEvent(null, List.of(reservation), List.of());

        // Key file or remote API is missing/unreachable in the test environment; the observer
        // must swallow the failure (best-effort) rather than propagate it.
        assertDoesNotThrow(() -> googleWalletPassGenerator.onReservationCancelled(event));
    }

    @Test
    void testOnEventCancelledWhenDisabledDoesNotThrow() {
        googleWalletPassGenerator.googleWalletEnabled = false;

        EventCancelledEvent event =
                new EventCancelledEvent(
                        UUID.randomUUID(),
                        "Festival",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        "Main Hall",
                        "Weather",
                        List.of(new Reservation()));

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventCancelled(event));
    }

    @Test
    void testOnEventCancelledWhenEnabledHandlesBestEffortFailure() {
        googleWalletPassGenerator.googleWalletEnabled = true;

        Reservation reservation = new Reservation();
        reservation.id = UUID.randomUUID();
        EventCancelledEvent event =
                new EventCancelledEvent(
                        UUID.randomUUID(),
                        "Festival",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        "Main Hall",
                        "Weather",
                        List.of(reservation));

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventCancelled(event));
    }

    @Test
    void testOnEventDeletedWhenDisabledDoesNotThrow() {
        googleWalletPassGenerator.googleWalletEnabled = false;

        EventDeletedEvent event =
                new EventDeletedEvent(UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventDeleted(event));
    }

    @Test
    void testOnEventDeletedWhenEnabledHandlesBestEffortFailure() {
        googleWalletPassGenerator.googleWalletEnabled = true;

        EventDeletedEvent event =
                new EventDeletedEvent(UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventDeleted(event));
    }
}
