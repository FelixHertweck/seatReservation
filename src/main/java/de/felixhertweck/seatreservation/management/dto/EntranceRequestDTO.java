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

import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Request body for the standalone {@code EntranceResource} CRUD endpoints. {@code eventLocationId}
 * is always required.
 */
@RegisterForReflection
public class EntranceRequestDTO {
    @NotNull(message = "EventLocation ID must not be null")
    private Long eventLocationId;

    @NotNull(message = "Entrance name must not be null")
    private String name;

    public EntranceRequestDTO() {}

    public EntranceRequestDTO(Long eventLocationId, String name) {
        this.eventLocationId = eventLocationId;
        this.name = name;
    }

    public Long getEventLocationId() {
        return eventLocationId;
    }

    public void setEventLocationId(Long eventLocationId) {
        this.eventLocationId = eventLocationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
