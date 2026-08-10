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

import java.time.Instant;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WalletPassData(
        UUID reservationId,
        UUID eventId,
        String eventName,
        String eventDescription,
        Instant startTime,
        Instant endTime,
        String locationName,
        String locationAddress,
        String seatLabel,
        String sectionName,
        String rowName,
        String seatName,
        UUID userId,
        String userName,
        String userEmail,
        String checkInToken,
        String qrCodePayload) {}
