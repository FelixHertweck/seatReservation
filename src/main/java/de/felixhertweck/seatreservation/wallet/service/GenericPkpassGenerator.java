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

import java.util.HashMap;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Generates a generic, unsigned PKPASS (.pkpass) archive without requiring Apple Developer Account
 * certificates or Apple-specific credentials.
 */
@ApplicationScoped
public class GenericPkpassGenerator extends AbstractPkpassGenerator {

    private static final Logger LOG = Logger.getLogger(GenericPkpassGenerator.class);

    @ConfigProperty(
            name = "wallet.generic.pass-type-identifier",
            defaultValue = "pass.de.felixhertweck.seatreservation")
    String passTypeIdentifier;

    @ConfigProperty(name = "wallet.generic.team-id", defaultValue = "GENERIC123")
    String teamId;

    @Override
    public WalletProvider getProvider() {
        return WalletProvider.GENERIC_PKPASS;
    }

    @Override
    public WalletPassResponseDTO generatePass(WalletPassData data) {
        return generatePass(java.util.List.of(data));
    }

    @Override
    public WalletPassResponseDTO generatePass(java.util.List<WalletPassData> allSeatReservations) {
        if (allSeatReservations == null || allSeatReservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations provided");
        }
        WalletPassData first = allSeatReservations.get(0);

        if (allSeatReservations.size() == 1) {
            LOG.debugf(
                    "Generating Generic PKPass (.pkpass) for single reservation ID: %s",
                    first.reservationId());
            try {
                byte[] passJsonBytes = buildPassJson(first, passTypeIdentifier, teamId);
                byte[] manifestBytes = buildManifest(Map.of("pass.json", passJsonBytes));
                byte[] pkpass = buildZip(passJsonBytes, manifestBytes, null);
                String filename = String.format("ticket_%s.pkpass", first.reservationId());
                return WalletPassResponseDTO.forGenericPkpass(pkpass, filename);
            } catch (Exception e) {
                LOG.errorf(
                        e,
                        "Error generating Generic PKPass for reservation ID %s",
                        first.reservationId());
                throw new RuntimeException("Failed to generate Generic PKPass", e);
            }
        } else {
            LOG.debugf(
                    "Generating Generic PKPasses Bundle (.pkpasses) for %d seat(s), event ID: %s",
                    allSeatReservations.size(), first.eventId());
            try {
                Map<String, byte[]> bundleEntries = new HashMap<>();
                for (int i = 0; i < allSeatReservations.size(); i++) {
                    WalletPassData seatData = allSeatReservations.get(i);
                    byte[] passJsonBytes = buildPassJson(seatData, passTypeIdentifier, teamId);
                    byte[] manifestBytes = buildManifest(Map.of("pass.json", passJsonBytes));
                    byte[] singlePkpass = buildZip(passJsonBytes, manifestBytes, null);
                    bundleEntries.put(
                            String.format("pass_%d_%s.pkpass", i + 1, seatData.reservationId()),
                            singlePkpass);
                }
                byte[] bundleBytes = buildPkpassesBundle(bundleEntries);
                String filename = String.format("tickets_%s.pkpasses", first.eventId());
                return WalletPassResponseDTO.forGenericPkpassBundle(bundleBytes, filename);
            } catch (Exception e) {
                LOG.errorf(
                        e,
                        "Error generating Generic PKPasses Bundle for event ID %s",
                        first.eventId());
                throw new RuntimeException("Failed to generate Generic PKPasses Bundle", e);
            }
        }
    }
}
