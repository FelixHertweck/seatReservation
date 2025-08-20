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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;

public class UserCreationDTO {
    @NotNull(message = "Username cannot be null")
    private final String username;

    @NotNull(message = "Email cannot be null")
    @NoHtmlSanitize
    private final String email;

    @NotNull(message = "Password cannot be null")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private final String password;

    @NotNull(message = "Firstname cannot be null")
    private final String firstname;

    @NotNull(message = "Lastname cannot be null")
    private final String lastname;

    private final Set<String> tags;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Set<String> getTags() {
        return tags;
    }

    public UserCreationDTO(
            String username,
            String email,
            String password,
            String firstname,
            String lastname,
            Set<String> tags) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.tags = tags;
    }

    public UserCreationDTO(AdminUserCreationDto adminUserCreationDto) {
        this.username = adminUserCreationDto.getUsername();
        this.email = adminUserCreationDto.getEmail();
        this.password = adminUserCreationDto.getPassword();
        this.firstname = adminUserCreationDto.getFirstname();
        this.lastname = adminUserCreationDto.getLastname();
        this.tags = adminUserCreationDto.getTags();
    }

    public UserCreationDTO(RegisterRequestDTO registerRequest) {
        this(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getFirstname(),
                registerRequest.getLastname(),
                Set.of());
    }
}
