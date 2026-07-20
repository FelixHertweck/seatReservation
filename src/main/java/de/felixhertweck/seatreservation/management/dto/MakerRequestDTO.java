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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Request body for the standalone {@code MarkerResource} CRUD endpoints, also used as the embedded
 * scaffold in {@link EventLocationRequestDTO} on create. {@code eventLocationId} is required for
 * the standalone resource and validated there; it stays null for the embedded create-time scaffold,
 * where the enclosing location does not exist yet.
 */
@RegisterForReflection
public class MakerRequestDTO {
    private Long eventLocationId;

    @NotNull(message = "Label must not be null")
    private String label;

    @NotNull(message = "Coordinate must not be null")
    @Valid
    private CoordinateDTO coordinate;

    public MakerRequestDTO() {}

    public MakerRequestDTO(String label, Integer xCoordinate, Integer yCoordinate) {
        this.label = label;
        this.coordinate = new CoordinateDTO(xCoordinate, yCoordinate);
    }

    public MakerRequestDTO(Long eventLocationId, String label, CoordinateDTO coordinate) {
        this.eventLocationId = eventLocationId;
        this.label = label;
        this.coordinate = coordinate;
    }

    public Long getEventLocationId() {
        return eventLocationId;
    }

    public void setEventLocationId(Long eventLocationId) {
        this.eventLocationId = eventLocationId;
    }

    public String getLabel() {
        return label;
    }

    public CoordinateDTO getCoordinate() {
        return coordinate;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setCoordinate(CoordinateDTO coordinate) {
        this.coordinate = coordinate;
    }
}
