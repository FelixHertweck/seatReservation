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
package de.felixhertweck.seatreservation.security.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Account details for creating a brand-new account via a passkey. The passkey is the only
 * credential created here; a password can be added later from the account's profile settings.
 */
@RegisterForReflection
public class WebAuthnRegistrationStartDTO extends RegistrationDetailsDTO {

    @Override
    public String toString() {
        return "WebAuthnRegistrationStartDTO{"
                + "username='"
                + getUsername()
                + '\''
                + ", firstname='"
                + getFirstname()
                + '\''
                + ", lastname='"
                + getLastname()
                + '\''
                + ", email='"
                + getEmail()
                + '\''
                + '}';
    }
}
