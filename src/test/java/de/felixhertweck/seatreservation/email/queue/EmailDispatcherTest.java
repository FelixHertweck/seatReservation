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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.EmailStatus;
import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import de.felixhertweck.seatreservation.model.entity.OutboundEmailAttachment;
import de.felixhertweck.seatreservation.model.repository.OutboundEmailRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class EmailDispatcherTest {

    @InjectMocks @Spy private EmailDispatcher emailDispatcher;

    @Mock private Mailer mailer;

    @Mock private OutboundEmailRepository outboundEmailRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject self reference for self-invocation tests
        emailDispatcher.self = emailDispatcher;
        emailDispatcher.batchSize = 20;
        emailDispatcher.retryBackoffSeconds = 60;
        emailDispatcher.maxBackoffSeconds = 3600;
        emailDispatcher.sendingTimeoutSeconds = 300;
    }

    @Test
    void testScheduledDrainCallsDrainQueue() {
        // We spy on emailDispatcher. Since scheduledDrain() calls drainQueue(),
        // we can check if it gets executed. We mock claimDueIds to return empty so it doesn't do
        // real dispatch.
        when(emailDispatcher.claimDueIds(anyInt())).thenReturn(Collections.emptyList());

        emailDispatcher.scheduledDrain();

        verify(emailDispatcher, times(1)).drainQueue();
    }

    @Test
    void testDrainQueueEmptyClaimed() {
        when(emailDispatcher.claimDueIds(20)).thenReturn(Collections.emptyList());

        int result = emailDispatcher.drainQueue();

        assertEquals(0, result);
        verify(emailDispatcher, never()).buildMail(any());
    }

    @Test
    void testDrainQueueSuccessfulDispatch() {
        UUID emailId = UUID.randomUUID();
        when(emailDispatcher.claimDueIds(20)).thenReturn(List.of(emailId));

        Mail mockMail = new Mail();
        when(emailDispatcher.buildMail(emailId)).thenReturn(mockMail);

        int result = emailDispatcher.drainQueue();

        assertEquals(1, result);
        verify(mailer, times(1)).send(mockMail);
        verify(emailDispatcher, times(1)).markSent(emailId);
    }

    @Test
    void testDrainQueueWithVanishedMessage() {
        UUID emailId = UUID.randomUUID();
        when(emailDispatcher.claimDueIds(20)).thenReturn(List.of(emailId));
        when(emailDispatcher.buildMail(emailId)).thenReturn(null);

        int result = emailDispatcher.drainQueue();

        assertEquals(0, result);
        verify(mailer, never()).send(any());
        verify(emailDispatcher, never()).markSent(any());
        verify(emailDispatcher, never()).markFailure(any(), any());
    }

    @Test
    void testDrainQueueWithMailerException() {
        UUID emailId = UUID.randomUUID();
        when(emailDispatcher.claimDueIds(20)).thenReturn(List.of(emailId));

        Mail mockMail = new Mail();
        when(emailDispatcher.buildMail(emailId)).thenReturn(mockMail);
        doThrow(new RuntimeException("SMTP Server Down")).when(mailer).send(mockMail);

        int result = emailDispatcher.drainQueue();

        assertEquals(0, result);
        verify(emailDispatcher, times(1)).markFailure(any(UUID.class), any(Exception.class));
        verify(emailDispatcher, never()).markSent(any());
    }

    @Test
    void testClaimDueIdsRequeuesStaleSending() {
        when(outboundEmailRepository.requeueStaleSending(any(Instant.class))).thenReturn(5L);
        List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(outboundEmailRepository.claimDue(any(Instant.class), anyInt()))
                .thenReturn(expectedIds);

        List<UUID> claimed = emailDispatcher.claimDueIds(20);

        assertEquals(expectedIds, claimed);
        verify(outboundEmailRepository, times(1)).requeueStaleSending(any(Instant.class));
        verify(outboundEmailRepository, times(1)).claimDue(any(Instant.class), anyInt());
    }

    @Test
    void testBuildMailWithNonExistentEmail() {
        UUID emailId = UUID.randomUUID();
        when(outboundEmailRepository.findById(emailId)).thenReturn(null);

        Mail result = emailDispatcher.buildMail(emailId);

        assertNull(result);
    }

    @Test
    void testBuildMailSuccessfullyWithoutAttachments() {
        UUID emailId = UUID.randomUUID();
        OutboundEmail email = new OutboundEmail();
        email.id = emailId;
        email.setSubject("Test Subject");
        email.setHtmlBody("<h1>Hello!</h1>");
        email.setTo(List.of("to@test.com"));
        email.setCc(List.of("cc@test.com"));
        email.setBcc(List.of("bcc@test.com"));

        when(outboundEmailRepository.findById(emailId)).thenReturn(email);

        Mail mail = emailDispatcher.buildMail(emailId);

        assertNotNull(mail);
        assertEquals("Test Subject", mail.getSubject());
        assertEquals("<h1>Hello!</h1>", mail.getHtml());
        assertEquals(List.of("to@test.com"), mail.getTo());
        assertEquals(List.of("cc@test.com"), mail.getCc());
        assertEquals(List.of("bcc@test.com"), mail.getBcc());
        assertTrue(mail.getAttachments().isEmpty());
    }

    @Test
    void testBuildMailWithAttachmentsAndInlineAttachments() {
        UUID emailId = UUID.randomUUID();
        OutboundEmail email = new OutboundEmail();
        email.id = emailId;
        email.setSubject("Test Subject");
        email.setHtmlBody("<h1>Hello!</h1>");
        email.setTo(List.of("to@test.com"));

        OutboundEmailAttachment regularAttachment =
                new OutboundEmailAttachment(
                        "doc.pdf", "application/pdf", null, "pdf-data".getBytes());
        OutboundEmailAttachment inlineAttachment =
                new OutboundEmailAttachment(
                        "image.png", "image/png", "img-cid-1", "png-data".getBytes());

        email.addAttachment(regularAttachment);
        email.addAttachment(inlineAttachment);

        when(outboundEmailRepository.findById(emailId)).thenReturn(email);

        Mail mail = emailDispatcher.buildMail(emailId);

        assertNotNull(mail);
        assertEquals(2, mail.getAttachments().size());

        io.quarkus.mailer.Attachment att1 = mail.getAttachments().get(0);
        assertEquals("doc.pdf", att1.getName());
        assertEquals("application/pdf", att1.getContentType());
        assertFalse(att1.isInlineAttachment());

        io.quarkus.mailer.Attachment att2 = mail.getAttachments().get(1);
        assertEquals("image.png", att2.getName());
        assertEquals("image/png", att2.getContentType());
        assertEquals("<img-cid-1>", att2.getContentId());
        assertTrue(att2.isInlineAttachment());
    }

    @Test
    void testMarkSent() {
        UUID emailId = UUID.randomUUID();
        OutboundEmail email = new OutboundEmail();
        email.id = emailId;
        email.setStatus(EmailStatus.PENDING);
        email.setAttempts(1);
        email.setLastError("Old Error");

        when(outboundEmailRepository.findById(emailId)).thenReturn(email);

        emailDispatcher.markSent(emailId);

        assertEquals(EmailStatus.SENT, email.getStatus());
        assertEquals(2, email.getAttempts());
        assertNull(email.getLastError());
        assertNotNull(email.getSentAt());
        assertNotNull(email.getUpdatedAt());
    }

    @Test
    void testMarkSentWithNonExistentEmail() {
        UUID emailId = UUID.randomUUID();
        when(outboundEmailRepository.findById(emailId)).thenReturn(null);

        // This should run without throwing any NullPointerException or error
        emailDispatcher.markSent(emailId);
    }

    @Test
    void testMarkFailureUnderMaxAttemptsRetries() {
        UUID emailId = UUID.randomUUID();
        OutboundEmail email = new OutboundEmail();
        email.id = emailId;
        email.setStatus(EmailStatus.SENDING);
        email.setAttempts(1);
        email.setMaxAttempts(3);

        when(outboundEmailRepository.findById(emailId)).thenReturn(email);

        Exception ex = new RuntimeException("Temporary failure");
        emailDispatcher.markFailure(emailId, ex);

        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(2, email.getAttempts());
        assertNotNull(email.getLastError());
        assertTrue(email.getLastError().contains("Temporary failure"));
        assertNotNull(email.getNextAttemptAt());
    }

    @Test
    void testMarkFailureExceedsMaxAttemptsDeadLetters() {
        UUID emailId = UUID.randomUUID();
        OutboundEmail email = new OutboundEmail();
        email.id = emailId;
        email.setStatus(EmailStatus.SENDING);
        email.setAttempts(2);
        email.setMaxAttempts(3);

        when(outboundEmailRepository.findById(emailId)).thenReturn(email);

        Exception ex = new RuntimeException("Permanent failure");
        emailDispatcher.markFailure(emailId, ex);

        assertEquals(EmailStatus.FAILED, email.getStatus());
        assertEquals(3, email.getAttempts());
        assertNotNull(email.getLastError());
        assertTrue(email.getLastError().contains("Permanent failure"));
    }

    @Test
    void testMarkFailureWithTruncatedLastError() {
        UUID emailId = UUID.randomUUID();
        OutboundEmail email = new OutboundEmail();
        email.id = emailId;
        email.setStatus(EmailStatus.SENDING);
        email.setAttempts(1);
        email.setMaxAttempts(3);

        when(outboundEmailRepository.findById(emailId)).thenReturn(email);

        // Generate a very long exception message
        StringBuilder longMsg = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            longMsg.append("x");
        }
        Exception ex = new RuntimeException(longMsg.toString());
        emailDispatcher.markFailure(emailId, ex);

        assertNotNull(email.getLastError());
        assertEquals(2048, email.getLastError().length());
    }

    @Test
    void testMarkFailureWithNonExistentEmail() {
        UUID emailId = UUID.randomUUID();
        when(outboundEmailRepository.findById(emailId)).thenReturn(null);

        // This should run without throwing any Exception
        emailDispatcher.markFailure(emailId, new RuntimeException("Error"));
    }
}
