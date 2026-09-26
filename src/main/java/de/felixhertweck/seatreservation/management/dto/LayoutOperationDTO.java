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

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * One step of a {@link LayoutBatchRequestDTO}. Exactly the payload field matching {@code entity} is
 * used; it is the regular request DTO of that entity, so no second set of field definitions has to
 * be maintained.
 *
 * <ul>
 *   <li>{@code CREATE}: payload required, {@code ref} optional. Later operations of the same batch
 *       can point at the created entity via that ref.
 *   <li>{@code UPDATE}: {@code id} and payload required ({@code LOCATION} needs no {@code id}).
 *   <li>{@code DELETE}: {@code id} required.
 * </ul>
 */
@RegisterForReflection
public class LayoutOperationDTO {

    @NotNull(message = "Operation entity must not be null")
    private LayoutEntityType entity;

    @NotNull(message = "Operation action must not be null")
    private LayoutOperationAction action;

    /** Target of an UPDATE or DELETE. */
    private UUID id;

    /** Client-chosen key of a CREATE, unique per entity type within one batch. */
    private String ref;

    @Valid private EventLocationUpdateDTO location;
    @Valid private EntranceRequestDTO entrance;
    @Valid private AreaRequestDTO area;
    @Valid private MakerRequestDTO marker;
    @Valid private SeatRequestDTO seat;

    /** Seat only: ref of an area created earlier in the batch; replaces {@code seat.areaId}. */
    private String areaRef;

    /**
     * Seat only: ref of an entrance created earlier in the batch; replaces {@code seat.entranceId}.
     */
    private String entranceRef;

    public LayoutOperationDTO() {}

    public LayoutEntityType getEntity() {
        return entity;
    }

    public void setEntity(LayoutEntityType entity) {
        this.entity = entity;
    }

    public LayoutOperationAction getAction() {
        return action;
    }

    public void setAction(LayoutOperationAction action) {
        this.action = action;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public EventLocationUpdateDTO getLocation() {
        return location;
    }

    public void setLocation(EventLocationUpdateDTO location) {
        this.location = location;
    }

    public EntranceRequestDTO getEntrance() {
        return entrance;
    }

    public void setEntrance(EntranceRequestDTO entrance) {
        this.entrance = entrance;
    }

    public AreaRequestDTO getArea() {
        return area;
    }

    public void setArea(AreaRequestDTO area) {
        this.area = area;
    }

    public MakerRequestDTO getMarker() {
        return marker;
    }

    public void setMarker(MakerRequestDTO marker) {
        this.marker = marker;
    }

    public SeatRequestDTO getSeat() {
        return seat;
    }

    public void setSeat(SeatRequestDTO seat) {
        this.seat = seat;
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
