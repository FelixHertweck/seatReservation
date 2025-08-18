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

import java.util.Set;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.common.dto.AreaDTO;
import de.felixhertweck.seatreservation.common.dto.MarkerDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;

public class EventLocationRequestDTO {
    @NotNull(message = "Name must not be null")
    private String name;

    @NotNull(message = "Address must not be null")
    private String address;

    @NotNull(message = "Capacity must not be null")
    private Integer capacity;

    private Set<MarkerDTO> markers;

    private Set<AreaDTO> areas;

    private Set<SeatDTO> seats;

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Set<MarkerDTO> getMarkers() {
        return markers;
    }

    public Set<AreaDTO> getAreas() {
        return areas;
    }

    public Set<SeatDTO> getSeats() {
        return seats;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setMarkers(Set<MarkerDTO> markers) {
        this.markers = markers;
    }

    public void setAreas(Set<AreaDTO> areas) {
        this.areas = areas;
    }

    public void setSeats(Set<SeatDTO> seats) {
        this.seats = seats;
    }

    public EventLocationRequestDTO() {}
}
