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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a named area of an event location together with the ids of all seats assigned to it.
 * Areas are intended to be rendered as an overlay on top of the seat map. Only seat ids are
 * referenced here since the full seat data is already part of the enclosing response.
 *
 * <p>{@code boundary} is an optional custom polygon (in the same coordinate system as {@code
 * Seat.coordinate}) that a renderer should use instead of deriving a shape from the member seats'
 * positions. When {@code null} or empty, renderers fall back to computing a bounding box from the
 * area's seats.
 */
@RegisterForReflection
public record AreaDTO(UUID id, String name, List<UUID> seatIds, List<CoordinateDTO> boundary) {

    /**
     * Maps an event location's areas to {@link AreaDTO}s, attaching the ids of the seats assigned
     * to each area.
     *
     * <p>The areas drive the result, not the seats: an area is reported even when no seat
     * references it yet (with an empty {@code seatIds}), so that a freshly drawn boundary is
     * visible on the map right away. The result follows the location's own area order.
     *
     * @param eventLocation the event location whose areas to map; may be {@code null}
     * @return the location's areas with their assigned seat ids, never {@code null}
     */
    public static List<AreaDTO> fromEventLocation(EventLocation eventLocation) {
        if (eventLocation == null) {
            return List.of();
        }
        return fromAreas(eventLocation.getAreas(), eventLocation.getSeats());
    }

    /**
     * Maps the given areas to {@link AreaDTO}s, attaching the ids of those seats that reference
     * them. Seats without an area, and seats referencing an area not present in {@code areas}, are
     * ignored.
     *
     * @param areas the areas to map; may be {@code null}
     * @param seats the seats to take the area assignment from; may be {@code null}
     * @return the areas with their assigned seat ids, never {@code null}
     */
    public static List<AreaDTO> fromAreas(
            Collection<EventLocationArea> areas, Collection<Seat> seats) {
        if (areas == null || areas.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<UUID>> seatIdsByAreaId = new LinkedHashMap<>();
        if (seats != null) {
            for (Seat seat : seats) {
                EventLocationArea area = seat.getArea();
                if (area == null) {
                    continue;
                }
                seatIdsByAreaId.computeIfAbsent(area.id, key -> new ArrayList<>()).add(seat.id);
            }
        }

        return areas.stream()
                .map(
                        area -> {
                            List<CoordinateDTO> boundary =
                                    (area.getBoundary() == null || area.getBoundary().isEmpty())
                                            ? null
                                            : area.getBoundary().stream()
                                                    .map(CoordinateDTO::new)
                                                    .toList();
                            return new AreaDTO(
                                    area.id,
                                    area.getName(),
                                    seatIdsByAreaId.getOrDefault(area.id, List.of()),
                                    boundary);
                        })
                .toList();
    }
}
