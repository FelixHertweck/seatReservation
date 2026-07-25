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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EmailAttachmentTest {

    @Test
    void testConstructorAndGetters() {
        byte[] data = new byte[] {1, 2, 3};
        EmailAttachment attachment = new EmailAttachment("test.png", "image/png", "cid123", data);

        assertEquals("test.png", attachment.fileName());
        assertEquals("image/png", attachment.contentType());
        assertEquals("cid123", attachment.contentId());
        assertEquals(data, attachment.data());
    }

    @Test
    void testInlineFactory() {
        byte[] data = new byte[] {4, 5, 6};
        EmailAttachment attachment =
                EmailAttachment.inline("logo.png", "image/png", "logo-cid", data);

        assertEquals("logo.png", attachment.fileName());
        assertEquals("image/png", attachment.contentType());
        assertEquals("logo-cid", attachment.contentId());
        assertEquals(data, attachment.data());
    }

    @Test
    void testFileFactory() {
        byte[] data = new byte[] {7, 8, 9};
        EmailAttachment attachment = EmailAttachment.file("report.pdf", "application/pdf", data);

        assertEquals("report.pdf", attachment.fileName());
        assertEquals("application/pdf", attachment.contentType());
        assertNull(attachment.contentId());
        assertEquals(data, attachment.data());
    }

    @Test
    void testEqualsAndHashCode() {
        byte[] data1 = new byte[] {1, 2, 3};
        byte[] data2 = new byte[] {1, 2, 3};
        byte[] data3 = new byte[] {1, 2, 4};

        EmailAttachment a1 = new EmailAttachment("a.txt", "text/plain", "cid1", data1);
        EmailAttachment a2 = new EmailAttachment("a.txt", "text/plain", "cid1", data2);
        EmailAttachment a3 = new EmailAttachment("b.txt", "text/plain", "cid1", data1);
        EmailAttachment a4 = new EmailAttachment("a.txt", "text/html", "cid1", data1);
        EmailAttachment a5 = new EmailAttachment("a.txt", "text/plain", "cid2", data1);
        EmailAttachment a6 = new EmailAttachment("a.txt", "text/plain", "cid1", data3);
        EmailAttachment a7 = new EmailAttachment("a.txt", "text/plain", null, data1);
        EmailAttachment a8 = new EmailAttachment("a.txt", "text/plain", null, data2);

        // Self equality
        assertEquals(a1, a1);

        // Null and type mismatch
        assertNotEquals(null, a1);
        assertNotEquals("string", a1);

        // Identical contents
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        // Differences in fileName
        assertNotEquals(a1, a3);
        assertNotEquals(a1.hashCode(), a3.hashCode());

        // Differences in contentType
        assertNotEquals(a1, a4);
        assertNotEquals(a1.hashCode(), a4.hashCode());

        // Differences in contentId
        assertNotEquals(a1, a5);
        assertNotEquals(a1.hashCode(), a5.hashCode());

        // Differences in data
        assertNotEquals(a1, a6);
        assertNotEquals(a1.hashCode(), a6.hashCode());

        // Null content ID handling
        assertEquals(a7, a8);
        assertEquals(a7.hashCode(), a8.hashCode());
        assertNotEquals(a1, a7);
    }

    @Test
    void testToString() {
        byte[] data = new byte[] {1, 2, 3};
        EmailAttachment attachment = new EmailAttachment("test.png", "image/png", "cid123", data);
        String expected =
                "EmailAttachment[fileName=test.png, contentType=image/png, contentId=cid123, data=3"
                        + " bytes]";
        assertEquals(expected, attachment.toString());

        EmailAttachment nullDataAttachment =
                new EmailAttachment("test.png", "image/png", "cid123", null);
        String expectedNullData =
                "EmailAttachment[fileName=test.png, contentType=image/png, contentId=cid123,"
                        + " data=null]";
        assertEquals(expectedNullData, nullDataAttachment.toString());
    }
}
