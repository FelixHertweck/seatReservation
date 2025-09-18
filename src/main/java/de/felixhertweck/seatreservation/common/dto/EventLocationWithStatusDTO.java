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

import java.util.List;

import de.felixhertweck.seatreservation.model.entity.EventLocation;

public record EventLocationWithStatusDTO(
        Long id,
        String name,
        String address,
        Integer capacity,
        LimitedUserInfoDTO manager,
        List<SeatWithStatusDTO> seats) {

    public static EventLocationWithStatusDTO toDTO(
            EventLocation eventLocation, List<SeatWithStatusDTO> seats) {
        return new EventLocationWithStatusDTO(
                eventLocation.getId(),
                eventLocation.getName(),
                eventLocation.getAddress(),
                eventLocation.getCapacity(),
                eventLocation.getManager() != null
                        ? LimitedUserInfoDTO.toDTO(eventLocation.getManager())
                        : null,
                seats);
    }
}
