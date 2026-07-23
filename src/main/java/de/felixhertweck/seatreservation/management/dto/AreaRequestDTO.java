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
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Request body for the standalone {@code AreaResource} CRUD endpoints: an {@link ImportAreaDto}
 * plus the id of the location the area belongs to.
 *
 * <p>The split exists so that {@code eventLocationId} can be declared required here while staying
 * absent from the create-time scaffold, where the enclosing location does not exist yet. Area
 * fields themselves live on the superclass and are inherited, so adding one is a single edit.
 */
@RegisterForReflection
public class AreaRequestDTO extends ImportAreaDto {

    @NotNull(message = "EventLocation ID must not be null")
    private UUID eventLocationId;

    public AreaRequestDTO() {}

    public AreaRequestDTO(UUID eventLocationId, String name, List<CoordinateDTO> boundary) {
        super(name, boundary);
        this.eventLocationId = eventLocationId;
    }

    public UUID getEventLocationId() {
        return eventLocationId;
    }

    public void setEventLocationId(UUID eventLocationId) {
        this.eventLocationId = eventLocationId;
    }
}
