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

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.felixhertweck.seatreservation.email.service.EmailNotification;
import de.felixhertweck.seatreservation.model.entity.User;

public abstract class AbstractEmailNotification implements EmailNotification {

    protected final String subject;

    protected AbstractEmailNotification(String subject) {
        this.subject = subject;
    }

    @Override
    public String subject() {
        return subject;
    }

    protected static String fullName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getFirstname() != null && user.getLastname() != null) {
            return user.getFirstname() + " " + user.getLastname();
        }
        return user.getUsername() != null ? user.getUsername() : "";
    }

    protected static String formatDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    protected static String currentYear() {
        return String.valueOf(Year.now(ZoneId.systemDefault()).getValue());
    }
}
