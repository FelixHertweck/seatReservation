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

import com.fasterxml.jackson.annotation.JsonInclude;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeatDTO(
        Long id,
        String seatNumber,
        String seatRow,
        Long locationId,
        int xCoordinate,
        int yCoordinate,
        ReservationStatus status) {
    public SeatDTO(Seat seat) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getSeatRow(),
                seat.getLocation().id,
                seat.getxCoordinate(),
                seat.getyCoordinate(),
                null);
    }

    public SeatDTO(Seat seat, ReservationStatus status) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getSeatRow(),
                seat.getLocation().id,
                seat.getxCoordinate(),
                seat.getyCoordinate(),
                status);
    }
}
