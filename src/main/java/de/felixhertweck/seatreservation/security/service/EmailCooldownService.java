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
package de.felixhertweck.seatreservation.security.service;

import java.time.Instant;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.repository.EmailCooldownRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EmailCooldownService {

    public enum Purpose {
        TWO_FACTOR_RESEND,
        EMAIL_CONFIRMATION_RESEND,
        PASSWORD_RESET,
        USERNAME_RECOVERY
    }

    @Inject EmailCooldownRepository repository;

    @ConfigProperty(name = "email.resend-cooldown-seconds", defaultValue = "60")
    int cooldownSeconds;

    /**
     * Returns the retry-after instant if still cooling down (and does NOT record a new send).
     * Returns empty and records "sent now" if the caller is clear to send.
     *
     * @param purpose the email cooldown purpose
     * @param key the identifier key (e.g. user ID, username, or normalized email)
     * @return Optional containing the retryAfter instant if cooling down, empty otherwise
     */
    public Optional<Instant> checkAndRecord(Purpose purpose, String key) {
        Instant now = Instant.now();
        Instant last = repository.findLastSentAt(purpose.name(), key);
        if (last != null) {
            Instant retryAfter = last.plusSeconds(cooldownSeconds);
            if (retryAfter.isAfter(now)) {
                return Optional.of(retryAfter);
            }
        }
        repository.recordSent(purpose.name(), key, now);
        return Optional.empty();
    }
}
