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
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class LayoutSeatDto {
    @Nullable private UUID id;
    @Nullable private String tempId;

    @NotBlank(message = "Seat number must not be blank")
    private String seatNumber;

    @NotBlank(message = "Seat row must not be blank")
    private String seatRow;

    @NotNull(message = "Coordinate must not be null")
    @Valid
    private CoordinateDTO coordinate;

    @Nullable private UUID entranceId;
    @Nullable private String entranceTempId;

    @Nullable private UUID areaId;
    @Nullable private String areaTempId;

    public LayoutSeatDto() {}

    public LayoutSeatDto(
            UUID id,
            String tempId,
            String seatNumber,
            String seatRow,
            CoordinateDTO coordinate,
            UUID entranceId,
            String entranceTempId,
            UUID areaId,
            String areaTempId) {
        this.id = id;
        this.tempId = tempId;
        this.seatNumber = seatNumber;
        this.seatRow = seatRow;
        this.coordinate = coordinate;
        this.entranceId = entranceId;
        this.entranceTempId = entranceTempId;
        this.areaId = areaId;
        this.areaTempId = areaTempId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTempId() {
        return tempId;
    }

    public void setTempId(String tempId) {
        this.tempId = tempId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public CoordinateDTO getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(CoordinateDTO coordinate) {
        this.coordinate = coordinate;
    }

    public UUID getEntranceId() {
        return entranceId;
    }

    public void setEntranceId(UUID entranceId) {
        this.entranceId = entranceId;
    }

    public String getEntranceTempId() {
        return entranceTempId;
    }

    public void setEntranceTempId(String entranceTempId) {
        this.entranceTempId = entranceTempId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public void setAreaId(UUID areaId) {
        this.areaId = areaId;
    }

    public String getAreaTempId() {
        return areaTempId;
    }

    public void setAreaTempId(String areaTempId) {
        this.areaTempId = areaTempId;
    }
}
