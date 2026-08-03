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
 * Challenge issued when a login needs a second factor. {@code totpAvailable}/{@code emailAvailable}
 * tell the client which methods it may offer the user -- both can be true, in which case the
 * challenge already has an email code sent and waiting, but a TOTP code is accepted just as well.
 */
@RegisterForReflection
public record TwoFactorRequiredDTO(
        boolean twoFactorRequired,
        String challengeToken,
        boolean totpAvailable,
        boolean emailAvailable) {}
