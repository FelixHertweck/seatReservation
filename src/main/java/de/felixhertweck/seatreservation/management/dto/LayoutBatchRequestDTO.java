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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Ordered list of layout operations for one event location. They are applied strictly in list order
 * inside a single transaction; the caller is responsible for a sensible order (e.g. create an area
 * before the seats using it, delete an area after its seats moved away).
 */
@RegisterForReflection
public class LayoutBatchRequestDTO {

    public static final int MAX_OPERATIONS = 10_000;

    @NotEmpty(message = "Operations must not be empty")
    @Size(max = MAX_OPERATIONS, message = "Too many operations in one batch")
    private List<@Valid LayoutOperationDTO> operations;

    public LayoutBatchRequestDTO() {}

    public LayoutBatchRequestDTO(List<LayoutOperationDTO> operations) {
        this.operations = operations;
    }

    public List<LayoutOperationDTO> getOperations() {
        return operations;
    }

    public void setOperations(List<LayoutOperationDTO> operations) {
        this.operations = operations;
    }
}
