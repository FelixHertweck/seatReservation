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
package de.felixhertweck.seatreservation.email.service;

import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.email.queue.EmailAttachment;
import de.felixhertweck.seatreservation.email.queue.EmailMessage;
import de.felixhertweck.seatreservation.email.queue.EmailQueueService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailSender {

    private static final Logger LOG = Logger.getLogger(EmailSender.class);

    @Inject EmailQueueService emailQueueService;

    @ConfigProperty(name = "email.bcc-address")
    Optional<String> bccAddress;

    public boolean send(EmailNotification notification) {
        if (notification == null) {
            return false;
        }

        List<String> rawRecipients = notification.recipients();
        if (rawRecipients == null || rawRecipients.isEmpty()) {
            return false;
        }

        List<String> validRecipients =
                rawRecipients.stream()
                        .filter(EmailSender::isValidAddress)
                        .map(String::trim)
                        .distinct()
                        .toList();

        if (validRecipients.isEmpty()) {
            return false;
        }

        String htmlContent = notification.renderHtml();
        String subject = notification.subject();

        EmailMessage.Builder builder =
                EmailMessage.builder()
                        .subject(subject)
                        .htmlBody(htmlContent)
                        .priority(notification.priority());

        builder.to(validRecipients.getFirst());
        if (validRecipients.size() > 1) {
            validRecipients.subList(1, validRecipients.size()).forEach(builder::cc);
        }

        if (notification.includeBcc()) {
            bccAddress.ifPresent(
                    address -> {
                        if (!address.trim().isEmpty() && !validRecipients.contains(address)) {
                            builder.bcc(address);
                        }
                    });
        }

        List<EmailAttachment> attachments = notification.attachments();
        if (attachments != null) {
            attachments.forEach(builder::attachment);
        }

        emailQueueService.enqueue(builder.build());
        return true;
    }

    public static boolean isValidAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        if (address.trim().toLowerCase().endsWith("@localhost")) {
            LOG.debugf("Skipping email sending for localhost address: %s", address);
            return false;
        }
        return true;
    }
}
