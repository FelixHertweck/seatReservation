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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Request body for renaming a passkey. The label is user-facing free text, so it is intentionally
 * left to the application-wide XSS-sanitizing String deserializer (no {@code NoHtmlSanitize}).
 */
@RegisterForReflection
public class WebAuthnCredentialUpdateDTO {

    @NotBlank(message = "Label must not be blank")
    @Size(max = 64, message = "Label must be at most 64 characters long")
    private String label;

    public WebAuthnCredentialUpdateDTO() {}

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
