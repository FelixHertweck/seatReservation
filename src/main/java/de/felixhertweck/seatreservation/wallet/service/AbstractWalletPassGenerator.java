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
package de.felixhertweck.seatreservation.wallet.service;

import java.util.List;

import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;

public abstract class AbstractWalletPassGenerator {

    public abstract WalletProvider getProvider();

    public abstract WalletPassResponseDTO generatePass(WalletPassData data);

    /**
     * Generates a pass for all seat reservations of the same user+event in a single entry.
     *
     * <p>The default implementation falls back to the single-reservation method using the first
     * element. Subclasses may override this to bundle multiple seats into one pass.
     *
     * @param allSeatReservations all reservations belonging to the same user and event, ordered by
     *     seat; must not be empty
     */
    public WalletPassResponseDTO generatePass(List<WalletPassData> allSeatReservations) {
        if (allSeatReservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations provided");
        }
        return generatePass(allSeatReservations.get(0));
    }

    protected String buildQrCodePayload(WalletPassData data) {
        if (data.qrCodePayload() != null && !data.qrCodePayload().isBlank()) {
            return data.qrCodePayload();
        }
        return String.format("%s;%s;%s", data.userId(), data.eventId(), data.checkInToken());
    }
}
