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
package de.felixhertweck.seatreservation.management.dto;

import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Request body for re-sending a reservation confirmation email. All fields are optional. */
@Schema(description = "Optional parameters for re-sending a reservation confirmation email.")
public class ResendConfirmationRequestDTO {

    @Size(max = 1000)
    @Schema(
            description =
                    "Optional message shown at the top of the email, e.g. the reason for"
                            + " re-sending",
            maxLength = 1000)
    private String message;

    /** Creates an empty request, i.e. without a message. */
    public ResendConfirmationRequestDTO() {
        // Intentionally empty: required for JSON deserialization; the message stays null.
    }

    /**
     * Returns the optional message shown at the top of the email.
     *
     * @return the message, or {@code null} if none was given
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the optional message shown at the top of the email.
     *
     * @param message the message, or {@code null} for none
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
