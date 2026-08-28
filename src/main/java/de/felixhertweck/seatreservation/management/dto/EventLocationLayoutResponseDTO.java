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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import io.quarkus.runtime.annotations.RegisterForReflection;

/** Response returned after successfully synchronizing an event location layout. */
@RegisterForReflection
public record EventLocationLayoutResponseDTO(
        UUID id,
        String name,
        String address,
        Set<UUID> managerIds,
        Map<String, UUID> createdEntranceIdMap,
        Map<String, UUID> createdAreaIdMap,
        Map<String, UUID> createdMarkerIdMap,
        Map<String, UUID> createdSeatIdMap,
        List<SeatDTO> seats,
        List<EventLocationMakerDTO> markers,
        List<AreaResponseDTO> areas,
        List<EntranceResponseDTO> entrances) {}
