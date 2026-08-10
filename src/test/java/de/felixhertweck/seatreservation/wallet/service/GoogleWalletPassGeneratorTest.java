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
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.felixhertweck.seatreservation.common.events.EventUpdatedEvent;
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
    void testOnEventUpdatedWhenDisabledDoesNotThrow() {
        googleWalletPassGenerator.googleWalletEnabled = false;

        EventUpdatedEvent event =
                new EventUpdatedEvent(
                        UUID.randomUUID(),
                        "New Festival Name",
                        "Main Hall",
                        "123 Music St",
                        Instant.now(),
                        Instant.now().plusSeconds(3600));

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
                        Instant.now().plusSeconds(7200));

        // When key file or remote API is missing/unreachable, onEventUpdated catches exception
        // gracefully (best-effort)
        assertDoesNotThrow(() -> googleWalletPassGenerator.onEventUpdated(event));
    }
}
