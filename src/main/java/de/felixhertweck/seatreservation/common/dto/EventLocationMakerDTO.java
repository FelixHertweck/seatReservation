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
package de.felixhertweck.seatreservation.common.dto;

import java.util.UUID;

import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Server-generated projection, never bound directly from client input (that goes through {@code
 * MakerRequestDTO}, which is sanitized). {@code label} is marked {@code @NoHtmlSanitize} since this
 * DTO is also deserialized on every Redis cache hit ({@code SeatmapCacheService}), where
 * re-sanitizing already-clean data would be pure overhead on the hottest read path.
 */
@RegisterForReflection
public record EventLocationMakerDTO(
        UUID id, @NoHtmlSanitize String label, CoordinateDTO coordinate, UUID eventLocationId) {
    public EventLocationMakerDTO(EventLocationMarker maker) {
        this(
                maker.id,
                maker.getLabel(),
                new CoordinateDTO(maker.getCoordinate()),
                maker.getEventLocation().getId());
    }
}
