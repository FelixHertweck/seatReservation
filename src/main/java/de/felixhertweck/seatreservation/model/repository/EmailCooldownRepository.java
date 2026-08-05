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
package de.felixhertweck.seatreservation.model.repository;

import java.time.Instant;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailCooldown;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class EmailCooldownRepository implements PanacheRepositoryBase<EmailCooldown, UUID> {

    public Instant findLastSentAt(String purpose, String key) {
        return find("purpose = ?1 and cooldownKey = ?2", purpose, key)
                .firstResultOptional()
                .map(EmailCooldown::getLastSentAt)
                .orElse(null);
    }

    @Transactional
    public void recordSent(String purpose, String key, Instant when) {
        EmailCooldown existing =
                find("purpose = ?1 and cooldownKey = ?2", purpose, key).firstResult();
        if (existing != null) {
            existing.setLastSentAt(when);
        } else {
            persist(new EmailCooldown(purpose, key, when));
        }
    }

    @Transactional
    public long deleteOldEntries(Instant before) {
        return delete("lastSentAt < ?1", before);
    }
}
