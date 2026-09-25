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

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A user of an import request that was not created.
 *
 * @param username the username as given in the import
 * @param reason the machine-readable reason
 * @param message a human-readable explanation
 */
@RegisterForReflection
public record UserImportFailureDTO(
        String username, UserImportFailureReason reason, String message) {}
