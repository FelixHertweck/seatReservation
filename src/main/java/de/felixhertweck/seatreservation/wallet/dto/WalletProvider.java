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
package de.felixhertweck.seatreservation.wallet.dto;

public enum WalletProvider {
    GOOGLE,
    APPLE,
    GENERIC_PKPASS;

    public static WalletProvider fromString(String value) {
        if (value == null) {
            return null;
        }
        for (WalletProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        if ("GENERIC".equalsIgnoreCase(value) || "PKPASS".equalsIgnoreCase(value)) {
            return GENERIC_PKPASS;
        }
        throw new IllegalArgumentException("Unknown wallet provider: " + value);
    }
}
