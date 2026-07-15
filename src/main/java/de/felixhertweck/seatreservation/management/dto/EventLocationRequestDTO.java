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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class EventLocationRequestDTO {
    @NotNull(message = "Name must not be null")
    private String name;

    @NotNull(message = "Address must not be null")
    private String address;

    @NotNull(message = "Capacity must not be null")
    private Integer capacity;

    /**
     * On update, this is a full replace, not a merge: omitting this field (or sending {@code null})
     * clears all of the location's existing markers. Send the complete desired list every time,
     * including markers that are meant to be kept.
     */
    @Valid private List<MakerRequestDTO> markers;

    /**
     * On update, this is a full replace, not a merge: omitting this field (or sending {@code null})
     * clears all of the location's existing area boundary points. Send the complete desired list
     * every time, including boundary points that are meant to be kept. See {@link
     * de.felixhertweck.seatreservation.management.service.EventLocationService#updateEventLocation}.
     */
    @Valid private List<AreaBoundaryPointRequestDTO> areaBoundaryPoints;

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Integer getCapacity() {
        return capacity;
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

    public List<MakerRequestDTO> getmarkers() {
        return markers;
    }

    public void setmarkers(List<MakerRequestDTO> markers) {
        this.markers = markers;
    }

    public List<AreaBoundaryPointRequestDTO> getAreaBoundaryPoints() {
        return areaBoundaryPoints;
    }

    public void setAreaBoundaryPoints(List<AreaBoundaryPointRequestDTO> areaBoundaryPoints) {
        this.areaBoundaryPoints = areaBoundaryPoints;
    }

    public EventLocationRequestDTO() {}
}
