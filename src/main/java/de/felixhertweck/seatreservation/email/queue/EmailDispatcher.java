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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailStatus;
import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import de.felixhertweck.seatreservation.model.entity.OutboundEmailAttachment;
import de.felixhertweck.seatreservation.model.repository.OutboundEmailRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Background worker that drains the {@link OutboundEmail} outbox.
 *
 * <p>On every tick it claims a batch of due messages, hands each one to the (blocking) mail server
 * and records the outcome. Failed sends are retried with exponential back-off until they succeed or
 * the configured attempt limit is reached, at which point the message becomes a {@link
 * EmailStatus#FAILED} dead letter.
 *
 * <p>SMTP I/O happens <em>outside</em> any database transaction: claiming, loading and status
 * updates each run in their own short transaction so a slow mail server never holds a database
 * connection open. {@link ConcurrentExecution#SKIP} prevents overlapping drains within a single
 * instance, and the claim step itself ({@link OutboundEmailRepository#claimDue}, an atomic {@code
 * UPDATE ... FOR UPDATE SKIP LOCKED}) prevents double delivery even when multiple clustered
 * instances drain concurrently.
 */
@ApplicationScoped
public class EmailDispatcher {

    private static final Logger LOG = Logger.getLogger(EmailDispatcher.class);

    @Inject Mailer mailer;

    @Inject OutboundEmailRepository outboundEmailRepository;

    @Inject MeterRegistry meterRegistry;

    @Inject EmailDispatcher self;

    @ConfigProperty(name = "email.queue.batch-size", defaultValue = "20")
    int batchSize;

    @ConfigProperty(name = "email.queue.retry-backoff-seconds", defaultValue = "60")
    long retryBackoffSeconds;

    @ConfigProperty(name = "email.queue.max-backoff-seconds", defaultValue = "3600")
    long maxBackoffSeconds;

    @ConfigProperty(name = "email.queue.sending-timeout-seconds", defaultValue = "300")
    long sendingTimeoutSeconds;

    @ConfigProperty(name = "email.queue.immediate-trigger", defaultValue = "true")
    boolean immediateTriggerEnabled;

    /** Guards against piling up concurrent drain loops when many mails are enqueued at once. */
    private final AtomicBoolean draining = new AtomicBoolean(false);

    /**
     * Set by every {@link #triggerDrain} call and cleared at the start of each loop pass; lets a
     * trigger that arrives while the loop is winding down ask for one more pass instead of being
     * silently dropped (see {@link #drainLoop}).
     */
    private final AtomicBoolean pendingRedrain = new AtomicBoolean(false);

    /**
     * Fallback poll of the outbox. In the common case {@link #onEmailEnqueued} already triggers a
     * drain right after a message is committed, so this mainly picks up retries (whose {@code
     * nextAttemptAt} lies in the future) and messages left behind by a crashed instance.
     */
    @Scheduled(
            every = "${email.queue.poll-interval:30s}",
            concurrentExecution = ConcurrentExecution.SKIP)
    void scheduledDrain() {
        drainQueue();
    }

    /**
     * Triggers an immediate drain once a message has been committed to the outbox, instead of
     * waiting for the next {@link #scheduledDrain} tick. Runs as an {@code AFTER_SUCCESS}
     * transactional observer so it never fires for a message whose enqueuing transaction rolled
     * back.
     *
     * @param event the enqueue notification (payload unused; only its arrival matters)
     */
    void onEmailEnqueued(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) EmailEnqueuedEvent event) {
        if (immediateTriggerEnabled) {
            triggerDrain();
        }
    }

    /**
     * Runs {@link #drainQueue} on a virtual thread until the outbox reports nothing left to send,
     * coalescing concurrent triggers (e.g. from a bulk send enqueuing many messages at once) into a
     * single loop so each one doesn't open its own database connection.
     */
    private void triggerDrain() {
        pendingRedrain.set(true);
        if (draining.compareAndSet(false, true)) {
            Thread.ofVirtual().name("email-dispatch-trigger").start(this::drainLoop);
        }
    }

    /**
     * Drains the queue until empty, then releases {@link #draining} and checks {@link
     * #pendingRedrain} once more before actually exiting.
     *
     * <p>Without that recheck, a trigger landing between this loop's last (empty) {@link
     * #drainQueue} call and the release of {@link #draining} would see draining still in progress,
     * assume this loop will pick up its message, and return without starting a new one -- silently
     * falling back to the next {@link #scheduledDrain} tick instead of dispatching immediately.
     */
    private void drainLoop() {
        try {
            do {
                pendingRedrain.set(false);
                int sent;
                do {
                    sent = drainQueue();
                } while (sent > 0);
                draining.set(false);
            } while (pendingRedrain.get() && draining.compareAndSet(false, true));
        } catch (RuntimeException e) {
            LOG.error("Immediate email dispatch loop failed", e);
            draining.set(false);
        }
    }

    /**
     * Sends all messages that are currently due. Exposed (package-private) so tests and operational
     * tooling can drain the queue deterministically without waiting for the scheduler.
     *
     * @return the number of messages that were sent successfully
     */
    public int drainQueue() {
        List<UUID> claimed = self.claimDueIds(batchSize);
        if (claimed.isEmpty()) {
            return 0;
        }
        LOG.debugf("Dispatching %d queued email(s)", claimed.size());

        int sent = 0;
        for (UUID id : claimed) {
            if (dispatchOne(id)) {
                sent++;
            }
        }
        LOG.debugf("Email dispatch cycle finished: %d/%d sent", sent, claimed.size());
        return sent;
    }

    /**
     * Atomically claims up to {@code limit} due messages by flipping them to {@link
     * EmailStatus#SENDING}, so a concurrent or overlapping drain (including one running in another
     * clustered instance) cannot pick them up again.
     *
     * @param limit the maximum number of messages to claim
     * @return the ids of the claimed messages
     */
    @Transactional
    public List<UUID> claimDueIds(int limit) {
        Instant now = Instant.now();
        // Recover messages left in SENDING by a previous crashed/aborted drain.
        long requeued =
                outboundEmailRepository.requeueStaleSending(
                        now.minusSeconds(sendingTimeoutSeconds));
        if (requeued > 0) {
            LOG.warnf("Requeued %d stale SENDING email(s) for retry", requeued);
        }

        return outboundEmailRepository.claimDue(now, limit);
    }

    /**
     * Sends a single claimed message and records the result.
     *
     * @param id the outbox id
     * @return {@code true} if the message was sent successfully
     */
    private boolean dispatchOne(UUID id) {
        Mail mail = self.buildMail(id);
        if (mail == null) {
            // Message vanished between claim and load; nothing to do.
            return false;
        }
        try {
            mailer.send(mail);
        } catch (RuntimeException e) {
            self.markFailure(id, e);
            return false;
        }
        self.markSent(id);
        return true;
    }

    /**
     * Loads a claimed message and converts it into a mailer {@link Mail}. Runs in a transaction so
     * lazy attachment data can be read; the returned {@link Mail} holds its own copies and is safe
     * to use after the transaction closes.
     *
     * @param id the outbox id
     * @return the ready-to-send mail, or {@code null} if the message no longer exists
     */
    @Transactional
    public Mail buildMail(UUID id) {
        OutboundEmail email = outboundEmailRepository.findById(id);
        if (email == null) {
            LOG.warnf("Queued email id=%s disappeared before sending", id);
            return null;
        }

        Mail mail = new Mail();
        mail.setSubject(email.getSubject());
        mail.setHtml(email.getHtmlBody());
        email.getTo().forEach(mail::addTo);
        email.getCc().forEach(mail::addCc);
        email.getBcc().forEach(mail::addBcc);

        for (OutboundEmailAttachment attachment : email.getAttachments()) {
            if (attachment.isInline()) {
                mail.addInlineAttachment(
                        attachment.getFileName(),
                        attachment.getData(),
                        attachment.getContentType(),
                        attachment.getContentId());
            } else {
                mail.addAttachment(
                        attachment.getFileName(),
                        attachment.getData(),
                        attachment.getContentType());
            }
        }
        return mail;
    }

    /**
     * Marks a message as successfully delivered.
     *
     * @param id the outbox id
     */
    @Transactional
    public void markSent(UUID id) {
        OutboundEmail email = outboundEmailRepository.findById(id);
        if (email == null) {
            return;
        }
        Instant now = Instant.now();
        email.setStatus(EmailStatus.SENT);
        email.setAttempts(email.getAttempts() + 1);
        email.setSentAt(now);
        email.setUpdatedAt(now);
        email.setLastError(null);
        LOG.infof(
                "Email id=%s sent successfully to %s (attempt %d)",
                id, email.getTo(), email.getAttempts());
    }

    /**
     * Records a failed send attempt and either schedules a retry with exponential back-off or, once
     * the attempt limit is reached, moves the message to the {@link EmailStatus#FAILED} dead-letter
     * state.
     *
     * @param id the outbox id
     * @param error the failure that occurred
     */
    @Transactional
    public void markFailure(UUID id, Exception error) {
        OutboundEmail email = outboundEmailRepository.findById(id);
        if (email == null) {
            return;
        }
        Instant now = Instant.now();
        int attempts = email.getAttempts() + 1;
        email.setAttempts(attempts);
        email.setUpdatedAt(now);
        email.setLastError(truncate(error.toString()));

        if (attempts >= email.getMaxAttempts()) {
            email.setStatus(EmailStatus.FAILED);
            if (meterRegistry != null) {
                meterRegistry.counter("email.outbox.failed").increment();
            }
            LOG.errorf(
                    error,
                    "Email id=%s permanently failed after %d attempt(s); moving to dead letter",
                    id,
                    attempts);
        } else {
            email.setStatus(EmailStatus.PENDING);
            email.setNextAttemptAt(now.plusSeconds(backoffSeconds(attempts)));
            LOG.warnf(
                    error,
                    "Email id=%s failed (attempt %d/%d); retrying at %s",
                    id,
                    attempts,
                    email.getMaxAttempts(),
                    email.getNextAttemptAt());
        }
    }

    /**
     * Computes the exponential back-off delay for the given attempt number, capped at the
     * configured maximum.
     *
     * @param attempts the number of attempts made so far
     * @return the delay in seconds before the next attempt
     */
    private long backoffSeconds(int attempts) {
        // attempts >= 1: 1x, 2x, 4x, 8x ... of the base back-off, capped.
        long factor = 1L << Math.min(attempts - 1, 16);
        long delay = retryBackoffSeconds * factor;
        return Math.min(delay, maxBackoffSeconds);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2048 ? value : value.substring(0, 2048);
    }
}
