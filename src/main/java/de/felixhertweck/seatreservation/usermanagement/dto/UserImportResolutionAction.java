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

/** How an import conflict with an existing user is resolved. */
@RegisterForReflection
public enum UserImportResolutionAction {
    /** Apply the given values to the existing user, keeping its id and all its data. */
    UPDATE,
    /** Delete the existing user (and everything that belongs to it) and create it anew. */
    REPLACE
}
