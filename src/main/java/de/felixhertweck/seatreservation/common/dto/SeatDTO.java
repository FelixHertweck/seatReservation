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

import com.fasterxml.jackson.annotation.JsonInclude;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Seat projection returned by every read endpoint.
 *
 * <p>Entrance and area are exposed both as {@code entrance}/{@code area} (the display name) and as
 * {@code entranceId}/{@code areaId} (the FK). The names are deliberately kept: the seat map and the
 * ticket/email rendering label seats without having to resolve the ids, while the ids are what the
 * management forms submit back. Do not drop the names without adapting those consumers.
 *
 * <p>String fields are marked {@code @NoHtmlSanitize}: this is a server-generated projection, never
 * bound directly from client input (that goes through {@code SeatRequestDTO}, which is sanitized).
 * It is, however, deserialized on every Redis cache hit ({@code SeatmapCacheService}), so
 * re-sanitizing here would be pure overhead on the hottest read path.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public record SeatDTO(
        UUID id,
        @NoHtmlSanitize String seatNumber,
        @NoHtmlSanitize String seatRow,
        UUID locationId,
        CoordinateDTO coordinate,
        @NoHtmlSanitize String entrance,
        @NoHtmlSanitize String area,
        UUID entranceId,
        UUID areaId) {
    public SeatDTO(Seat seat) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getSeatRow(),
                seat.getLocation().id,
                new CoordinateDTO(seat.getCoordinate()),
                seat.getEntrance() != null ? seat.getEntrance().getName() : null,
                seat.getArea() != null ? seat.getArea().getName() : null,
                seat.getEntrance() != null ? seat.getEntrance().id : null,
                seat.getArea() != null ? seat.getArea().id : null);
    }
}
