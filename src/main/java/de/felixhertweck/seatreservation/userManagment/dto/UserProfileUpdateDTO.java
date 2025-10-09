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
package de.felixhertweck.seatreservation.userManagment.dto;

import java.util.Set;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UserProfileUpdateDTO {
    @NoHtmlSanitize
    @NotNull(message = "email cannot be null")
    @Email(regexp = "^(|.+[@].+[\\\\.].+)$", message = "Invalid email format")
    private final String email;

    @NotNull(message = "firstname cannot be null")
    private final String firstname;

    @NotNull(message = "lastname cannot be null")
    private final String lastname;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    @NoHtmlSanitize
    private final String password;

    @NotNull(message = "tags cannot be null")
    private final Set<String> tags;

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getTags() {
        return tags;
    }

    public UserProfileUpdateDTO(
            String firstname, String lastname, String password, String email, Set<String> tags) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
        this.email = email;
        this.tags = tags;
    }
}
