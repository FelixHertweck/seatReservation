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
package de.felixhertweck.seatreservation.management.dto;

import java.util.List;

import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record EventLocationResponseDTO(
        Long id,
        String name,
        String address,
        Integer capacity,
        LimitedUserInfoDTO manager,
        List<Long> seatIds,
        List<EventLocationMakerDTO> markers) {
    public EventLocationResponseDTO(EventLocation eventLocation) {
        this(
                eventLocation.getId(),
                eventLocation.getName(),
                eventLocation.getAddress(),
                eventLocation.getCapacity(),
                (eventLocation.getManager() != null
                        ? new LimitedUserInfoDTO(eventLocation.getManager())
                        : null),
                (eventLocation.getSeats() != null
                        ? eventLocation.getSeats().stream().map(Seat::getId).toList()
                        : List.of()),
                (eventLocation.getMarkers() != null
                        ? eventLocation.getMarkers().stream()
                                .map(EventLocationMakerDTO::new)
                                .toList()
                        : List.of()));
    }
}
