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

import java.util.ArrayList;
import java.util.List;

/**
 * Transport-agnostic, fully rendered email ready to be handed to {@link EmailQueueService}.
 *
 * <p>This is a plain value object: it deliberately knows nothing about JPA or the mailer API. It
 * carries the final recipients, subject, HTML body and attachments so the queue can persist the
 * message and the dispatcher can send it later without touching the domain model again.
 *
 * <p>Instances are created through the {@link #builder()} API.
 */
public final class EmailMessage {

    private final List<String> to;
    private final List<String> cc;
    private final List<String> bcc;
    private final String subject;
    private final String htmlBody;
    private final List<EmailAttachment> attachments;

    private EmailMessage(Builder builder) {
        this.to = List.copyOf(builder.to);
        this.cc = List.copyOf(builder.cc);
        this.bcc = List.copyOf(builder.bcc);
        this.subject = builder.subject;
        this.htmlBody = builder.htmlBody;
        this.attachments = List.copyOf(builder.attachments);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getTo() {
        return to;
    }

    public List<String> getCc() {
        return cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    /** Fluent builder for {@link EmailMessage}. Null and blank addresses are ignored. */
    public static final class Builder {
        private final List<String> to = new ArrayList<>();
        private final List<String> cc = new ArrayList<>();
        private final List<String> bcc = new ArrayList<>();
        private String subject = "";
        private String htmlBody = "";
        private final List<EmailAttachment> attachments = new ArrayList<>();

        private Builder() {}

        public Builder to(String address) {
            addIfPresent(to, address);
            return this;
        }

        public Builder to(List<String> addresses) {
            if (addresses != null) {
                addresses.forEach(a -> addIfPresent(to, a));
            }
            return this;
        }

        public Builder cc(String address) {
            addIfPresent(cc, address);
            return this;
        }

        public Builder bcc(String address) {
            addIfPresent(bcc, address);
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject != null ? subject : "";
            return this;
        }

        public Builder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody != null ? htmlBody : "";
            return this;
        }

        public Builder attachment(EmailAttachment attachment) {
            if (attachment != null) {
                attachments.add(attachment);
            }
            return this;
        }

        public EmailMessage build() {
            return new EmailMessage(this);
        }

        private static void addIfPresent(List<String> target, String address) {
            if (address != null && !address.isBlank() && !target.contains(address)) {
                target.add(address);
            }
        }
    }
}
