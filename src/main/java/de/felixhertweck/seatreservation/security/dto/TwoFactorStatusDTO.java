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

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 2FA status for the current user. TOTP and email are independent, equally valid factors -- both
 * {@code totpEnabled} and {@code emailEnabled} can be true at once, and {@code twoFactorEnabled} is
 * true whenever at least one of them is.
 *
 * <p>{@code backupCodes} is only ever populated by {@code POST /2fa/enable}, and only when that
 * call freshly generated backup codes the user has not seen yet (e.g. first-time activation); it is
 * {@code null} everywhere else.
 */
@RegisterForReflection
public record TwoFactorStatusDTO(
        boolean twoFactorEnabled,
        boolean totpEnabled,
        boolean emailEnabled,
        boolean twoFactorPasskeyEnabled,
        boolean hasTotpSecret,
        long remainingBackupCodes,
        List<String> backupCodes) {}
