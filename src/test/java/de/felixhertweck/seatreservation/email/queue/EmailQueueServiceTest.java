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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.model.entity.EmailStatus;
import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import de.felixhertweck.seatreservation.model.entity.OutboundEmailAttachment;
import de.felixhertweck.seatreservation.model.repository.OutboundEmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailQueueServiceTest {

    @Mock private OutboundEmailRepository outboundEmailRepository;

    @InjectMocks private EmailQueueService emailQueueService;

    @BeforeEach
    void setUp() {
        emailQueueService.maxAttempts = 5;
    }

    @Test
    void enqueue_withNoRecipients_returnsNullAndDoesNotPersist() {
        EmailMessage message =
                EmailMessage.builder().subject("Test Subject").htmlBody("<p>Hello</p>").build();

        OutboundEmail result = emailQueueService.enqueue(message);

        assertNull(result);
        verifyNoInteractions(outboundEmailRepository);
    }

    @Test
    void enqueue_withRecipients_persistsEmailAndReturnsOutboundEmail() {
        EmailMessage message =
                EmailMessage.builder()
                        .to("to@example.com")
                        .cc("cc@example.com")
                        .bcc("bcc@example.com")
                        .subject("Test Subject")
                        .htmlBody("<p>Hello</p>")
                        .build();

        Instant before = Instant.now();
        OutboundEmail result = emailQueueService.enqueue(message);
        Instant after = Instant.now();

        assertNotNull(result);
        assertEquals(List.of("to@example.com"), result.getTo());
        assertEquals(List.of("cc@example.com"), result.getCc());
        assertEquals(List.of("bcc@example.com"), result.getBcc());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("<p>Hello</p>", result.getHtmlBody());
        assertEquals(5, result.getMaxAttempts());
        assertEquals(EmailStatus.PENDING, result.getStatus());
        assertEquals(0, result.getAttempts());

        assertNotNull(result.getCreatedAt());
        assertFalse(result.getCreatedAt().isBefore(before));
        assertFalse(result.getCreatedAt().isAfter(after));

        assertNotNull(result.getUpdatedAt());
        assertFalse(result.getUpdatedAt().isBefore(before));
        assertFalse(result.getUpdatedAt().isAfter(after));

        assertNotNull(result.getNextAttemptAt());
        assertFalse(result.getNextAttemptAt().isBefore(before));
        assertFalse(result.getNextAttemptAt().isAfter(after));

        ArgumentCaptor<OutboundEmail> captor = ArgumentCaptor.forClass(OutboundEmail.class);
        verify(outboundEmailRepository).persist(captor.capture());
        assertSame(result, captor.getValue());
    }

    @Test
    void enqueue_withAttachments_mapsAndSavesAttachments() {
        byte[] data1 = new byte[] {1, 2, 3};
        byte[] data2 = new byte[] {4, 5, 6};
        EmailAttachment attachment1 = EmailAttachment.file("file.pdf", "application/pdf", data1);
        EmailAttachment attachment2 =
                EmailAttachment.inline("image.png", "image/png", "img123", data2);

        EmailMessage message =
                EmailMessage.builder()
                        .to("recipient@example.com")
                        .subject("With Attachments")
                        .htmlBody("<p>Body</p>")
                        .attachment(attachment1)
                        .attachment(attachment2)
                        .build();

        OutboundEmail result = emailQueueService.enqueue(message);

        assertNotNull(result);
        List<OutboundEmailAttachment> attachments = result.getAttachments();
        assertEquals(2, attachments.size());

        OutboundEmailAttachment outAttr1 = attachments.get(0);
        assertEquals("file.pdf", outAttr1.getFileName());
        assertEquals("application/pdf", outAttr1.getContentType());
        assertNull(outAttr1.getContentId());
        assertArrayEquals(data1, outAttr1.getData());
        assertSame(result, outAttr1.getEmail());

        OutboundEmailAttachment outAttr2 = attachments.get(1);
        assertEquals("image.png", outAttr2.getFileName());
        assertEquals("image/png", outAttr2.getContentType());
        assertEquals("img123", outAttr2.getContentId());
        assertArrayEquals(data2, outAttr2.getData());
        assertSame(result, outAttr2.getEmail());

        verify(outboundEmailRepository).persist(result);
    }
}
