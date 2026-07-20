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

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A named area scaffolded together with its event location in {@link EventLocationRequestDTO}.
 * Carries no {@code eventLocationId}: the enclosing location does not exist yet at that point. The
 * standalone CRUD counterpart is {@link AreaRequestDTO}, where the id is required.
 */
@RegisterForReflection
public class ImportAreaDto {

    @NotNull(message = "Area name must not be null")
    private String name;

    private List<@Valid CoordinateDTO> boundary;

    public ImportAreaDto() {}

    public ImportAreaDto(String name, List<CoordinateDTO> boundary) {
        this.name = name;
        this.boundary = boundary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CoordinateDTO> getBoundary() {
        return boundary;
    }

    public void setBoundary(List<CoordinateDTO> boundary) {
        this.boundary = boundary;
    }
}
