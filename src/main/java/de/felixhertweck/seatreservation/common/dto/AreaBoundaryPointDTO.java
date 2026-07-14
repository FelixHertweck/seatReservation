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

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A single vertex of a custom area boundary polygon, expressed in the same coordinate system as
 * {@code Seat.xCoordinate}/{@code Seat.yCoordinate}.
 */
@RegisterForReflection
public record AreaBoundaryPointDTO(int xCoordinate, int yCoordinate) {}
