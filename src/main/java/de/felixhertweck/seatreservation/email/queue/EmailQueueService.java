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
package de.felixhertweck.seatreservation.email.queue;

import java.time.Instant;
import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import de.felixhertweck.seatreservation.model.entity.OutboundEmailAttachment;
import de.felixhertweck.seatreservation.model.repository.OutboundEmailRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Entry point for the transactional email outbox.
 *
 * <p>Callers hand over a fully rendered {@link EmailMessage}; this service persists it as a {@link
 * OutboundEmail} in the <em>current</em> transaction (transaction type {@code REQUIRED}). That
 * means the mail is committed atomically with the business change that triggered it: if the
 * surrounding transaction rolls back, no orphan mail is queued, and once it commits the mail is
 * guaranteed to be picked up by the {@link EmailDispatcher}.
 */
@ApplicationScoped
public class EmailQueueService {

    private static final Logger LOG = Logger.getLogger(EmailQueueService.class);

    @Inject OutboundEmailRepository outboundEmailRepository;

    @ConfigProperty(name = "email.queue.max-attempts", defaultValue = "5")
    int maxAttempts;

    /**
     * Queues a rendered message for asynchronous delivery.
     *
     * <p>Messages without any resolvable recipient are dropped (and logged), mirroring the previous
     * "skip empty address" behaviour, so callers do not need to guard against it.
     *
     * @param message the fully rendered message to send
     * @return the persisted outbox entry, or {@code null} if the message had no recipients
     */
    @Transactional
    public OutboundEmail enqueue(EmailMessage message) {
        if (message.getTo().isEmpty() && message.getCc().isEmpty() && message.getBcc().isEmpty()) {
            LOG.warnf(
                    "Dropping email with subject '%s': no recipients after filtering.",
                    message.getSubject());
            return null;
        }

        Instant now = Instant.now();
        OutboundEmail email = new OutboundEmail();
        email.setTo(new ArrayList<>(message.getTo()));
        email.setCc(new ArrayList<>(message.getCc()));
        email.setBcc(new ArrayList<>(message.getBcc()));
        email.setSubject(message.getSubject());
        email.setHtmlBody(message.getHtmlBody());
        email.setMaxAttempts(maxAttempts);
        email.setNextAttemptAt(now);
        email.setCreatedAt(now);
        email.setUpdatedAt(now);

        for (EmailAttachment attachment : message.getAttachments()) {
            email.addAttachment(
                    new OutboundEmailAttachment(
                            attachment.fileName(),
                            attachment.contentType(),
                            attachment.contentId(),
                            attachment.data()));
        }

        outboundEmailRepository.persist(email);
        LOG.debugf(
                "Queued email id=%d subject='%s' recipients=%d attachments=%d",
                email.id,
                message.getSubject(),
                message.getTo().size() + message.getCc().size() + message.getBcc().size(),
                message.getAttachments().size());
        return email;
    }
}
