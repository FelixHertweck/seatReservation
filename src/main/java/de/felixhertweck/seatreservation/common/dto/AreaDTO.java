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

import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a named area within an event location together with the ids of all seats assigned to
 * it. Areas are derived by grouping the seats of a location by their {@code area} value and are
 * intended to be rendered as an overlay on top of the seat map. Only seat ids are referenced here
 * since the full seat data is already part of the enclosing response.
 */
@RegisterForReflection
public record AreaDTO(String name, List<Long> seatIds) {

    /**
     * Groups the given seats by their area name into a list of {@link AreaDTO}s. Seats without an
     * area (null or blank) are ignored. The resulting group order follows the order in which the
     * areas are first encountered.
     *
     * @param seats the seats to group; may be {@code null}
     * @return a list of areas with their assigned seat ids, never {@code null}
     */
    public static List<AreaDTO> fromSeats(Collection<Seat> seats) {
        if (seats == null) {
            return List.of();
        }
        Map<String, List<Long>> grouped = new LinkedHashMap<>();
        for (Seat seat : seats) {
            String area = seat.getArea();
            if (area == null || area.trim().isEmpty()) {
                continue;
            }
            grouped.computeIfAbsent(area.trim(), key -> new ArrayList<>()).add(seat.id);
        }
        List<AreaDTO> result = new ArrayList<>();
        grouped.forEach((name, seatIds) -> result.add(new AreaDTO(name, seatIds)));
        return result;
    }
}
