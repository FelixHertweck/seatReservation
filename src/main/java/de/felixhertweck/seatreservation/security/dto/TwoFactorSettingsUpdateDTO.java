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

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Settings that can be changed without re-verifying a 2FA code. Enabling/disabling a factor
 * (TOTP/EMAIL) is deliberately not settable here: that must go through {@code POST /2fa/enable}
 * (requires proving possession of the factor) or {@code POST /2fa/disable} (requires proving
 * possession of a still-active factor). Otherwise a caller could turn on a factor that was never
 * provisioned, or strip protection from a hijacked session without proving anything.
 */
@RegisterForReflection
public record TwoFactorSettingsUpdateDTO(Boolean twoFactorPasskeyEnabled) {}
