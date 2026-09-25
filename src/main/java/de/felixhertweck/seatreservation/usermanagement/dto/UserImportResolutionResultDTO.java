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
package de.felixhertweck.seatreservation.usermanagement.dto;

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Outcome of one conflict resolution.
 *
 * @param existingUserId the id of the existing user the resolution was requested for
 * @param username the username of the resulting user, if known
 * @param success whether the resolution was applied
 * @param message the reason if it was not applied
 * @param userId the id of the resulting user (differs from {@code existingUserId} after a
 *     replacement)
 */
@RegisterForReflection
public record UserImportResolutionResultDTO(
        UUID existingUserId, String username, boolean success, String message, UUID userId) {}
