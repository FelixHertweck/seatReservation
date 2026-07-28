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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import de.felixhertweck.seatreservation.email.queue.EmailMessage;
import de.felixhertweck.seatreservation.email.queue.EmailQueueService;
import de.felixhertweck.seatreservation.model.entity.EmailStatus;
import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import de.felixhertweck.seatreservation.model.repository.OutboundEmailRepository;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code email.queue.immediate-trigger} path end-to-end: unlike {@link
 * EmailQueueDispatcherTest} (which disables it for deterministic manual {@code drainQueue()}
 * calls), this test re-enables it and asserts a queued mail gets sent on its own. The profile keeps
 * the inherited {@code poll-interval: 24h} from {@code application-test.yaml}, so a SENT status can
 * only come from the immediate trigger, not the scheduled fallback.
 */
@QuarkusTest
@TestProfile(EmailQueueImmediateDispatchTest.ImmediateTriggerProfile.class)
class EmailQueueImmediateDispatchTest {

    private static final String RECIPIENT = "recipient@example.com";

    @Inject EmailQueueService emailQueueService;

    @Inject OutboundEmailRepository outboundEmailRepository;

    @Inject MockMailbox mailbox;

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

    private OutboundEmail reload(UUID id) {
        return QuarkusTransaction.requiringNew().call(() -> outboundEmailRepository.findById(id));
    }

    private OutboundEmail awaitStatus(UUID id, EmailStatus expected, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        OutboundEmail email;
        do {
            email = reload(id);
            if (email.getStatus() == expected) {
                return email;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for status " + expected, e);
            }
        } while (Instant.now().isBefore(deadline));
        fail(
                "Timed out waiting for status "
                        + expected
                        + ", last seen status was "
                        + email.getStatus());
        throw new AssertionError("unreachable");
    }

    @Test
    void enqueue_withImmediateTriggerEnabled_isSentWithoutManualDrain() {
        OutboundEmail queued =
                emailQueueService.enqueue(
                        EmailMessage.builder()
                                .to(RECIPIENT)
                                .subject("Subject")
                                .htmlBody("<p>Hello</p>")
                                .build());
        assertNotNull(queued);

        OutboundEmail result = awaitStatus(queued.id, EmailStatus.SENT, Duration.ofSeconds(5));

        assertEquals(1, mailbox.getMailsSentTo(RECIPIENT).size());
        assertNotNull(result.getSentAt());
    }

    public static class ImmediateTriggerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("email.queue.immediate-trigger", "true");
        }
    }
}
