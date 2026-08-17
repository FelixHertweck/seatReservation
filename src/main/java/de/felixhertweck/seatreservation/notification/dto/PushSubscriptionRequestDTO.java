/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
package de.felixhertweck.seatreservation.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubscriptionRequestDTO(
        @NotBlank(message = "Endpoint cannot be blank")
                @Size(max = 2048, message = "Endpoint must not exceed 2048 characters")
                String endpoint,
        @NotBlank(message = "P256dh key cannot be blank")
                @Size(max = 255, message = "P256dh key must not exceed 255 characters")
                String p256dh,
        @NotBlank(message = "Auth key cannot be blank")
                @Size(max = 255, message = "Auth key must not exceed 255 characters")
                String auth) {}
