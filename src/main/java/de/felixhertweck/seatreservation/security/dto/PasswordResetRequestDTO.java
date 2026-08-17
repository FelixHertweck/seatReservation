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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(description = "Request DTO for initiating a password reset")
public class PasswordResetRequestDTO {

    @NotBlank(message = "Username must not be blank")
    @NoHtmlSanitize
    @Schema(description = "The username of the account", required = true)
    private String username;

    @NotBlank(message = "Email must not be blank")
    @NoHtmlSanitize
    @Email(message = "Invalid email format")
    @Schema(description = "The email address associated with the account", required = true)
    private String email;

    @NoHtmlSanitize
    @Schema(description = "ALTCHA proof-of-work verification payload", required = true)
    private String altchaPayload;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAltchaPayload() {
        return altchaPayload;
    }

    public void setAltchaPayload(String altchaPayload) {
        this.altchaPayload = altchaPayload;
    }
}
