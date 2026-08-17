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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Account details shared by both registration flows (password-based and passkey-based): the profile
 * is mandatory either way, only the credential itself (password vs. passkey) differs.
 */
@RegisterForReflection
@Schema(description = "Account registration details")
public abstract class RegistrationDetailsDTO {

    @NotBlank(message = "Username must not be blank")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]{3,64}$",
            message =
                    "Username must be 3-64 characters long and contain only letters, numbers, dots,"
                            + " underscores and hyphens")
    @Schema(description = "The username of the account", required = true)
    private String username;

    @NotBlank(message = "Firstname must not be blank")
    @Schema(description = "The first name of the user", required = true)
    private String firstname;

    @NotBlank(message = "Lastname must not be blank")
    @Schema(description = "The last name of the user", required = true)
    private String lastname;

    @NotBlank(message = "Email must not be blank")
    @NoHtmlSanitize
    @Email(message = "Invalid email format")
    @Schema(description = "The email address associated with the account", required = true)
    private String email;

    @NoHtmlSanitize
    @Schema(description = "ALTCHA proof-of-work verification payload", required = true)
    private String altchaPayload;

    public String getAltchaPayload() {
        return altchaPayload;
    }

    public void setAltchaPayload(String altchaPayload) {
        this.altchaPayload = altchaPayload;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (firstname != null && !firstname.isBlank()) {
            sb.append(firstname.trim());
        }
        if (lastname != null && !lastname.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(lastname.trim());
        }
        return sb.length() > 0 ? sb.toString() : username;
    }
}
