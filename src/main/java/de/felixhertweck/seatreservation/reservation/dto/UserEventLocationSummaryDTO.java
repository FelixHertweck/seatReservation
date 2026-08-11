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
package de.felixhertweck.seatreservation.reservation.dto;

import java.util.UUID;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Lightweight summary DTO for user-facing event location list views, excluding structural seat map
 * details.
 *
 * @param id location ID
 * @param name location name
 * @param address location address
 */
@RegisterForReflection
public record UserEventLocationSummaryDTO(UUID id, String name, String address) {

    /**
     * Constructs a summary DTO from an EventLocation entity.
     *
     * @param eventLocation the event location entity
     */
    public UserEventLocationSummaryDTO(EventLocation eventLocation) {
        this(eventLocation.getId(), eventLocation.getName(), eventLocation.getAddress());
    }
}
