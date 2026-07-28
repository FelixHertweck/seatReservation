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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailMessageTest {

    @Test
    void builder_withValidFields_createsEmailMessage() {
        EmailAttachment attachment1 =
                EmailAttachment.file("test.pdf", "application/pdf", new byte[] {1, 2, 3});
        EmailAttachment attachment2 =
                EmailAttachment.inline("logo.png", "image/png", "logo-id", new byte[] {4, 5});

        EmailMessage message =
                EmailMessage.builder()
                        .to("to@example.com")
                        .cc("cc@example.com")
                        .bcc("bcc@example.com")
                        .subject("Test Subject")
                        .htmlBody("<p>Test Body</p>")
                        .attachment(attachment1)
                        .attachment(attachment2)
                        .build();

        assertEquals(List.of("to@example.com"), message.getTo());
        assertEquals(List.of("cc@example.com"), message.getCc());
        assertEquals(List.of("bcc@example.com"), message.getBcc());
        assertEquals("Test Subject", message.getSubject());
        assertEquals("<p>Test Body</p>", message.getHtmlBody());
        assertEquals(List.of(attachment1, attachment2), message.getAttachments());
    }

    @Test
    void builder_withMultipleToAddresses_addsAllValidDistinctAddresses() {
        EmailMessage message =
                EmailMessage.builder()
                        .to("to1@example.com")
                        .to(
                                List.of(
                                        "to2@example.com",
                                        "to1@example.com",
                                        "",
                                        "  ",
                                        "to3@example.com"))
                        .to((String) null)
                        .build();

        assertEquals(
                List.of("to1@example.com", "to2@example.com", "to3@example.com"), message.getTo());
    }

    @Test
    void builder_withNullOrBlankAddresses_ignoresThem() {
        EmailMessage message =
                EmailMessage.builder()
                        .to((String) null)
                        .to("")
                        .to("   ")
                        .cc((String) null)
                        .cc("")
                        .cc("   ")
                        .bcc((String) null)
                        .bcc("")
                        .bcc("   ")
                        .build();

        assertTrue(message.getTo().isEmpty());
        assertTrue(message.getCc().isEmpty());
        assertTrue(message.getBcc().isEmpty());
    }

    @Test
    void builder_withNullSubjectAndHtmlBody_setsEmptyStrings() {
        EmailMessage message = EmailMessage.builder().subject(null).htmlBody(null).build();

        assertEquals("", message.getSubject());
        assertEquals("", message.getHtmlBody());
    }

    @Test
    void builder_withNullAttachment_ignoresIt() {
        EmailMessage message = EmailMessage.builder().attachment(null).build();

        assertTrue(message.getAttachments().isEmpty());
    }

    @Test
    void emailMessage_isImmutable() {
        EmailMessage message = EmailMessage.builder().to("to@example.com").build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> message.getTo().add("another@example.com"));
        assertThrows(
                UnsupportedOperationException.class, () -> message.getCc().add("cc@example.com"));
        assertThrows(
                UnsupportedOperationException.class, () -> message.getBcc().add("bcc@example.com"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> message.getAttachments().add(EmailAttachment.file("x", "y", new byte[0])));
    }
}
