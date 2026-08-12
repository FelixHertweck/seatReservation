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

import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.qute.Template;

public class PasswordResetNotification extends AbstractUserNotification {

    private final PasswordResetToken token;
    private final String frontendBaseUrl;
    private final Template template;

    public PasswordResetNotification(
            User user,
            PasswordResetToken token,
            String frontendBaseUrl,
            String subject,
            Template template) {
        super(user, subject);
        this.token = token;
        this.frontendBaseUrl = frontendBaseUrl;
        this.template = template;
    }

    @Override
    public String renderHtml() {
        String resetLink =
                (frontendBaseUrl != null ? frontendBaseUrl.trim() : "")
                        + "/reset-password?token="
                        + (token != null ? token.getToken() : "");
        String expTime =
                token != null && token.getExpirationTime() != null
                        ? formatDateTime(token.getExpirationTime())
                        : "";
        return template.data("fullName", fullName(user))
                .data("resetLink", resetLink)
                .data("expirationTime", expTime)
                .data("currentYear", currentYear())
                .render();
    }
}
