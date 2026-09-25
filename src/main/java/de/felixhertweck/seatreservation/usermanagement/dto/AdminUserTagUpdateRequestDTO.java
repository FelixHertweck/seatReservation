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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Request to add and/or remove tags for several users at once. Tags are only added or removed,
 * never replaced wholesale, so tags that only some of the users have stay untouched unless listed.
 */
@RegisterForReflection
public record AdminUserTagUpdateRequestDTO(
        @NotNull(message = "userIds cannot be null") @NotEmpty(message = "userIds cannot be empty")
                List<@NotNull(message = "userId cannot be null") UUID> userIds,
        @NotNull(message = "addTags cannot be null")
                Set<
                                @NotBlank(message = "tag cannot be blank")
                                @Size(max = 100, message = "tag must be at most 100 characters")
                                String>
                        addTags,
        @NotNull(message = "removeTags cannot be null")
                Set<
                                @NotBlank(message = "tag cannot be blank")
                                @Size(max = 100, message = "tag must be at most 100 characters")
                                String>
                        removeTags) {}
