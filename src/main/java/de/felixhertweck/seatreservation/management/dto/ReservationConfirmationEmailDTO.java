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

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "DTO representing the confirmation email content for a reservation.")
public class ReservationConfirmationEmailDTO {

    @Schema(description = "The email subject", example = "Your reservation confirmation")
    private String subject;

    @Schema(description = "The rendered HTML content of the confirmation email")
    private String htmlContent;

    public ReservationConfirmationEmailDTO() {}

    public ReservationConfirmationEmailDTO(String subject, String htmlContent) {
        this.subject = subject;
        this.htmlContent = htmlContent;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
}
