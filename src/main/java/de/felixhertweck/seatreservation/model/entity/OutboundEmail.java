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
package de.felixhertweck.seatreservation.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * A single email queued in the transactional outbox.
 *
 * <p>The message is fully rendered (recipients, subject, HTML body and any attachments) at enqueue
 * time and persisted in the same transaction as the business change that triggered it. A background
 * dispatcher later claims {@link EmailStatus#PENDING} rows whose {@link #nextAttemptAt} is due,
 * hands them to the mail server and records the outcome, retrying with back-off until the message
 * is {@link EmailStatus#SENT} or the attempts are exhausted ({@link EmailStatus#FAILED}).
 */
@Entity
@Table(
        name = "outbound_emails",
        indexes = {
            @Index(name = "idx_outbound_email_status", columnList = "status"),
            @Index(name = "idx_outbound_email_next_attempt", columnList = "next_attempt_at")
        })
public class OutboundEmail extends PanacheEntity {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "outbound_email_recipients",
            joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "address", nullable = false)
    private List<String> to = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "outbound_email_cc", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "address", nullable = false)
    private List<String> cc = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "outbound_email_bcc", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "address", nullable = false)
    private List<String> bcc = new ArrayList<>();

    @Column(nullable = false, length = 1024)
    private String subject;

    @Lob
    @Column(name = "html_body", nullable = false)
    private String htmlBody;

    @OneToMany(
            mappedBy = "email",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<OutboundEmailAttachment> attachments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_error", length = 2048)
    private String lastError;

    /** Constructor for JPA. */
    public OutboundEmail() {}

    /**
     * Registers an attachment and wires up the bidirectional relationship.
     *
     * @param attachment the attachment to add
     */
    public void addAttachment(OutboundEmailAttachment attachment) {
        attachment.setEmail(this);
        this.attachments.add(attachment);
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to != null ? to : new ArrayList<>();
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc != null ? cc : new ArrayList<>();
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc != null ? bcc : new ArrayList<>();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public List<OutboundEmailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<OutboundEmailAttachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
