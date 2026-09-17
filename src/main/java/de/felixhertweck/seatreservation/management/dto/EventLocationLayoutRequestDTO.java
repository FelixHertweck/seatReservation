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
import java.util.Set;
import java.util.UUID;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Bulk payload to atomically synchronize the full layout (metadata, entrances, areas, markers,
 * seats, and pending deletions) of an event location in a single HTTP request and database
 * transaction.
 */
@RegisterForReflection
public class EventLocationLayoutRequestDTO {
    @Nullable private String name;
    @Nullable private String address;
    @Nullable private Set<UUID> managerIds;

    @Nullable private List<@Valid LayoutEntranceDto> entrances;
    @Nullable private List<@Valid LayoutAreaDto> areas;
    @Nullable private List<@Valid LayoutMarkerDto> markers;
    @Nullable private List<@Valid LayoutSeatDto> seats;

    @Nullable private List<UUID> deletedEntranceIds;
    @Nullable private List<UUID> deletedAreaIds;
    @Nullable private List<UUID> deletedMarkerIds;
    @Nullable private List<UUID> deletedSeatIds;

    public EventLocationLayoutRequestDTO() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Set<UUID> getManagerIds() {
        return managerIds;
    }

    public void setManagerIds(Set<UUID> managerIds) {
        this.managerIds = managerIds;
    }

    public List<LayoutEntranceDto> getEntrances() {
        return entrances;
    }

    public void setEntrances(List<LayoutEntranceDto> entrances) {
        this.entrances = entrances;
    }

    public List<LayoutAreaDto> getAreas() {
        return areas;
    }

    public void setAreas(List<LayoutAreaDto> areas) {
        this.areas = areas;
    }

    public List<LayoutMarkerDto> getMarkers() {
        return markers;
    }

    public void setMarkers(List<LayoutMarkerDto> markers) {
        this.markers = markers;
    }

    public List<LayoutSeatDto> getSeats() {
        return seats;
    }

    public void setSeats(List<LayoutSeatDto> seats) {
        this.seats = seats;
    }

    public List<UUID> getDeletedEntranceIds() {
        return deletedEntranceIds;
    }

    public void setDeletedEntranceIds(List<UUID> deletedEntranceIds) {
        this.deletedEntranceIds = deletedEntranceIds;
    }

    public List<UUID> getDeletedAreaIds() {
        return deletedAreaIds;
    }

    public void setDeletedAreaIds(List<UUID> deletedAreaIds) {
        this.deletedAreaIds = deletedAreaIds;
    }

    public List<UUID> getDeletedMarkerIds() {
        return deletedMarkerIds;
    }

    public void setDeletedMarkerIds(List<UUID> deletedMarkerIds) {
        this.deletedMarkerIds = deletedMarkerIds;
    }

    public List<UUID> getDeletedSeatIds() {
        return deletedSeatIds;
    }

    public void setDeletedSeatIds(List<UUID> deletedSeatIds) {
        this.deletedSeatIds = deletedSeatIds;
    }
}
