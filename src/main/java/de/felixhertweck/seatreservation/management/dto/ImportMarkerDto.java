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
 * A labelled marker scaffolded together with its event location in {@link EventLocationRequestDTO}.
 * Carries no {@code eventLocationId}: the enclosing location does not exist yet at that point. The
 * standalone CRUD counterpart is {@link MakerRequestDTO}, where the id is required.
 */
@RegisterForReflection
public class ImportMarkerDto {

    @NotNull(message = "Label must not be null")
    private String label;

    @NotNull(message = "Coordinate must not be null")
    @Valid
    private CoordinateDTO coordinate;

    public ImportMarkerDto() {}

    public ImportMarkerDto(String label, CoordinateDTO coordinate) {
        this.label = label;
        this.coordinate = coordinate;
    }

    public ImportMarkerDto(String label, Integer xCoordinate, Integer yCoordinate) {
        this.label = label;
        this.coordinate = new CoordinateDTO(xCoordinate, yCoordinate);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public CoordinateDTO getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(CoordinateDTO coordinate) {
        this.coordinate = coordinate;
    }
}
