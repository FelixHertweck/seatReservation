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
package de.felixhertweck.seatreservation.sanitization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HtmlSanitizerUtilsTest {

    @Test
    void sanitize_ReturnsNullWhenInputIsNull() {
        String result = HtmlSanitizerUtils.sanitize(null);
        assertNull(result, "Sanitize should return null when input is null");
    }

    @Test
    void sanitize_ReturnsEmptyStringWhenInputIsEmpty() {
        String result = HtmlSanitizerUtils.sanitize("");
        assertEquals("", result, "Sanitize should return empty string when input is empty");
    }

    @Test
    void sanitize_RemovesScriptTags() {
        String unsafeHtml = "<script>alert('XSS')</script>Hello";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<script>"), "Sanitized result should not contain script tags");
        assertFalse(result.contains("alert"), "Sanitized result should not contain script content");
    }

    @Test
    void sanitize_RemovesOnEventHandlers() {
        String unsafeHtml = "<div onclick='alert(1)'>Click me</div>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(
                result.contains("onclick"), "Sanitized result should not contain onclick handler");
        assertFalse(result.contains("alert"), "Sanitized result should not contain alert content");
    }

    @Test
    void sanitize_RemovesIframeTags() {
        String unsafeHtml = "<iframe src='http://evil.com'></iframe>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<iframe"), "Sanitized result should not contain iframe tags");
        assertFalse(result.contains("evil.com"), "Sanitized result should not contain iframe src");
    }

    @Test
    void sanitize_RemovesAnchorTagsButAllowsMailtoProtocol() {
        // The policy allows mailto protocol, but it doesn't allow any HTML tags
        // so the anchor tag is removed but the text content is preserved
        String safeHtml = "<a href='mailto:test@example.com'>Email</a>";
        String result = HtmlSanitizerUtils.sanitize(safeHtml);
        assertEquals("Email", result, "Should preserve text content but remove tags");
    }

    @Test
    void sanitize_RemovesHttpLinks() {
        String unsafeHtml = "<a href='http://example.com'>Link</a>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("href"), "Sanitized result should remove http links");
    }

    @Test
    void sanitize_RemovesHttpsLinks() {
        String unsafeHtml = "<a href='https://example.com'>Link</a>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("href"), "Sanitized result should remove https links");
    }

    @Test
    void sanitize_PreservesPlainText() {
        String plainText = "This is plain text without any HTML";
        String result = HtmlSanitizerUtils.sanitize(plainText);
        assertEquals(plainText, result, "Plain text should be preserved as is");
    }

    @Test
    void sanitize_HandlesMultipleXssAttempts() {
        String unsafeHtml =
                "<script>alert('XSS')</script><img src=x onerror='alert(1)'><iframe"
                        + " src='javascript:alert(2)'></iframe>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<script>"), "Should remove script tags");
        assertFalse(result.contains("<img"), "Should remove img tags with event handlers");
        assertFalse(result.contains("<iframe"), "Should remove iframe tags");
        assertFalse(result.contains("alert"), "Should remove all alert calls");
    }

    @Test
    void sanitize_RemovesStyleTags() {
        String unsafeHtml = "<style>body { background: red; }</style>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<style>"), "Sanitized result should not contain style tags");
    }

    @Test
    void sanitize_RemovesEmbedTags() {
        String unsafeHtml = "<embed src='malicious.swf'>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<embed"), "Sanitized result should not contain embed tags");
    }

    @Test
    void sanitize_RemovesObjectTags() {
        String unsafeHtml = "<object data='malicious.swf'></object>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<object"), "Sanitized result should not contain object tags");
    }

    @Test
    void sanitize_HandlesNestedTags() {
        String unsafeHtml = "<div><script>alert('XSS')</script><p>Text</p></div>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("<script>"), "Should remove nested script tags");
        assertFalse(result.contains("alert"), "Should remove script content");
    }

    @Test
    void sanitize_HandlesMalformedHtml() {
        String malformedHtml = "<script>alert('XSS')<p>Unclosed";
        String result = HtmlSanitizerUtils.sanitize(malformedHtml);
        assertFalse(result.contains("<script>"), "Should handle malformed HTML");
        assertNotNull(result, "Should return a non-null result for malformed HTML");
    }

    @Test
    void sanitize_RemovesJavascriptProtocol() {
        String unsafeHtml = "<a href='javascript:alert(1)'>Click</a>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("javascript:"), "Should remove javascript protocol");
        assertFalse(result.contains("alert"), "Should remove javascript content");
    }

    @Test
    void sanitize_RemovesDataProtocol() {
        String unsafeHtml = "<a href='data:text/html,<script>alert(1)</script>'>Click</a>";
        String result = HtmlSanitizerUtils.sanitize(unsafeHtml);
        assertFalse(result.contains("data:"), "Should remove data protocol");
    }
}
