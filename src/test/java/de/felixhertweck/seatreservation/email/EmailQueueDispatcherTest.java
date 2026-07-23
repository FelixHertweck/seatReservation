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
package de.felixhertweck.seatreservation.email;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.felixhertweck.seatreservation.email.queue.EmailDispatcher;
import de.felixhertweck.seatreservation.email.queue.EmailMessage;
import de.felixhertweck.seatreservation.email.queue.EmailQueueService;
import de.felixhertweck.seatreservation.model.entity.EmailStatus;
import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import de.felixhertweck.seatreservation.model.repository.OutboundEmailRepository;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmailQueueDispatcherTest {

    private static final String RECIPIENT = "recipient@example.com";

    @Inject EmailQueueService emailQueueService;

    @Inject EmailDispatcher emailDispatcher;

    @Inject OutboundEmailRepository outboundEmailRepository;

    @Inject MockMailbox mailbox;

    @ConfigProperty(name = "email.queue.max-attempts", defaultValue = "5")
    int maxAttempts;

    @BeforeEach
    void setUp() {
        mailbox.clear();
        QuarkusTransaction.requiringNew()
                .run(
                        () ->
                                outboundEmailRepository
                                        .listAll()
                                        .forEach(outboundEmailRepository::delete));
    }

    private OutboundEmail enqueueSimple() {
        return emailQueueService.enqueue(
                EmailMessage.builder()
                        .to(RECIPIENT)
                        .subject("Subject")
                        .htmlBody("<p>Hello</p>")
                        .build());
    }

    private OutboundEmail reload(UUID id) {
        return QuarkusTransaction.requiringNew().call(() -> outboundEmailRepository.findById(id));
    }

    @Test
    void enqueueThenDrain_sendsMailAndMarksSent() {
        OutboundEmail queued = enqueueSimple();
        assertNotNull(queued);
        assertEquals(EmailStatus.PENDING, reload(queued.id).getStatus());

        int sent = emailDispatcher.drainQueue();

        assertEquals(1, sent);
        assertEquals(1, mailbox.getMailsSentTo(RECIPIENT).size());
        OutboundEmail result = reload(queued.id);
        assertEquals(EmailStatus.SENT, result.getStatus());
        assertEquals(1, result.getAttempts());
        assertNotNull(result.getSentAt());
        assertNull(result.getLastError());
    }

    @Test
    void drainTwice_doesNotResendAlreadySentMail() {
        enqueueSimple();

        emailDispatcher.drainQueue();
        int secondRun = emailDispatcher.drainQueue();

        assertEquals(0, secondRun);
        assertEquals(1, mailbox.getMailsSentTo(RECIPIENT).size());
    }

    @Test
    void enqueue_withoutRecipients_isDropped() {
        OutboundEmail queued =
                emailQueueService.enqueue(
                        EmailMessage.builder()
                                .subject("No recipients")
                                .htmlBody("<p>x</p>")
                                .build());

        assertNull(queued);
        long count = QuarkusTransaction.requiringNew().call(() -> outboundEmailRepository.count());
        assertEquals(0, count);
    }

    @Test
    void repeatedFailures_areRetriedThenDeadLettered() {
        UUID id = enqueueSimple().id;
        RuntimeException error = new RuntimeException("SMTP unavailable");

        // First failure: still PENDING and scheduled for a retry.
        emailDispatcher.markFailure(id, error);
        OutboundEmail afterFirst = reload(id);
        assertEquals(EmailStatus.PENDING, afterFirst.getStatus());
        assertEquals(1, afterFirst.getAttempts());
        assertNotNull(afterFirst.getLastError());

        // Exhaust the remaining attempts.
        for (int attempt = 1; attempt < maxAttempts; attempt++) {
            emailDispatcher.markFailure(id, error);
        }

        OutboundEmail dead = reload(id);
        assertEquals(EmailStatus.FAILED, dead.getStatus());
        assertEquals(maxAttempts, dead.getAttempts());
        assertNotNull(dead.getLastError());
    }

    @Test
    void staleSendingMessage_isRequeuedAndSent() {
        // Simulate a message left in SENDING by a crashed dispatcher long ago.
        UUID id =
                QuarkusTransaction.requiringNew()
                        .call(
                                () -> {
                                    Instant old = Instant.now().minusSeconds(3600);
                                    OutboundEmail email = new OutboundEmail();
                                    email.setTo(List.of(RECIPIENT));
                                    email.setSubject("Stuck");
                                    email.setHtmlBody("<p>Stuck</p>");
                                    email.setStatus(EmailStatus.SENDING);
                                    email.setMaxAttempts(maxAttempts);
                                    email.setNextAttemptAt(old);
                                    email.setCreatedAt(old);
                                    email.setUpdatedAt(old);
                                    outboundEmailRepository.persist(email);
                                    return email.id;
                                });

        int sent = emailDispatcher.drainQueue();

        assertEquals(1, sent);
        assertEquals(EmailStatus.SENT, reload(id).getStatus());
        assertEquals(1, mailbox.getMailsSentTo(RECIPIENT).size());
    }

    @Test
    void drain_withNothingDue_doesNothing() {
        assertEquals(0, emailDispatcher.drainQueue());
        assertEquals(
                0L, QuarkusTransaction.requiringNew().call(() -> outboundEmailRepository.count()));
    }
}
