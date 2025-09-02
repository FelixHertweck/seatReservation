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
package de.felixhertweck.seatreservation.eventManagement.dto;

import jakarta.validation.constraints.NotNull;

public class SeatRequestDTO {
    @NotNull(message = "Seat number must not be null")
    private String seatNumber;

    @NotNull(message = "EventLocation ID must not be null")
    private Long eventLocationId;

    @NotNull(message = "X coordinate must not be null")
    private Integer xCoordinate;

    @NotNull(message = "Y coordinate must not be null")
    private Integer yCoordinate;

    public SeatRequestDTO() {
        // Default constructor for serialization/deserialization
    }

    public SeatRequestDTO(
            String seatNumber, Long eventLocationId, int xCoordinate, int yCoordinate) {
        this.seatNumber = seatNumber;
        this.eventLocationId = eventLocationId;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Long getEventLocationId() {
        return eventLocationId;
    }

    public Integer getxCoordinate() {
        return xCoordinate;
    }

    public Integer getyCoordinate() {
        return yCoordinate;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setEventLocationId(Long eventLocationId) {
        this.eventLocationId = eventLocationId;
    }

    public void setxCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public void setyCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
    }
}
