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
import jakarta.validation.constraints.Size;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Account details for creating a brand-new account via a passkey. All fields except the username
 * are optional: a passkey-only account can be created without a password, and the profile fields
 * may be filled in later.
 */
@RegisterForReflection
public class WebAuthnRegistrationStartDTO {

    @NotBlank(message = "Username must not be blank")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]{3,64}$",
            message =
                    "Username must be 3-64 characters long and contain only letters, numbers, dots,"
                            + " underscores and hyphens")
    private String username;

    private String firstname;

    private String lastname;

    @NoHtmlSanitize
    @Email(message = "Invalid email format")
    private String email;

    /** Optional password. When omitted, the account can only be accessed via passkeys. */
    @NoHtmlSanitize
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    @Override
    public String toString() {
        return "WebAuthnRegistrationStartDTO{"
                + "username='"
                + username
                + '\''
                + ", firstname='"
                + firstname
                + '\''
                + ", lastname='"
                + lastname
                + '\''
                + ", email='"
                + email
                + '\''
                + ", passwordSet="
                + (password != null && !password.isEmpty())
                + '}';
    }
}
