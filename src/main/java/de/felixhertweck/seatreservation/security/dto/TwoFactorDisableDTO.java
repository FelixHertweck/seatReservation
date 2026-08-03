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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Disables a single 2FA factor ({@code method}: TOTP or EMAIL), proven by a current TOTP/email code
 * or an unused backup code -- without this, a hijacked session alone would be enough to strip 2FA
 * from an account. If the other factor is still active afterwards, 2FA as a whole stays enabled;
 * only when both are off does {@code twoFactorEnabled} become false.
 */
@RegisterForReflection
public record TwoFactorDisableDTO(@NotNull TwoFactorMethod method, @NotBlank String code) {}
