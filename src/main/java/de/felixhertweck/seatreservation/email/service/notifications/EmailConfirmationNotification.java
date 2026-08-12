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

import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.qute.Template;

public class EmailConfirmationNotification extends AbstractUserNotification {

    private final EmailVerification emailVerification;
    private final String verificationLink;
    private final Template template;

    public EmailConfirmationNotification(
            User user,
            EmailVerification emailVerification,
            String verificationLink,
            String subject,
            Template template) {
        super(user, subject);
        this.emailVerification = emailVerification;
        this.verificationLink = verificationLink;
        this.template = template;
    }

    @Override
    public String renderHtml() {
        String token = emailVerification != null ? emailVerification.getToken() : "";
        String expTime =
                emailVerification != null && emailVerification.getExpirationTime() != null
                        ? formatDateTime(emailVerification.getExpirationTime())
                        : "";
        return template.data("fullName", fullName(user))
                .data("verificationCode", token)
                .data("verificationLink", verificationLink)
                .data("expirationTime", expTime)
                .data("currentYear", currentYear())
                .render();
    }
}
