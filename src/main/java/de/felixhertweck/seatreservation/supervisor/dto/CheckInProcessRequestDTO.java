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
package de.felixhertweck.seatreservation.supervisor.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CheckInProcessRequestDTO {
    @NotNull(message = "userId must not be null")
    public Long userId;

    @NotNull(message = "eventId must not be null")
    public Long eventId;

    public List<Long> checkIn;

    public List<Long> cancel;

    public CheckInProcessRequestDTO(
            Long eventId, Long userId, List<Long> checkIn, List<Long> cancel) {
        this.eventId = eventId;
        this.userId = userId;
        this.checkIn = checkIn;
        this.cancel = cancel;
    }
}
