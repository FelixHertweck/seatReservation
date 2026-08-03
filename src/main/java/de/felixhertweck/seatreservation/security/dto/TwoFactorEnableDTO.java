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

import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * {@code code} is required for TOTP (the app-generated code), but not for EMAIL when the account's
 * email address is already verified -- possession of that address was already proven during account
 * email verification, so no separate 2FA setup code is needed.
 */
@RegisterForReflection
public record TwoFactorEnableDTO(@NotNull TwoFactorMethod method, String code) {}
