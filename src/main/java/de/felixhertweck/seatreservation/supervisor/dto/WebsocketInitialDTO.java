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
package de.felixhertweck.seatreservation.supervisor.dto;

import java.util.List;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WebsocketInitialDTO(
        String type,
        SupervisorEventLocationDTO location,
        SupervisorEventResponseDTO event,
        List<SupervisorReservationResponseDTO> reservations) {
    /**
     * @param reservations the already-mapped reservation DTOs (including any box office {@code
     *     guestName}), built by the caller since guest names require a separate repository lookup
     *     that doesn't belong in this DTO
     */
    public static WebsocketInitialDTO initial(
            EventLocation location,
            Event event,
            List<SupervisorReservationResponseDTO> reservations) {
        return new WebsocketInitialDTO(
                "INITIAL",
                new SupervisorEventLocationDTO(location),
                new SupervisorEventResponseDTO(event),
                reservations);
    }
}
