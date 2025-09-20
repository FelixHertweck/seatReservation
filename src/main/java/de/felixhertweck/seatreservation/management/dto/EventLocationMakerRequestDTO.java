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

public class EventLocationMakerRequestDTO {
    @NotNull(message = "Label must not be null")
    private String label;

    @NotNull(message = "X coordinate must not be null")
    private Integer xCoordinate;

    @NotNull(message = "Y coordinate must not be null")
    private Integer yCoordinate;

    public EventLocationMakerRequestDTO() {}

    public EventLocationMakerRequestDTO(String label, Integer xCoordinate, Integer yCoordinate) {
        this.label = label;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getLabel() {
        return label;
    }

    public Integer getxCoordinate() {
        return xCoordinate;
    }

    public Integer getyCoordinate() {
        return yCoordinate;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setxCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public void setyCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
    }
}
