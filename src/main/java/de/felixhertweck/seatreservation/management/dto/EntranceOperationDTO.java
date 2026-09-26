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

import io.quarkus.runtime.annotations.RegisterForReflection;

/** {@link LayoutOperationDTO} on entrances. {@code data} is absent for DELETE. */
@RegisterForReflection
public class EntranceOperationDTO extends LayoutOperationDTO {

    @Valid private EntranceRequestDTO data;

    public EntranceOperationDTO() {}

    @Override
    public LayoutEntityType getEntity() {
        return LayoutEntityType.ENTRANCE;
    }

    public EntranceRequestDTO getData() {
        return data;
    }

    public void setData(EntranceRequestDTO data) {
        this.data = data;
    }
}
