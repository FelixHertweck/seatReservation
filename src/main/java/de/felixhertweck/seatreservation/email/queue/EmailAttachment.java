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

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable description of a binary attachment to be queued with an {@link EmailMessage}.
 *
 * <p>When {@link #contentId()} is non-null the attachment is embedded inline and can be referenced
 * from the HTML body via {@code cid:<contentId>}; otherwise it is a regular file attachment.
 *
 * @param fileName the file name shown to the recipient
 * @param contentType the MIME type (e.g. {@code image/png})
 * @param contentId the inline content-id, or {@code null} for a regular attachment
 * @param data the raw attachment bytes
 */
public record EmailAttachment(String fileName, String contentType, String contentId, byte[] data) {

    /**
     * Overridden because the record-generated {@code equals} compares {@link #data} by array
     * reference (via {@code Objects.equals}), not by content.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o
                instanceof
                EmailAttachment(
                        String oFileName,
                        String oContentType,
                        String oContentId,
                        byte[] oData))) {
            return false;
        }
        return Objects.equals(fileName, oFileName)
                && Objects.equals(contentType, oContentType)
                && Objects.equals(contentId, oContentId)
                && Arrays.equals(data, oData);
    }

    /** Overridden to stay consistent with the content-based {@link #equals(Object)}. */
    @Override
    public int hashCode() {
        int result = Objects.hash(fileName, contentType, contentId);
        return 31 * result + Arrays.hashCode(data);
    }

    /** Overridden so {@link #data} is rendered as its length rather than a raw array reference. */
    @Override
    public String toString() {
        return "EmailAttachment[fileName="
                + fileName
                + ", contentType="
                + contentType
                + ", contentId="
                + contentId
                + ", data="
                + (data != null ? data.length + " bytes" : "null")
                + "]";
    }

    /**
     * Creates an inline attachment referenced from the HTML body via {@code cid:<contentId>}.
     *
     * @param fileName the file name
     * @param contentType the MIME type
     * @param contentId the content-id referenced in the HTML
     * @param data the raw bytes
     * @return the inline attachment
     */
    public static EmailAttachment inline(
            String fileName, String contentType, String contentId, byte[] data) {
        return new EmailAttachment(fileName, contentType, contentId, data);
    }

    /**
     * Creates a regular (non-inline) file attachment.
     *
     * @param fileName the file name
     * @param contentType the MIME type
     * @param data the raw bytes
     * @return the file attachment
     */
    public static EmailAttachment file(String fileName, String contentType, byte[] data) {
        return new EmailAttachment(fileName, contentType, null, data);
    }
}
