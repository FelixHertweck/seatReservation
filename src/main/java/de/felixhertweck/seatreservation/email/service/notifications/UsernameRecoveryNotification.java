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

import io.quarkus.qute.Template;

public class UsernameRecoveryNotification extends AbstractEmailNotification {

    private final String email;
    private final List<String> usernames;
    private final Template template;

    public UsernameRecoveryNotification(
            String email, List<String> usernames, String subject, Template template) {
        super(subject);
        this.email = email;
        this.usernames = usernames;
        this.template = template;
    }

    @Override
    public List<String> recipients() {
        return email != null ? List.of(email) : List.of();
    }

    @Override
    public String renderHtml() {
        return template.data("usernames", usernames).data("currentYear", currentYear()).render();
    }
}
