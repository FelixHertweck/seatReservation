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

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;

public class AdminUserUpdateDTO {
    @NoHtmlSanitize
    @Email(regexp = "^(|.+[@].+[\\\\.].+)$", message = "Invalid email format")
    private final String email;

    private final String firstname;

    private final String lastname;

    private final String password;

    private final Set<String> tags;

    private final Set<String> roles;

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

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getTags() {
        return tags;
    }

    public AdminUserUpdateDTO(
            String firstname,
            String lastname,
            String password,
            String email,
            Set<String> roles,
            Set<String> tags) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
        this.email = email;
        this.roles = roles;
        this.tags = tags;
    }
}
