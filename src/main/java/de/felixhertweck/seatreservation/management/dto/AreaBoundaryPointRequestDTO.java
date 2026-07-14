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
 * A single vertex of a custom area boundary polygon, submitted as part of an {@link
 * EventLocationRequestDTO} or {@link ImportEventLocationDto}. Points sharing the same {@code area}
 * name are connected in the order they appear in the submitted list to form that area's polygon.
 */
@RegisterForReflection
public class AreaBoundaryPointRequestDTO {
    @NotNull(message = "Area must not be null")
    private String area;

    @NotNull(message = "X coordinate must not be null")
    private Integer xCoordinate;

    @NotNull(message = "Y coordinate must not be null")
    private Integer yCoordinate;

    public AreaBoundaryPointRequestDTO() {}

    public AreaBoundaryPointRequestDTO(String area, Integer xCoordinate, Integer yCoordinate) {
        this.area = area;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Integer getxCoordinate() {
        return xCoordinate;
    }

    public void setxCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public Integer getyCoordinate() {
        return yCoordinate;
    }

    public void setyCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
    }
}
