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
package de.felixhertweck.seatreservation.common.dto;

import jakarta.validation.constraints.PositiveOrZero;

import de.felixhertweck.seatreservation.model.entity.Coordinate;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A 2D position shared by every seat-map element (seats, markers, area boundary points). Used both
 * as part of API responses and, with cascaded validation, as part of API requests.
 */
@RegisterForReflection
public record CoordinateDTO(
        @PositiveOrZero(message = "X coordinate must be greater than or equal to 0")
                int xCoordinate,
        @PositiveOrZero(message = "Y coordinate must be greater than or equal to 0")
                int yCoordinate) {
    public CoordinateDTO(Coordinate coordinate) {
        this(coordinate.xCoordinate(), coordinate.yCoordinate());
    }

    public Coordinate toEntity() {
        return new Coordinate(xCoordinate, yCoordinate);
    }
}
