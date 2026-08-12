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
package de.felixhertweck.seatreservation.email.service.notifications;

import java.util.List;

import de.felixhertweck.seatreservation.email.queue.EmailAttachment;
import de.felixhertweck.seatreservation.model.entity.EmailPriority;
import de.felixhertweck.seatreservation.model.entity.User;

public class EventReservationsCsvNotification extends AbstractUserNotification {

    private final String htmlContent;
    private final EmailAttachment csvAttachment;

    public EventReservationsCsvNotification(
            User manager, String subject, String htmlContent, EmailAttachment csvAttachment) {
        super(manager, subject);
        this.htmlContent = htmlContent;
        this.csvAttachment = csvAttachment;
    }

    @Override
    public String renderHtml() {
        return htmlContent;
    }

    @Override
    public List<EmailAttachment> attachments() {
        return csvAttachment != null ? List.of(csvAttachment) : List.of();
    }

    @Override
    public EmailPriority priority() {
        return EmailPriority.BULK;
    }
}
