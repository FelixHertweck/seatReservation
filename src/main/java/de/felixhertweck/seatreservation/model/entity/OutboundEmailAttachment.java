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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A binary attachment belonging to an {@link OutboundEmail}. The rendered bytes (for example a
 * seat-map PNG or a QR code) are stored alongside the message so the dispatcher can send the mail
 * without re-rendering anything.
 *
 * <p>If {@link #contentId} is set the attachment is embedded inline (referenced from the HTML body
 * via {@code cid:<contentId>}); otherwise it is added as a regular file attachment.
 */
@Entity
@Table(name = "outbound_email_attachments")
public class OutboundEmailAttachment extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private OutboundEmail email;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** Content-ID for inline attachments; {@code null} for regular file attachments. */
    @Column(name = "content_id")
    private String contentId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", nullable = false)
    private byte[] data;

    /** Constructor for JPA. */
    public OutboundEmailAttachment() {}

    public OutboundEmailAttachment(
            String fileName, String contentType, String contentId, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.contentId = contentId;
        this.data = data;
    }

    public boolean isInline() {
        return contentId != null && !contentId.isBlank();
    }

    public OutboundEmail getEmail() {
        return email;
    }

    public void setEmail(OutboundEmail email) {
        this.email = email;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
