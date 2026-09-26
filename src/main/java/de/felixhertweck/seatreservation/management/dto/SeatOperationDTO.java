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

/** {@link LayoutOperationDTO} on seats. {@code data} is absent for DELETE. */
@RegisterForReflection
public class SeatOperationDTO extends LayoutOperationDTO {

    @Valid private SeatRequestDTO data;

    /** Ref of an area created earlier in the same batch; replaces {@code data.areaId}. */
    private String areaRef;

    /** Ref of an entrance created earlier in the same batch; replaces {@code data.entranceId}. */
    private String entranceRef;

    public SeatOperationDTO() {}

    @Override
    public LayoutEntityType getEntity() {
        return LayoutEntityType.SEAT;
    }

    public SeatRequestDTO getData() {
        return data;
    }

    public void setData(SeatRequestDTO data) {
        this.data = data;
    }

    public String getAreaRef() {
        return areaRef;
    }

    public void setAreaRef(String areaRef) {
        this.areaRef = areaRef;
    }

    public String getEntranceRef() {
        return entranceRef;
    }

    public void setEntranceRef(String entranceRef) {
        this.entranceRef = entranceRef;
    }
}
