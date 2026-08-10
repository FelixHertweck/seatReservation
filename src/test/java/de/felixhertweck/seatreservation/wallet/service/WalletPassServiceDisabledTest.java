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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.util.Map;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(WalletPassServiceDisabledTest.DisabledProfile.class)
class WalletPassServiceDisabledTest {

    public static class DisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "wallet.google.enabled", "false",
                    "wallet.apple.enabled", "false",
                    "wallet.generic.enabled", "false");
        }
    }

    @Inject WalletPassService walletPassService;

    @Test
    void testWalletProvidersDisabled() {
        assertFalse(walletPassService.isGoogleWalletEnabled());
        assertFalse(walletPassService.isAppleWalletEnabled());
        assertFalse(walletPassService.isGenericWalletEnabled());
    }

    @Test
    void testGeneratePassThrowsIllegalStateWhenDisabled() {
        User user = new User();
        user.id = id(1);

        assertThrows(
                IllegalStateException.class,
                () -> walletPassService.generatePass(id(999), user, WalletProvider.GOOGLE));
        assertThrows(
                IllegalStateException.class,
                () -> walletPassService.generatePass(id(999), user, WalletProvider.APPLE));
        assertThrows(
                IllegalStateException.class,
                () -> walletPassService.generatePass(id(999), user, WalletProvider.GENERIC_PKPASS));
    }
}
