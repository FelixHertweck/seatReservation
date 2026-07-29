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
package de.felixhertweck.seatreservation.security.dto;

/**
 * Shared password strength constraint, referenced by every DTO that accepts a plaintext password
 * (registration, admin/self-service user creation and update, password reset) so the minimum length
 * and its validation message stay in one place.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final String MIN_LENGTH_MESSAGE = "Password must be at least 8 characters long";

    private PasswordPolicy() {}
}
