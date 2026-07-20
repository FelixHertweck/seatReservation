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

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SeatRequestDTO {
    @NotNull(message = "Seat number must not be null")
    private String seatNumber;

    @NotNull(message = "EventLocation ID must not be null")
    private Long eventLocationId;

    @NotNull(message = "Coordinate must not be null")
    @Valid
    private CoordinateDTO coordinate;

    @NotNull(message = "Row must not be null")
    private String seatRow;

    @Nullable private String entrance;

    @Nullable private String area;

    public SeatRequestDTO() {
        // Default constructor for serialization/deserialization
    }

    public SeatRequestDTO(
            String seatNumber,
            String seatRow,
            Long eventLocationId,
            int xCoordinate,
            int yCoordinate,
            String entrance,
            String area) {
        this.seatNumber = seatNumber;
        this.eventLocationId = eventLocationId;
        this.coordinate = new CoordinateDTO(xCoordinate, yCoordinate);
        this.seatRow = seatRow;
        this.entrance = entrance;
        this.area = area;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Long getEventLocationId() {
        return eventLocationId;
    }

    public CoordinateDTO getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(CoordinateDTO coordinate) {
        this.coordinate = coordinate;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setEventLocationId(Long eventLocationId) {
        this.eventLocationId = eventLocationId;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public String getEntrance() {
        return entrance;
    }

    public void setEntrance(String entrance) {
        this.entrance = entrance;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}
