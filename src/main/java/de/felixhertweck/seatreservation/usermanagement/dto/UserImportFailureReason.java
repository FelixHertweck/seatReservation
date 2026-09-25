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

/** Why a user of an import request could not be created. */
@RegisterForReflection
public enum UserImportFailureReason {
    /** A user with this username (compared case-insensitively) already exists. */
    USERNAME_EXISTS,
    /** The username appears more than once in the import; only the first entry is used. */
    DUPLICATE_IN_BATCH,
    /** The username is reserved for a system account. */
    RESERVED_USERNAME,
    /** The user data is invalid. */
    INVALID
}
