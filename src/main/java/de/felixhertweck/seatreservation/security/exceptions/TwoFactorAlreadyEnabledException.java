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
package de.felixhertweck.seatreservation.security.exceptions;

/**
 * Thrown when TOTP setup is requested but TOTP is already enabled for the account. Re-enrollment
 * while already enabled would let a hijacked session silently replace the provisioned secret
 * without ever proving possession of the existing one, so the caller must disable TOTP (which does
 * require that proof) before setting it up again.
 */
public class TwoFactorAlreadyEnabledException extends RuntimeException {

    public TwoFactorAlreadyEnabledException(String message) {
        super(message);
    }
}
